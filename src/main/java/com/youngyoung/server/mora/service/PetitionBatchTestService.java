package com.youngyoung.server.mora.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youngyoung.server.mora.client.OpenAssemblyClient;
import com.youngyoung.server.mora.dto.AiResponseDto;
import com.youngyoung.server.mora.dto.OpenApiDto;
import com.youngyoung.server.mora.entity.Laws;
import com.youngyoung.server.mora.entity.LawsLink;
import com.youngyoung.server.mora.entity.Petition;
import com.youngyoung.server.mora.entity.Scrap;
import com.youngyoung.server.mora.entity.User;
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
import org.springframework.scheduling.annotation.Async;
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
public class PetitionBatchTestService {

    private final PetitionRepo petitionRepo;
    private final LawsRepo lawsRepo;
    private final LawsLinkRepo lawsLinkRepo;
    private final UserRepo userRepo;
    private final ScrapRepo scrapRepo;
    private final OpenAssemblyClient openAssemblyClient;
    private final GptService gptService;
    private final ObjectMapper objectMapper;

    @Value("${open-api.assembly.key}")
    private String assemblyApiKey;

    // ======================================================================
    // 1. [테스트용] 처리현황 데이터 생성 (API) - 비동기
    // ======================================================================
    @Async
    @Transactional
    public void createDummyProcessedPetitions() {
        log.info(">>>> [TEST] 처리현황 API(22대) 가져와서 저장 시작 (비동기)");
        try {
            // 처리현황 API는 파라미터로 "국민동의청원" 필터링 가능
            String jsonResponse = openAssemblyClient.getProcessedPetitions(
                    assemblyApiKey, "json", 1, 50, "22", "국민동의청원", null
            );
            processAndSaveTestPetitions(jsonResponse, true, 30);
        } catch (Exception e) {
            log.error("처리현황 테스트 데이터 생성 중 오류", e);
        }
    }

    // ======================================================================
    // 2. [테스트용] 계류현황 데이터 생성 (API) - 비동기 & 필터링 강화
    // ======================================================================
    @Async
    @Transactional
    public void createDummyPendingPetitions() {
        log.info(">>>> [TEST] 계류현황 API 가져와서 '국민동의청원'만 골라 저장 시작 (비동기)");
        try {
            // ★ [수정] 필터링을 위해 넉넉하게 50개를 가져옴 (파라미터로 필터링이 안 되므로)
            String jsonResponse = openAssemblyClient.getPendingPetitions(
                    assemblyApiKey, "json", 1, 50, null, null
            );
            // 내부 로직에서 "국민동의청원"인지 확인하고 30개만 저장
            processAndSaveTestPetitions(jsonResponse, false, 30);
        } catch (Exception e) {
            log.error("계류현황 테스트 데이터 생성 중 오류", e);
        }
    }

    // ======================================================================
    // 3. [테스트용] 엑셀 상위 40개 가져오기 (크롤링 + GPT 포함) - 비동기
    // ======================================================================
    @Async
    @Transactional
    public void createFromExcelTop40() {
        log.info(">>>> [TEST] 엑셀 다운로드 후 상위 40개 데이터 수집 시작 (비동기)");

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

            // 2. 엑셀 파싱
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

                // GPT 분석 (안전 처리)
                AiResponseDto aiData;
                try {
                    aiData = gptService.analyzePetition(fullText);
                } catch (Exception e) {
                    log.error("GPT 오류 (청원: {}). 기본값 저장.", dto.getTitle());
                    aiData = AiResponseDto.builder()
                            .subTitle(dto.getTitle())
                            .needs("분석 실패: 원문을 확인해주세요.")
                            .summary("AI 분석 실패")
                            .positiveTags(List.of("분석 실패"))
                            .negativeTags(List.of("분석 실패"))
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
    // ======================================================================
    @Transactional
    public String resetPetitionForEmailTest(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일의 유저를 찾을 수 없습니다: " + email));

        List<Scrap> scraps = scrapRepo.findAllByUserId(user.getId());
        if (scraps.isEmpty()) {
            return "실패: [" + email + "] 유저는 스크랩한 청원이 없습니다.";
        }

        Petition target = null;
        String originalResult = "";

        for (Scrap scrap : scraps) {
            // Scrap 엔티티에는 연관관계가 없으므로 ID로 다시 조회
            Long petId = scrap.getPetId();
            Petition p = petitionRepo.findById(petId);

            if (p != null && p.getStatus() == 1 && p.getResult() != null && !p.getResult().equals("-")) {
                target = p;
                originalResult = p.getResult();
                break;
            }
        }

        if (target != null) {
            target.updateResult("-");
            // status는 1 유지

            // finalDate가 미래면 배치가 조회 안 하므로 과거로 변경
            if (target.getFinalDate() == null || target.getFinalDate().isAfter(LocalDateTime.now())) {
                target.updateFinalDateAndDept(LocalDateTime.now().minusDays(1), target.getDepartment());
            }

            petitionRepo.save(target);
            log.info("이메일 테스트 준비 완료: 청원[{}] (원래결과: {} -> 초기화됨)", target.getTitle(), originalResult);
            return "테스트 준비 완료! 청원명: [" + target.getTitle() + "]. 이제 /test/batch 를 실행하세요.";
        } else {
            return "실패: [" + email + "] 님이 스크랩한 청원 중 '이미 처리된' 청원이 없습니다.";
        }
    }

    // ======================================================================
    // 5. [테스트용] 청원24(cheongwon.go.kr) 단일 링크 크롤링 (텍스트 파싱 방식)
    // ======================================================================
    @Async
    @Transactional
    public void createFromCheongwon24(String targetUrl) {
        log.info(">>>> [TEST] 청원24 크롤링 시작: {}", targetUrl);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--lang=ko_KR");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        try {
            driver.get(targetUrl);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            Thread.sleep(2000);

            // 페이지 전체 텍스트 가져오기
            String bodyText = driver.findElement(By.tagName("body")).getText();
            String[] lines = bodyText.split("\n");

            // 1. 제목 (Title) - "처리기관" 바로 윗줄 찾기
            String title = "제목 파싱 실패";
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].contains("처리기관") && i > 0) {
                    // "처리기관"이 있는 줄의 바로 윗줄이 제목일 확률 99%
                    String candidate = lines[i-1].trim();

                    // 만약 윗줄이 "종결", "처리 중" 같은 상태값이라면 그 윗줄을 다시 확인
                    if (candidate.equals("종결") || candidate.equals("처리 중") || candidate.equals("의견수렴 중") || candidate.equals("접수")) {
                        if (i > 1) candidate = lines[i-2].trim();
                    }

                    title = candidate;
                    break;
                }
            }
            // 그래도 실패하면 기존 방식 시도
            if (title.equals("제목 파싱 실패")) {
                try {
                    title = driver.findElement(By.cssSelector(".view_tit")).getText().trim();
                    if (title.contains("\n")) title = title.split("\n")[title.split("\n").length - 1].trim();
                } catch (Exception e) {}
            }

            // 2. 처리기관 (Department)
            String department = "-";
            for (String line : lines) {
                if (line.contains("처리기관") && line.contains(":")) {
                    department = line.split(":")[1].trim(); // "처리기관: 조달청..." -> "조달청..."
                    break;
                }
            }

            // 3. 의견 수렴 기간 (Vote Date)
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = LocalDateTime.now().plusDays(30);
            for (String line : lines) {
                if (line.contains("의견 수렴 기간")) {
                    // "의견 수렴 기간 : 2026.01.09.~2026.02.09."
                    Pattern pattern = Pattern.compile("(\\d{4}\\.\\d{2}\\.\\d{2}\\.)");
                    Matcher matcher = pattern.matcher(line);
                    List<String> dates = new ArrayList<>();
                    while (matcher.find()) dates.add(matcher.group(1));

                    if (dates.size() >= 2) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
                        startDate = LocalDate.parse(dates.get(0), formatter).atStartOfDay();
                        endDate = LocalDate.parse(dates.get(1), formatter).atStartOfDay();
                    }
                    break;
                }
            }

            // 4. 동의자 수 (Allows)
            int allows = 0;
            for (String line : lines) {
                if (line.contains("의견이 총") && line.contains("건")) {
                    String numStr = line.replaceAll("[^0-9]", "");
                    if (!numStr.isEmpty()) allows = Integer.parseInt(numStr);
                    break;
                }
            }

            // 5. 본문 (Content)
            String fullText = "";
            try {
                // 본문 영역은 여전히 Selector로 찾는 게 가장 깔끔함
                List<String> contentSelectors = List.of(".view_cont", ".pet_content", ".content_view");
                for (String selector : contentSelectors) {
                    try {
                        List<WebElement> els = driver.findElements(By.cssSelector(selector));
                        if (!els.isEmpty()) {
                            fullText = els.get(0).getText().trim();
                            if (!fullText.isEmpty()) break;
                        }
                    } catch (Exception ignored) {}
                }
                // 실패시 전체 텍스트 사용 (이미 위에서 가져온 bodyText 활용)
                if (fullText.isEmpty()) {
                    fullText = bodyText;
                    if (fullText.length() > 5000) fullText = fullText.substring(0, 5000);
                }
            } catch (Exception e) {
                fullText = "본문 파싱 실패";
            }

            // 중복 검사
            if (petitionRepo.findByTitle(title).isPresent()) {
                log.info("이미 존재하는 청원 스킵: {}", title);
                return;
            }

            // 6. GPT 분석
            AiResponseDto aiData;
            try {
                aiData = gptService.analyzePetition(fullText);
            } catch (Exception e) {
                log.error("GPT 분석 실패. 기본값 저장.");
                aiData = AiResponseDto.builder()
                        .subTitle(title)
                        .needs("분석 실패")
                        .summary("AI 분석 실패")
                        .positiveTags(List.of("분석 실패"))
                        .negativeTags(List.of("분석 실패"))
                        .laws(null)
                        .build();
            }

            List<String> posTags = aiData.getPositiveTags() != null ? aiData.getPositiveTags() : new ArrayList<>();
            List<String> negTags = aiData.getNegativeTags() != null ? aiData.getNegativeTags() : new ArrayList<>();

            // 7. DB 저장
            Petition petition = Petition.builder()
                    .title(title)
                    .subTitle(aiData.getSubTitle())
                    .category("기타")
                    .voteStartDate(startDate)
                    .voteEndDate(endDate)
                    .allows(allows)
                    .type(0)
                    .status(0)
                    .result("-")
                    .department(department)
                    .finalDate(null)
                    .good(0)
                    .bad(0)
                    .url(targetUrl)
                    .petitionNeeds(aiData.getNeeds())
                    .petitionSummary(aiData.getSummary())
                    .positiveEx(String.join(",", posTags))
                    .negativeEx(String.join(",", negTags))
                    .build();

            Petition saved = petitionRepo.save(petition);
            log.info("청원24 저장 완료: {} (ID: {})", saved.getTitle(), saved.getId());

            if (aiData.getLaws() != null) {
                for (AiResponseDto.LawDto lawDto : aiData.getLaws()) {
                    Laws law = lawsRepo.findByTitle(lawDto.getName())
                            .orElseGet(() -> lawsRepo.save(Laws.builder()
                                    .title(lawDto.getName()).summary(lawDto.getContent()).build()));
                    lawsLinkRepo.save(LawsLink.builder().petId(saved.getId()).lawId(law.getId()).build());
                }
            }

        } catch (Exception e) {
            log.error("청원24 처리 중 오류 발생", e);
        } finally {
            driver.quit();
        }
    }

    // ======================================================================
    // 6. [보정용] 기존 완료된 청원들의 날짜(VoteStart, VoteEnd, Final) 수정
    // ======================================================================
    @Async
    @Transactional
    public void fixExistingPetitionDates() {
        log.info(">>>> [FIX] 기존 완료 청원 날짜 보정 작업 시작");

        // 1. 대상 조회: Type=1(국회) 이면서 결과가 있는(Result != "-") 청원들
        List<Petition> targets = petitionRepo.findByTypeAndResultNot(1, "-");
        log.info("보정 대상 청원 수: {}개", targets.size());

        if (targets.isEmpty()) return;

        // Selenium 설정 (한 번만 켜서 재사용)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--lang=ko_KR");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);

        try {
            int count = 0;
            for (Petition p : targets) {
                count++;
                log.info("[{}/{}] 보정 중: {}", count, targets.size(), p.getTitle());

                // -------------------------------------------------
                // A. API 재조회 -> finalDate (회부일) 수정
                // -------------------------------------------------
                LocalDateTime newFinalDate = p.getFinalDate(); // 기본은 기존값 유지
                try {
                    // API에서 22대/21대 모두 검색
                    OpenApiDto.Row row = searchApiForFix(p.getTitle(), "22");
                    if (row == null) row = searchApiForFix(p.getTitle(), "21");

                    if (row != null) {
                        // COMMITTEE_DT(회부일)가 있으면 그걸로 교체
                        LocalDateTime apiDate = parseDate(row.getCommitteeDt());
                        if (apiDate != null) {
                            newFinalDate = apiDate;
                        }
                    }
                } catch (Exception e) {
                    log.warn("API 조회 실패: {}", p.getTitle());
                }

                // -------------------------------------------------
                // B. 크롤링 -> voteStartDate, voteEndDate 수정
                // -------------------------------------------------
                LocalDateTime newStartDate = p.getVoteStartDate();
                LocalDateTime newEndDate = p.getVoteEndDate();

                try {
                    driver.get(p.getUrl());
                    // 상세 페이지 로딩 대기 (너무 빠르면 차단될 수 있으니 1초 대기)
                    Thread.sleep(1000);

                    // 기존에 만들어둔 extractVoteDates 메소드 재활용
                    LocalDateTime[] voteDates = extractVoteDates(driver);

                    if (voteDates != null) {
                        newStartDate = voteDates[0];
                        newEndDate = voteDates[1];
                    }
                } catch (Exception e) {
                    log.warn("크롤링 실패: {}", p.getTitle());
                }

                // -------------------------------------------------
                // C. DB 업데이트 (Dirty Checking으로 자동 저장됨)
                // -------------------------------------------------
                // Entity에 updateDates 편의 메소드를 추가하거나, Setter 사용 (여기선 Setter 가정)
                // * Petition Entity에 아래 메소드들이 없다면 추가 필요 *

                // 1. finalDate 업데이트 (Department는 기존꺼 유지)
                p.updateFinalDateAndDept(newFinalDate, p.getDepartment());

                // 2. 투표 기간 업데이트 (Entity에 메소드 추가 필요, 없으면 아래처럼 직접 수정 불가)
                // -> Petition Entity에 updateVoteDates 메소드를 추가해주세요!
                p.updateVoteDates(newStartDate, newEndDate);

                log.info(" -> 업데이트 완료 (Start: {}, End: {}, Final: {})",
                        newStartDate.toLocalDate(), newEndDate.toLocalDate(),
                        (newFinalDate != null ? newFinalDate.toLocalDate() : "null"));
            }
        } catch (Exception e) {
            log.error("보정 작업 중 치명적 오류", e);
        } finally {
            driver.quit();
            log.info(">>>> [FIX] 보정 작업 종료");
        }
    }

    // [보정용 Helper] API 검색 (기존 로직 단순화)
    private OpenApiDto.Row searchApiForFix(String title, String age) {
        try {
            // 처리현황 API 조회
            String jsonResponse = openAssemblyClient.getProcessedPetitions(
                    assemblyApiKey, "json", 1, 100, age, null, null
            );
            OpenApiDto dto = objectMapper.readValue(jsonResponse, OpenApiDto.class);

            if (dto.getProcessedList() != null && !dto.getProcessedList().isEmpty()) {
                List<OpenApiDto.Row> rows = dto.getProcessedList().get(1).getRow();
                String normDbTitle = title.replaceAll("\\s+", "");

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

    // ======================================================================
    // Helper Method: API 응답 처리 및 저장 (공통)
    // ======================================================================
    private void processAndSaveTestPetitions(String jsonResponse, boolean isProcessedData, int limitCount) throws Exception {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--lang=ko_KR");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        WebDriver driver = new ChromeDriver(options);
        int savedCount = 0;

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
                if (savedCount >= limitCount) break;

                // ★ [로그 추가] API에서 날짜값이 어떻게 오는지 확인
                log.info(">>>> [API 데이터 확인] 청원명: {}, 접수일(PROPOSE): {}, 회부일(COMMITTEE): {}",
                        row.getBillName(), row.getProposeDt(), row.getCommitteeDt());

                // 국민동의청원 필터링
                if (row.getApprover() != null && !row.getApprover().contains("국민동의청원")) continue;
                if (petitionRepo.findByTitle(row.getBillName()).isPresent()) continue;

                log.info("데이터 생성 중({}/{}): {}", savedCount + 1, limitCount, row.getBillName());

                String targetUrl = (row.getLinkUrl() != null && !row.getLinkUrl().isEmpty())
                        ? row.getLinkUrl() : "https://petitions.assembly.go.kr";

                driver.get(targetUrl);
                Thread.sleep(1500); // 로딩 대기

                // 1. 본문 추출
                String fullText = extractPetitionContent(driver);
                if (fullText.isBlank()) fullText = "테스트용 임시 본문입니다. " + row.getBillName();

                // 2. ★ [추가] 청원 기간(Vote Date) 크롤링
                LocalDateTime[] voteDates = extractVoteDates(driver);

                // 기본값 (API 데이터) 설정
                LocalDateTime startDate = parseDate(row.getProposeDt());
                LocalDateTime endDate = parseDate(row.getCommitteeDt());
                if (endDate == null && startDate != null) endDate = startDate.plusDays(30);

                // 크롤링 성공 시 덮어쓰기
                if (voteDates != null) {
                    startDate = voteDates[0];
                    endDate = voteDates[1];
                    log.info("청원기간 크롤링 성공: {} ~ {}", startDate.toLocalDate(), endDate.toLocalDate());
                } else {
                    log.warn("청원기간 크롤링 실패 -> API 데이터 대체 사용");
                }

                AiResponseDto aiData = gptService.analyzePetition(fullText);

                String department = row.getCurrCommittee();
                if (department == null || department.isBlank() || department.equals("null")) {
                    department = "-";
                }

                LocalDateTime finalDate = parseDate(row.getCommitteeDt());
                if (isProcessedData && finalDate == null) {
                    finalDate = LocalDateTime.now().minusMonths(6);
                }

                String result = row.getProcResultCd();
                if (result == null || result.isBlank() || result.equals("null")) {
                    result = "-";
                }

                int allows = extractAllowsFromProposer(row.getProposer());
                String category = "기타";

                Petition petition = Petition.builder()
                        .title(row.getBillName())
                        .subTitle(aiData.getSubTitle())
                        .category(category)
                        .voteStartDate(startDate) // 크롤링된 날짜 적용
                        .voteEndDate(endDate)     // 크롤링된 날짜 적용
                        .allows(allows)
                        .type(1)
                        .status(1)
                        .result(result)
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
                savedCount++;
                log.info("저장 완료: {} (동의: {}, 기간: {}~{})", petition.getTitle(), allows, startDate.toLocalDate(), endDate.toLocalDate());

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

    private int extractAllowsFromProposer(String proposer) {
        if (proposer == null || proposer.isBlank()) return 0;
        try {
            String numberOnly = proposer.replaceAll("[^0-9]", "");
            if (numberOnly.isBlank()) return 0;
            return Integer.parseInt(numberOnly);
        } catch (NumberFormatException e) {
            return 0;
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

    // 청원24 상세 페이지에서 "청원기간" 추출
    private LocalDateTime[] extractVoteDates(WebDriver driver) {
        try {
            // 1. 페이지 전체 텍스트 가져오기
            String bodyText = driver.findElement(By.tagName("body")).getText();

            // 2. 날짜 패턴 찾기 (YYYY-MM-DD ~ YYYY-MM-DD)
            // 예: "청원기간 2025-03-17 ~ 2025-04-16"
            Pattern pattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*~\\s*(\\d{4}-\\d{2}-\\d{2})");
            Matcher matcher = pattern.matcher(bodyText);

            if (matcher.find()) {
                String startStr = matcher.group(1); // 첫 번째 날짜
                String endStr = matcher.group(2);   // 두 번째 날짜

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                return new LocalDateTime[] {
                        LocalDate.parse(startStr, formatter).atStartOfDay(),
                        LocalDate.parse(endStr, formatter).atStartOfDay()
                };
            }
        } catch (Exception e) {
            log.warn("청원기간 파싱 중 오류 발생: {}", e.getMessage());
        }
        return null; // 파싱 실패 시 null 반환
    }
}
