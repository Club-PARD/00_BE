package com.youngyoung.server.mora.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youngyoung.server.mora.client.OpenAssemblyClient;
import com.youngyoung.server.mora.dto.AiResponseDto;
import com.youngyoung.server.mora.dto.OpenApiDto;
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
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Optional;
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
    private final OpenAssemblyClient openAssemblyClient;
    private final ObjectMapper objectMapper;

    @Value("${open-api.assembly.key}")
    private String assemblyApiKey;

    @Transactional
    public void runBatch() {
        log.info("========== [Mora Batch] 작업 시작 ==========");
        updatePetitionStatusAndResult(); // Phase 0
        runCrawlingBatch();              // Phase 1~4
        log.info("========== [Mora Batch] 작업 종료 ==========");
    }

    // [Phase 0] 기존 데이터 업데이트
    private void updatePetitionStatusAndResult() {
        log.info(">> [Phase 0] 기존 청원 상태 및 결과 업데이트 시작");

        // 1. 기간 만료 처리 (0 -> 1)
        List<Petition> expiredPetitions = petitionRepo.findAllByStatusAndVoteEndDateBefore(0, LocalDateTime.now());
        for (Petition p : expiredPetitions) {
            p.updateStatus(1);
        }
        log.info("기간 만료 청원 {}건 상태 변경 완료 (0->1)", expiredPetitions.size());

        // 2. 계류정보(소관위, 회부일) 업데이트 (Status=1, Department="-")
        List<Petition> pendingPetitions = petitionRepo.findByStatusAndDepartment(1, "-");
        log.info("계류정보 업데이트 대상 {}건 조회됨", pendingPetitions.size());

        for (Petition p : pendingPetitions) {
            try {
                // API 호출 (제목으로 검색)
                String jsonResponse = openAssemblyClient.getPendingPetitions(assemblyApiKey, "json", 1, 20, p.getTitle());
                OpenApiDto dto = objectMapper.readValue(jsonResponse, OpenApiDto.class);

                if (dto.getPendingList() != null && !dto.getPendingList().isEmpty()) {
                    List<OpenApiDto.Row> rows = dto.getPendingList().get(1).getRow();
                    if (rows != null && !rows.isEmpty()) {
                        // 제목이 정확히 일치하는지 확인 (API 검색은 '포함' 검색일 수 있음)
                        // 일치하는게 없으면 첫번째꺼 씀 (대부분 첫번째가 맞음)
                        OpenApiDto.Row targetRow = rows.get(0);

                        String cDate = targetRow.getCommitteeDt();
                        // ★ [핵심 수정] 문자열 "null"이거나 진짜 null이면 건너뜀
                        if (cDate != null && !cDate.isBlank() && !cDate.equals("null")) {
                            LocalDate committeeDate = LocalDate.parse(cDate, DateTimeFormatter.ISO_DATE);
                            p.updateFinalDateAndDept(committeeDate.atStartOfDay(), targetRow.getCurrCommittee());
                            log.info("계류정보 업데이트 성공: [{}] -> 소관위: {}, 회부일: {}", p.getTitle(), targetRow.getCurrCommittee(), committeeDate);
                        } else {
                            log.info("계류정보 없음(날짜미정): [{}]", p.getTitle());
                        }
                    }
                } else {
                    log.info("계류정보 API 결과 없음: [{}]", p.getTitle());
                }
            } catch (Exception e) {
                log.warn("계류현황 API 처리 중 오류 (청원: {}): {}", p.getTitle(), e.getMessage());
            }
        }

        // 3. 처리결과 업데이트 (Result="-", FinalDate < 오늘)
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Petition> processingPetitions = petitionRepo.findByResultAndFinalDateBefore("-", todayStart);
        log.info("처리결과 업데이트 대상 {}건 조회됨", processingPetitions.size());

        for (Petition p : processingPetitions) {
            try {
                String jsonResponse = openAssemblyClient.getProcessedPetitions(
                        assemblyApiKey, "json", 1, 20, "22", null, p.getTitle()
                );

                OpenApiDto dto = objectMapper.readValue(jsonResponse, OpenApiDto.class);
                if (dto.getProcessedList() != null && !dto.getProcessedList().isEmpty()) {
                    List<OpenApiDto.Row> rows = dto.getProcessedList().get(1).getRow();
                    if (rows != null && !rows.isEmpty()) {
                        OpenApiDto.Row row = rows.get(0);

                        String resultCd = row.getProcResultCd();
                        // ★ [핵심 수정] 결과도 "null" 문자열 체크
                        if (resultCd != null && !resultCd.isBlank() && !resultCd.equals("null")) {
                            p.updateResult(resultCd);
                            p.updateStatus(2); // 완료 상태 변경
                            log.info("처리결과 업데이트(완료): [{}] -> 결과: {}, 상태: 2", p.getTitle(), resultCd);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("처리현황 API 처리 중 오류 (청원: {}): {}", p.getTitle(), e.getMessage());
            }
        }
    }

    // [Phase 1~4] 신규 크롤링 (기존 동일)
    private void runCrawlingBatch() {
        LocalDate targetDate = LocalDate.now().minusDays(6);
        log.info(">> [Phase 1] 크롤링 배치 시작. (신규 수집 대상 날짜: {})", targetDate);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--lang=ko_KR");
        options.addArguments("--window-size=1920,1080");

        String userHome = System.getProperty("user.home");
        String downloadPath = java.nio.file.Paths.get(userHome, "Downloads").toString();
        File downloadDir = new File(downloadPath);
        if (!downloadDir.exists()) downloadDir.mkdirs();

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadPath);
        options.setExperimentalOption("prefs", prefs);

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        try {
            driver.get("https://assembly.go.kr/portal/cnts/cntsCont/dataA.do?cntsDivCd=PTT&menuNo=600248");

            File[] oldFiles = downloadDir.listFiles((dir, name) -> name.startsWith("진행중청원 목록"));
            if (oldFiles != null) for (File f : oldFiles) f.delete();

            WebElement downloadBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("prgsPtt_excel-down")));
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", downloadBtn);
            log.info("엑셀 다운로드 버튼 클릭 완료");

            File excelFile = new File(downloadPath, "진행중청원 목록.xlsx");
            waitForFileDownload(excelFile, 30);

            List<ExcelRowDto> allPetitions = parseExcelAll(excelFile);
            List<ExcelRowDto> newPetitions = new ArrayList<>();
            int updateCount = 0;

            for (ExcelRowDto dto : allPetitions) {
                Optional<Petition> exist = petitionRepo.findByTitle(dto.getTitle());
                if (exist.isPresent()) {
                    Petition p = exist.get();
                    if (!p.getAllows().equals(dto.getAllows())) {
                        p.updateAllows(dto.getAllows());
                        updateCount++;
                    }
                } else {
                    if (dto.getVoteStartDate().toLocalDate().isEqual(targetDate)) {
                        newPetitions.add(dto);
                    }
                }
            }
            log.info("기존 청원 {}건 동의자 수 최신화 완료.", updateCount);

            if (newPetitions.isEmpty()) {
                log.info("신규 등록 대상 청원이 없습니다.");
                return;
            }

            log.info("신규 청원 {}건 발견! 상세 수집을 시작합니다.", newPetitions.size());

            Map<String, String> titleUrlMap = new HashMap<>();
            for (int i = 1; i <= 8; i++) {
                js.executeScript("prgsPttList_search(" + i + ");");
                Thread.sleep(2000);
                List<WebElement> elements = driver.findElements(By.cssSelector("#prgsPttList-dataset-data-table a.board_subject100"));
                for (WebElement el : elements) {
                    String url = extractUrlFromOnclick(el.getAttribute("onclick"));
                    if (url != null) titleUrlMap.put(el.getText().trim().replaceAll("\\s+", ""), url);
                }
            }

            for (ExcelRowDto dto : newPetitions) {
                String url = titleUrlMap.get(dto.getTitle().replaceAll("\\s+", ""));
                if (url == null) url = findUrlByPartialMatch(titleUrlMap, dto.getTitle());
                if (url == null) continue;

                driver.get(url);
                Thread.sleep(2000);
                String fullText = extractPetitionContent(driver);
                if (fullText.isBlank()) continue;

                AiResponseDto aiData = gptService.analyzePetition(fullText);

                Petition p = Petition.builder()
                        .title(dto.getTitle())
                        .subTitle(aiData.getSubTitle())
                        .category(dto.getCategory())
                        .voteStartDate(dto.getVoteStartDate())
                        .voteEndDate(dto.getVoteEndDate())
                        .allows(dto.getAllows())
                        .url(url)
                        .petitionNeeds(aiData.getNeeds())
                        .petitionSummary(aiData.getSummary())
                        .positiveEx(String.join(",", aiData.getPositiveTags()))
                        .negativeEx(String.join(",", aiData.getNegativeTags()))
                        .type(1).status(0).result("-").department("-").good(0).bad(0).finalDate(null)
                        .build();

                Petition saved = petitionRepo.save(p);
                log.info("신규 청원 저장 완료 (ID: {}, 소제목: {})", saved.getId(), aiData.getSubTitle());

                if (aiData.getLaws() != null) {
                    for (AiResponseDto.LawDto lawDto : aiData.getLaws()) {
                        Laws law = lawsRepo.findByTitle(lawDto.getName())
                                .orElseGet(() -> lawsRepo.save(Laws.builder().title(lawDto.getName()).summary(lawDto.getContent()).build()));
                        lawsLinkRepo.save(LawsLink.builder().petId(saved.getId()).lawId(law.getId()).build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("크롤링 배치 작업 중 오류 발생", e);
        } finally {
            driver.quit();
        }
    }

    private List<ExcelRowDto> parseExcelAll(File file) {
        List<ExcelRowDto> result = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file); Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    String category = row.getCell(1).getStringCellValue();
                    String title = row.getCell(2).getStringCellValue();
                    String period = row.getCell(3).getStringCellValue();
                    String[] dates = period.split(" ~ ");
                    LocalDate startDate = LocalDate.parse(dates[0].trim(), DateTimeFormatter.ISO_DATE);
                    LocalDate endDate = LocalDate.parse(dates[1].trim(), DateTimeFormatter.ISO_DATE);
                    int allows = 0;
                    if (row.getCell(4).getCellType() == CellType.NUMERIC) allows = (int) row.getCell(4).getNumericCellValue();
                    else allows = Integer.parseInt(row.getCell(4).getStringCellValue().replace(",", ""));
                    result.add(ExcelRowDto.builder().category(category).title(title).voteStartDate(startDate.atStartOfDay()).voteEndDate(endDate.atStartOfDay()).allows(allows).build());
                } catch (Exception e) {}
            }
        } catch (Exception e) { log.error("엑셀 파일 읽기 실패", e); }
        return result;
    }

    private String extractPetitionContent(WebDriver driver) {
        List<String> selectors = List.of(".pet_content", ".pre_view", ".view_txt", ".contents", ".board_view", "div[class*='content']", ".cont");
        for (String selector : selectors) {
            try {
                List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                if (!elements.isEmpty() && !elements.get(0).getText().trim().isEmpty()) return elements.get(0).getText().trim();
            } catch (Exception ignored) {}
        }
        try { return driver.findElement(By.tagName("body")).getText(); } catch (Exception e) { return ""; }
    }

    private void waitForFileDownload(File file, int timeoutSeconds) throws InterruptedException {
        int attempts = 0;
        while (attempts < timeoutSeconds) {
            if (file.exists() && file.length() > 0) return;
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
            if (!extractedId.startsWith("http") && !extractedId.startsWith("/")) return "https://petitions.assembly.go.kr/proceed/onGoingAll/" + extractedId;
            return extractedId;
        }
        return null;
    }

    private String findUrlByPartialMatch(Map<String, String> map, String excelTitle) {
        String normalizedExcelTitle = excelTitle.replaceAll("\\s+", "");
        for (String mapKey : map.keySet()) {
            if (mapKey.contains(normalizedExcelTitle) || normalizedExcelTitle.contains(mapKey)) return map.get(mapKey);
        }
        return null;
    }
}

@Getter
@Builder
class ExcelRowDto {
    private String category;
    private String title;
    private LocalDateTime voteStartDate;
    private LocalDateTime voteEndDate;
    private int allows;
}