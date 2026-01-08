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
        updatePetitionStatusAndResult();
        runCrawlingBatch();
        log.info("========== [Mora Batch] 작업 종료 ==========");
    }

    // [Phase 0] 기존 데이터 업데이트 (API + 22/21대 통합 검색)
    private void updatePetitionStatusAndResult() {
        log.info(">> [Phase 0] 기존 청원 상태 및 결과 업데이트 시작");

        // 1. 기간 만료 처리
        List<Petition> expiredPetitions = petitionRepo.findAllByStatusAndVoteEndDateBefore(0, LocalDateTime.now());
        for (Petition p : expiredPetitions) p.updateStatus(1);
        log.info("기간 만료 청원 {}건 상태 변경 완료 (0->1)", expiredPetitions.size());

        // 2. 계류정보 업데이트
        List<Petition> pendingPetitions = petitionRepo.findByStatusAndDepartment(1, "-");
        log.info("업데이트 대상(소관위 미정) {}건 조회됨", pendingPetitions.size());

        for (Petition p : pendingPetitions) {
            String title = p.getTitle();
            OpenApiDto.Row row = searchPetitionByAge(title, false); // 계류현황 검색

            if (row != null) {
                updateCommitteeInfo(p, row, "계류현황");
            } else {
                row = searchPetitionByAge(title, true); // 처리현황 검색
                if (row != null) {
                    updateCommitteeInfo(p, row, "처리현황");
                    String result = row.getProcResultCd();
                    if (isValid(result)) {
                        p.updateResult(result);
                        p.updateStatus(2);
                        log.info(" >> 처리결과 동시 업데이트 완료: {}", result);
                    }
                } else {
                    log.info("API 결과 없음 (계류/처리 모두 부재): [{}]", title);
                }
            }
        }

        // 3. 처리결과 업데이트
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        List<Petition> processingPetitions = petitionRepo.findByResultAndFinalDateBefore("-", todayStart);

        for (Petition p : processingPetitions) {
            OpenApiDto.Row row = searchPetitionByAge(p.getTitle(), true);
            if (row != null && isValid(row.getProcResultCd())) {
                p.updateResult(row.getProcResultCd());
                p.updateStatus(2);
                log.info("처리결과 추가 업데이트: [{}] -> {}", p.getTitle(), row.getProcResultCd());
            }
        }
    }

    // [Phase 1~4] 신규 크롤링
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
            // 1. 엑셀 다운로드
            driver.get("https://assembly.go.kr/portal/cnts/cntsCont/dataA.do?cntsDivCd=PTT&menuNo=600248");

            File[] oldFiles = downloadDir.listFiles((dir, name) -> name.startsWith("진행중청원 목록"));
            if (oldFiles != null) for (File f : oldFiles) f.delete();

            WebElement downloadBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("prgsPtt_excel-down")));
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].click();", downloadBtn);

            File excelFile = new File(downloadPath, "진행중청원 목록.xlsx");
            waitForFileDownload(excelFile, 30);

            // 2. 엑셀 파싱
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

            // 3. ★ [복구됨] URL 매핑 (1~8페이지 스캔) - 이게 없어서 에러가 났었습니다!
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

            // 4. 상세 크롤링 & 저장
            for (ExcelRowDto dto : newPetitions) {
                String url = titleUrlMap.get(dto.getTitle().replaceAll("\\s+", ""));
                if (url == null) url = findUrlByPartialMatch(titleUrlMap, dto.getTitle());
                if (url == null) continue;

                driver.get(url);
                Thread.sleep(2000);
                String fullText = extractPetitionContent(driver);
                if (fullText.isBlank()) continue;

                // GPT 분석 (안전 처리)
                AiResponseDto aiData;
                try {
                    aiData = gptService.analyzePetition(fullText);
                } catch (Exception e) {
                    log.error("GPT 분석 실패 (청원: {}). 기본값으로 저장", dto.getTitle());
                    aiData = AiResponseDto.builder()
                            .subTitle(dto.getTitle())
                            .needs("AI 분석에 실패하여 원문을 확인해주세요.")
                            .summary("AI 분석 실패")
                            .positiveTags(List.of("분석 실패/잠시 후 다시 시도해주세요"))
                            .negativeTags(List.of("분석 실패/잠시 후 다시 시도해주세요"))
                            .laws(null)
                            .build();
                }

                List<String> posTags = aiData.getPositiveTags() != null ? aiData.getPositiveTags() : new ArrayList<>();
                List<String> negTags = aiData.getNegativeTags() != null ? aiData.getNegativeTags() : new ArrayList<>();

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
                        .positiveEx(String.join(",", posTags))
                        .negativeEx(String.join(",", negTags))
                        .type(1).status(0).result("-").department("-").good(0).bad(0).finalDate(null)
                        .build();

                Petition saved = petitionRepo.save(p);
                log.info("신규 청원 저장 완료 (ID: {}, 소제목: {})", saved.getId(), aiData.getSubTitle());

                if (aiData.getLaws() != null) {
                    for (AiResponseDto.LawDto lawDto : aiData.getLaws()) {
                        Laws law = lawsRepo.findByTitle(lawDto.getName())
                                .orElseGet(() -> lawsRepo.save(Laws.builder()
                                        .title(lawDto.getName()).summary(lawDto.getContent()).build()));
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

    // --- Helpers ---

    private OpenApiDto.Row searchPetitionByAge(String title, boolean isProcessed) {
        OpenApiDto.Row row = searchApi(title, "22", isProcessed);
        return (row != null) ? row : searchApi(title, "21", isProcessed);
    }

    private OpenApiDto.Row searchApi(String dbTitle, String age, boolean isProcessed) {
        try {
            String jsonResponse = isProcessed
                    ? openAssemblyClient.getProcessedPetitions(assemblyApiKey, "json", 1, 100, age, null, null)
                    : openAssemblyClient.getPendingPetitions(assemblyApiKey, "json", 1, 100, age, null);

            OpenApiDto dto = objectMapper.readValue(jsonResponse, OpenApiDto.class);
            List<OpenApiDto.Row> rows = isProcessed
                    ? (dto.getProcessedList() != null && !dto.getProcessedList().isEmpty() ? dto.getProcessedList().get(1).getRow() : null)
                    : (dto.getPendingList() != null && !dto.getPendingList().isEmpty() ? dto.getPendingList().get(1).getRow() : null);

            if (rows != null) {
                String normDbTitle = dbTitle.replaceAll("\\s+", "");
                for (OpenApiDto.Row r : rows) {
                    String normApiTitle = r.getBillName().replaceAll("\\s+", "");
                    if (normDbTitle.contains(normApiTitle) || normApiTitle.contains(normDbTitle)) {
                        return r;
                    }
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private void updateCommitteeInfo(Petition p, OpenApiDto.Row row, String source) {
        String cDate = row.getCommitteeDt();
        if (isValid(cDate)) {
            LocalDate date = LocalDate.parse(cDate, DateTimeFormatter.ISO_DATE);
            p.updateFinalDateAndDept(date.atStartOfDay(), row.getCurrCommittee());
            log.info("[{}] 업데이트: [{}] -> 소관위: {}, 회부일: {}", source, p.getTitle(), row.getCurrCommittee(), date);
        }
    }

    private boolean isValid(String str) {
        return str != null && !str.isBlank() && !str.equals("null");
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
                    int allows = (row.getCell(4).getCellType() == CellType.NUMERIC) ? (int) row.getCell(4).getNumericCellValue() : Integer.parseInt(row.getCell(4).getStringCellValue().replace(",", ""));
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