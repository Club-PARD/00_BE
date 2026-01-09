package com.youngyoung.server.mora.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youngyoung.server.mora.client.OpenAssemblyClient;
import com.youngyoung.server.mora.dto.AiResponseDto;
import com.youngyoung.server.mora.dto.OpenApiDto;
import com.youngyoung.server.mora.entity.*;
import com.youngyoung.server.mora.repo.*;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PetitionBatchTestService {

    private final PetitionRepo petitionRepo;
    private final LawsRepo lawsRepo;
    private final LawsLinkRepo lawsLinkRepo;
    private final OpenAssemblyClient openAssemblyClient;
    private final GptService gptService;
    private final ObjectMapper objectMapper;
    private final UserRepo userRepo;
    private final ScrapRepo scrapRepo;

    @Value("${open-api.assembly.key}")
    private String assemblyApiKey;

    // ======================================================================
    // 1. [테스트용] 처리현황 데이터 생성 (API)
    // ======================================================================
    @Transactional
    public void createDummyProcessedPetitions() {
        log.info(">>>> [TEST] 처리현황 API(21대) 30개 가져와서 저장 시작");
        try {
            String jsonResponse = openAssemblyClient.getProcessedPetitions(
                    assemblyApiKey, "json", 1, 30, "21", "국민동의청원", null
            );
            processAndSaveTestPetitions(jsonResponse, true);
        } catch (Exception e) {
            log.error("처리현황 테스트 데이터 생성 중 오류", e);
        }
    }

    // ======================================================================
    // 2. [테스트용] 계류현황 데이터 생성 (API)
    // ======================================================================
    @Transactional
    public void createDummyPendingPetitions() {
        log.info(">>>> [TEST] 계류현황 API 30개 가져와서 저장 시작");
        try {
            String jsonResponse = openAssemblyClient.getPendingPetitions(
                    assemblyApiKey, "json", 1, 30, null, null
            );
            processAndSaveTestPetitions(jsonResponse, false);
        } catch (Exception e) {
            log.error("계류현황 테스트 데이터 생성 중 오류", e);
        }
    }

    // ======================================================================
    // 3. [테스트용] 엑셀 상위 40개 가져오기 (크롤링 + GPT 포함)
    // ======================================================================
    @Transactional
    public void createFromExcelTop40() {
        log.info(">>>> [TEST] 엑셀 다운로드 후 상위 40개 데이터 수집 시작");

        // Selenium 설정
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
            log.info("엑셀 다운로드 요청...");

            File excelFile = new File(downloadPath, "진행중청원 목록.xlsx");
            waitForFileDownload(excelFile, 30);

            // 2. 엑셀 파싱 및 상위 40개 필터링
            List<ExcelRowDto> allPetitions = parseExcelAll(excelFile);
            List<ExcelRowDto> top40Petitions = allPetitions.stream().limit(40).toList();
            log.info("엑셀 파싱 완료. 상위 {}개 데이터를 처리합니다.", top40Petitions.size());

            // 3. URL 수집 (1~5 페이지 스캔)
            Map<String, String> titleUrlMap = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                js.executeScript("prgsPttList_search(" + i + ");");
                Thread.sleep(1500);
                List<WebElement> elements = driver.findElements(By.cssSelector("#prgsPttList-dataset-data-table a.board_subject100"));
                for (WebElement el : elements) {
                    String url = extractUrlFromOnclick(el.getAttribute("onclick"));
                    if (url != null) {
                        titleUrlMap.put(el.getText().trim().replaceAll("\\s+", ""), url);
                    }
                }
            }

            // 4. 상세 크롤링 & 저장
            int count = 0;
            for (ExcelRowDto dto : top40Petitions) {
                if (petitionRepo.findByTitle(dto.getTitle()).isPresent()) {
                    log.info("[SKIP] 이미 존재하는 청원: {}", dto.getTitle());
                    continue;
                }

                String lookupTitle = dto.getTitle().replaceAll("\\s+", "");
                String detailUrl = titleUrlMap.get(lookupTitle);
                if (detailUrl == null) detailUrl = findUrlByPartialMatch(titleUrlMap, dto.getTitle());

                if (detailUrl == null) {
                    log.warn("[SKIP] URL 못 찾음: {}", dto.getTitle());
                    continue;
                }

                driver.get(detailUrl);
                Thread.sleep(1500);
                String fullText = extractPetitionContent(driver);
                if (fullText.isBlank()) continue;

                // ★ GPT 분석 (안전 처리)
                AiResponseDto aiData;
                try {
                    aiData = gptService.analyzePetition(fullText);
                } catch (Exception e) {
                    log.error("GPT 오류 (청원: {}). 기본값 저장.", dto.getTitle());
                    aiData = AiResponseDto.builder()
                            .subTitle(dto.getTitle())
                            .needs("분석 실패: 원문을 확인해주세요.")
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
                        .url(detailUrl)
                        .petitionNeeds(aiData.getNeeds())
                        .petitionSummary(aiData.getSummary())
                        .positiveEx(String.join(",", posTags))
                        .negativeEx(String.join(",", negTags))
                        .type(1)
                        .status(0)
                        .result("-")
                        .department("-")
                        .good(0)
                        .bad(0)
                        .finalDate(null)
                        .build();

                Petition saved = petitionRepo.save(p);
                count++;
                log.info("[{}/40] 저장 완료: {}", count, saved.getTitle());

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
            log.error("엑셀 배치 작업 중 오류", e);
        } finally {
            driver.quit();
        }
    }

    // ======================================================================
    // 4. [테스트용] 이메일 발송 테스트 데이터 리셋
    // 설명: 유저의 스크랩 목록 중 '완료된(결과가 나온)' 청원을 찾아 '결과 없음(-)'으로 되돌림
    // ======================================================================
    @Transactional
    public String resetPetitionForEmailTest(String email) {
        // 1. 유저 찾기
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일의 유저를 찾을 수 없습니다: " + email));

        // 2. 해당 유저의 스크랩 목록 조회 (Scrap 엔티티 리스트)
        List<Scrap> scraps = scrapRepo.findAllByUserId(user.getId());

        if (scraps.isEmpty()) {
            return "실패: [" + email + "] 유저는 스크랩한 청원이 없습니다.";
        }

        Petition target = null;
        String originalResult = "";

        // 3. 스크랩한 청원 ID들로 실제 청원을 조회하여 조건 검사
        for (Scrap scrap : scraps) {
            Long petId = scrap.getPetId(); // Entity 구조상 getPetId() 사용

            // 청원 조회
            Optional<Petition> petOpt = Optional.ofNullable(petitionRepo.findById(petId));
            if (petOpt.isEmpty()) continue;

            Petition p = petOpt.get();

            // 조건: status가 1이고, result가 "-"가 아닌 것 (이미 결과가 나온 것)
            if (p.getStatus() == 1 && p.getResult() != null && !p.getResult().equals("-")) {
                target = p;
                originalResult = p.getResult();
                break; // 하나만 찾으면 됨
            }
        }

        // 4. 데이터 초기화 (배치가 다시 업데이트하도록 유도)
        if (target != null) {
            target.updateResult("-"); // 결과를 다시 "-"로 초기화
            // status는 1 그대로 유지 (완료/진행 구분 없이 1로 쓰기로 했으므로)

            // 안전장치: finalDate(회부일)가 없거나 미래라면, 배치가 조회를 안 할 수 있으므로 과거로 강제 설정
            if (target.getFinalDate() == null || target.getFinalDate().isAfter(LocalDateTime.now())) {
                target.updateFinalDateAndDept(LocalDateTime.now().minusDays(1), target.getDepartment());
            }

            petitionRepo.save(target);
            log.info("이메일 테스트 준비 완료: 청원[{}] (원래결과: {} -> 초기화됨)", target.getTitle(), originalResult);

            return "테스트 준비 완료! 대상 청원: [" + target.getTitle() + "]. 이제 /test/batch 를 실행하면 이메일이 발송됩니다.";
        } else {
            return "실패: [" + email + "] 님이 스크랩한 청원 중 '이미 처리된(결과가 나온)' 청원이 없습니다. 먼저 /test/processedBatch 로 데이터를 만들고 스크랩해주세요.";
        }
    }

    // ======================================================================
    // Helper Methods
    // ======================================================================

    private void processAndSaveTestPetitions(String jsonResponse, boolean isProcessedData) throws Exception {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--lang=ko_KR");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);

        try {
            OpenApiDto dto = objectMapper.readValue(jsonResponse, OpenApiDto.class);
            List<OpenApiDto.Row> rows;

            if (isProcessedData) {
                if (dto.getProcessedList() == null || dto.getProcessedList().isEmpty()) return;
                rows = dto.getProcessedList().get(1).getRow();
            } else {
                if (dto.getPendingList() == null || dto.getPendingList().isEmpty()) return;
                rows = dto.getPendingList().get(1).getRow();
            }

            for (OpenApiDto.Row row : rows) {
                if (petitionRepo.findByTitle(row.getBillName()).isPresent()) {
                    log.info("이미 존재하는 데이터 스킵: {}", row.getBillName());
                    continue;
                }

                String targetUrl = (row.getLinkUrl() != null && !row.getLinkUrl().isEmpty())
                        ? row.getLinkUrl() : "https://petitions.assembly.go.kr";

                driver.get(targetUrl);
                Thread.sleep(1500);
                String fullText = extractPetitionContent(driver);
                if (fullText.isBlank()) fullText = "테스트용 임시 본문입니다. " + row.getBillName();

                AiResponseDto aiData = gptService.analyzePetition(fullText);

                LocalDateTime startDate = parseDate(row.getProposeDt());
                LocalDateTime endDate = parseDate(row.getCommitteeDt());
                if (endDate == null) endDate = startDate.plusDays(30);

                // 1. 소관위 (Department)
                String department = row.getCurrCommittee();
                if (department == null || department.isBlank() || department.equals("null")) {
                    department = "-";
                }

                // 2. 회부일 (FinalDate)
                LocalDateTime finalDate = parseDate(row.getCommitteeDt());
                if (isProcessedData && finalDate == null) {
                    finalDate = LocalDateTime.now().minusMonths(6);
                }

                // ★ [수정됨] 3. 결과 (Result) - API 값 사용
                String result = row.getProcResultCd();
                if (result == null || result.isBlank() || result.equals("null")) {
                    result = "-";
                }

                // 4. 동의자 수 (Allows) - "김태인외 50,060인" 파싱
                int allows = extractAllowsFromProposer(row.getProposer());

                // 5. 카테고리 - "기타"로 변경
                String category = "기타";

                Petition petition = Petition.builder()
                        .title(row.getBillName())
                        .subTitle(aiData.getSubTitle())
                        .category(category) // 수정된 카테고리
                        .voteStartDate(startDate)
                        .voteEndDate(endDate)
                        .allows(allows) // 파싱된 동의자 수
                        .type(1)
                        .status(1)
                        .result(result) // 수정된 결과
                        .department(department)
                        .finalDate(finalDate)
                        .good(0)
                        .bad(0)
                        .url(targetUrl)
                        .petitionNeeds(aiData.getNeeds())
                        .petitionSummary(aiData.getSummary())
                        .positiveEx(String.join(",", aiData.getPositiveTags()))
                        .negativeEx(String.join(",", aiData.getNegativeTags()))
                        .build();

                petitionRepo.save(petition);
                log.info("저장 완료: {} (동의: {}, 결과: {})", petition.getTitle(), allows, result);

                if (aiData.getLaws() != null) {
                    for (AiResponseDto.LawDto lawDto : aiData.getLaws()) {
                        Laws law = lawsRepo.findByTitle(lawDto.getName())
                                .orElseGet(() -> lawsRepo.save(Laws.builder()
                                        .title(lawDto.getName()).summary(lawDto.getContent()).build()));
                        lawsLinkRepo.save(LawsLink.builder().petId(petition.getId()).lawId(law.getId()).build());
                    }
                }
            }
        } finally {
            driver.quit();
        }
    }

    // 동의자 수 파싱 헬퍼 메소드
    private int extractAllowsFromProposer(String proposer) {
        if (proposer == null || proposer.isBlank()) return 0;
        try {
            // "김태인외 50,060인" -> "50060" 추출 (숫자만 남김)
            String numberOnly = proposer.replaceAll("[^0-9]", "");
            if (numberOnly.isBlank()) return 0;
            return Integer.parseInt(numberOnly);
        } catch (NumberFormatException e) {
            return 0; // 파싱 실패 시 0 리턴
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
                    int allows = (row.getCell(4).getCellType() == CellType.NUMERIC)
                            ? (int) row.getCell(4).getNumericCellValue()
                            : Integer.parseInt(row.getCell(4).getStringCellValue().replace(",", ""));
                    result.add(ExcelRowDto.builder()
                            .category(category)
                            .title(title)
                            .voteStartDate(startDate.atStartOfDay())
                            .voteEndDate(endDate.atStartOfDay())
                            .allows(allows)
                            .build());
                } catch (Exception e) {}
            }
        } catch (Exception e) { log.error("엑셀 읽기 실패", e); }
        return result;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || dateStr.equals("null")) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE).atStartOfDay();
        } catch (Exception e) { return null; }
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

// (ExcelRowDto는 파일 맨 아래나 별도 파일에 위치)