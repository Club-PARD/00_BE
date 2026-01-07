package com.youngyoung.server.mora.service;

import com.youngyoung.server.mora.dto.AiResponseDto;
import com.youngyoung.server.mora.entity.Laws;
import com.youngyoung.server.mora.entity.LawsLink;
import com.youngyoung.server.mora.entity.Petition;
import com.youngyoung.server.mora.repo.LawsLinkRepo;
import com.youngyoung.server.mora.repo.LawsRepo;
import com.youngyoung.server.mora.repo.PetitionRepo;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetitionBatchService {

    private final PetitionRepo petitionRepo;
    private final LawsRepo lawsRepo;
    private final LawsLinkRepo lawsLinkRepo;
    private final GptService gptService;

    @Transactional
    public void runBatch() {
        // [설정] 2일 전 데이터부터 수집 (요청 사항 반영)
        LocalDate targetDate = LocalDate.now().minusDays(2);
        log.info("배치 시작 - 오늘: {}, 수집 대상 날짜: {}", LocalDate.now(), targetDate);

        // 1. Selenium 옵션 설정
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--lang=ko_KR");
        options.addArguments("--window-size=1920,1080"); // 화면 크기 확보

        // 다운로드 경로 설정
        String userHome = System.getProperty("user.home");
        String downloadPath = java.nio.file.Paths.get(userHome, "Downloads").toString();

        File downloadDir = new File(downloadPath);
        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        log.info("다운로드 경로: {}", downloadPath);

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15)); // 대기 시간 15초로 넉넉하게

        try {
            // =================================================
            // PHASE 1: 엑셀 다운로드 (데이터 포털)
            // =================================================
            driver.get("https://assembly.go.kr/portal/cnts/cntsCont/dataA.do?cntsDivCd=PTT&menuNo=600248");

            // 기존 파일 청소
            File[] oldFiles = downloadDir.listFiles((dir, name) -> name.startsWith("진행중청원 목록"));
            if (oldFiles != null) {
                for (File f : oldFiles) f.delete();
            }

            // 다운로드 버튼 클릭
            WebElement downloadBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("prgsPtt_excel-down")));
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", downloadBtn);
            log.info("엑셀 다운로드 버튼 클릭 완료");

            // 파일 대기
            File excelFile = new File(downloadPath, "진행중청원 목록.xlsx");
            waitForFileDownload(excelFile, 30);

            // 엑셀 파싱
            List<ExcelRowDto> targetPetitions = parseExcelAndFilter(excelFile, targetDate);

            if (targetPetitions.isEmpty()) {
                log.info("수집 대상 날짜({})에 해당하는 청원이 없습니다.", targetDate);
                return;
            }

            // =================================================
            // PHASE 2: 웹에서 URL 매핑 (1~8페이지 스캔)
            // =================================================
            Map<String, String> titleUrlMap = new HashMap<>();

            for (int i = 1; i <= 8; i++) {
                log.info("{} 페이지 URL 수집 중...", i);

                // JS로 페이지 이동
                js.executeScript("prgsPttList_search(" + i + ");");
                Thread.sleep(2000); // 페이지 로딩 대기

                // 목록 테이블에서 제목과 링크 추출
                List<WebElement> elements = driver.findElements(By.cssSelector("#prgsPttList-dataset-data-table a.board_subject100"));

                for (WebElement el : elements) {
                    String title = el.getText().trim();
                    String onClickValue = el.getAttribute("onclick");
                    String url = extractUrlFromOnclick(onClickValue);

                    if (url != null) {
                        titleUrlMap.put(title.replaceAll("\\s+", ""), url);
                    }
                }
            }

            // =================================================
            // PHASE 3: 상세 페이지 크롤링 & AI 분석
            // =================================================
            for (ExcelRowDto excelData : targetPetitions) {
                // 제목 매칭 (공백 제거 후 비교)
                String lookupTitle = excelData.getTitle().replaceAll("\\s+", "");
                String detailUrl = titleUrlMap.get(lookupTitle);

                // 못 찾으면 부분 일치 시도
                if (detailUrl == null) {
                    detailUrl = findUrlByPartialMatch(titleUrlMap, excelData.getTitle());
                }

                if (detailUrl == null) {
                    log.warn("[SKIP] URL을 찾을 수 없음: {}", excelData.getTitle());
                    continue;
                }

                log.info("상세 페이지 진입: {}", detailUrl);
                driver.get(detailUrl);
                Thread.sleep(2000); // 상세 페이지 로딩 대기

                // ★ [핵심 수정] 본문 찾기 전략 (여러 클래스 시도)
                String fullText = extractPetitionContent(driver);

                if (fullText.isBlank()) {
                    log.error("[FAIL] 본문 추출 실패. URL: {}", detailUrl);
                    continue;
                }

                // GPT 분석 요청
                log.info("GPT 분석 시작...");
                AiResponseDto aiData = gptService.analyzePetition(fullText);

                // DB 저장
                Petition petition = Petition.builder()
                        .title(excelData.getTitle())
                        .category(excelData.getCategory())
                        .voteStartDate(excelData.getVoteStartDate())
                        .voteEndDate(excelData.getVoteEndDate())
                        .allows(excelData.getAllows())
                        .type(1)
                        .status(0)
                        .result("-")
                        .good(0)
                        .bad(0)
                        .petitionNeeds(aiData.getNeeds())
                        .petitionSummary(aiData.getSummary())
                        .positiveEx(String.join(",", aiData.getPositiveTags()))
                        .negativeEx(String.join(",", aiData.getNegativeTags()))
                        .build();

                Petition savedPetition = petitionRepo.save(petition);
                log.info("청원 저장 완료 (ID: {})", savedPetition.getId());

                // 관련 법안 저장
                if (aiData.getLaws() != null) {
                    for (AiResponseDto.LawDto lawDto : aiData.getLaws()) {
                        Laws law = lawsRepo.findByTitle(lawDto.getName())
                                .orElseGet(() -> lawsRepo.save(Laws.builder()
                                        .title(lawDto.getName())
                                        .summary(lawDto.getContent())
                                        .build()));

                        lawsLinkRepo.save(LawsLink.builder()
                                .petId(savedPetition.getId())
                                .lawId(law.getId())
                                .build());
                    }
                }
            }

        } catch (Exception e) {
            log.error("배치 작업 중 치명적 오류 발생", e);
        } finally {
            driver.quit();
        }
    }

    // ★ [핵심 Helper] 본문 추출 로직 (여러 선택자 시도)
    private String extractPetitionContent(WebDriver driver) {
        // 국회 청원 사이트에서 사용될 수 있는 본문 클래스 후보군
        List<String> selectors = List.of(
                ".pet_content",      // 기존 추정
                ".pre_view",         // 많이 쓰이는 미리보기 클래스
                ".view_txt",         // 일반적인 게시판 본문
                ".contents",         // 범용 컨텐츠
                ".board_view",       // 게시판 뷰
                "div[class*='content']", // 'content'가 포함된 div
                ".cont"              // 축약형
        );

        for (String selector : selectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty()) {
                    String text = elements.get(0).getText().trim();
                    if (!text.isEmpty()) {
                        log.info("본문 찾기 성공 (Selector: {})", selector);
                        return text;
                    }
                }
            } catch (Exception ignored) {
                // 실패 시 다음 선택자 시도
            }
        }

        // 정 안되면 body 텍스트라도 긁어오기 (최후의 수단)
        try {
            log.warn("본문 클래스를 찾지 못해 Body 전체 텍스트를 시도합니다.");
            return driver.findElement(By.tagName("body")).getText();
        } catch (Exception e) {
            return "";
        }
    }

    private List<ExcelRowDto> parseExcelAndFilter(File file, LocalDate targetDate) {
        List<ExcelRowDto> result = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // [0]번호, [1]분야, [2]제목, [3]기간, [4]동의자수
                    String period = row.getCell(3).getStringCellValue();
                    String[] dates = period.split(" ~ ");

                    LocalDate startDate = LocalDate.parse(dates[0].trim(), DateTimeFormatter.ISO_DATE);

                    // 시작일이 타겟 날짜와 같은지 확인
                    if (startDate.isEqual(targetDate)) {
                        String category = row.getCell(1).getStringCellValue();
                        String title = row.getCell(2).getStringCellValue();
                        LocalDate endDate = LocalDate.parse(dates[1].trim(), DateTimeFormatter.ISO_DATE);

                        int allows = 0;
                        if (row.getCell(4).getCellType() == CellType.NUMERIC) {
                            allows = (int) row.getCell(4).getNumericCellValue();
                        } else {
                            allows = Integer.parseInt(row.getCell(4).getStringCellValue().replace(",", ""));
                        }

                        result.add(ExcelRowDto.builder()
                                .category(category)
                                .title(title)
                                .voteStartDate(startDate.atStartOfDay())
                                .voteEndDate(endDate.atStartOfDay())
                                .allows(allows)
                                .build());

                        log.info("수집 대상 발견: {}", title);
                    }
                } catch (Exception e) {
                    // 날짜 파싱 등 에러 시 해당 행 스킵
                }
            }
        } catch (Exception e) {
            log.error("엑셀 파일 읽기 실패", e);
        }
        return result;
    }

    private void waitForFileDownload(File file, int timeoutSeconds) throws InterruptedException {
        int attempts = 0;
        while (attempts < timeoutSeconds) {
            if (file.exists() && file.length() > 0) {
                log.info("파일 다운로드 확인: {}", file.getName());
                return;
            }
            Thread.sleep(1000);
            attempts++;
        }
        throw new RuntimeException("파일 다운로드 실패");
    }

    private String extractUrlFromOnclick(String onClickValue) {
        if (onClickValue == null || onClickValue.isEmpty()) return null;

        Pattern pattern = Pattern.compile("'([^']*)'");
        Matcher matcher = pattern.matcher(onClickValue);

        if (matcher.find()) {
            String extractedId = matcher.group(1);
            if (!extractedId.startsWith("http") && !extractedId.startsWith("/")) {
                // 최신 URL 패턴 적용
                return "https://petitions.assembly.go.kr/proceed/onGoingAll/" + extractedId;
            }
            return extractedId;
        }
        return null;
    }

    private String findUrlByPartialMatch(Map<String, String> map, String excelTitle) {
        String normalizedExcelTitle = excelTitle.replaceAll("\\s+", "");
        for (String mapKey : map.keySet()) {
            if (mapKey.contains(normalizedExcelTitle) || normalizedExcelTitle.contains(mapKey)) {
                log.info("유사 매칭 성공 [{} -> {}]", excelTitle, mapKey);
                return map.get(mapKey);
            }
        }
        return null;
    }
}

// DTO
@Getter
@Builder
class ExcelRowDto {
    private String category;
    private String title;
    private LocalDateTime voteStartDate;
    private LocalDateTime voteEndDate;
    private int allows;
}