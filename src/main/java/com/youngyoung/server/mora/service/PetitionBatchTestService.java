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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    @Value("${open-api.assembly.key}")
    private String assemblyApiKey;

    // 1. [테스트용] 처리현황 데이터 생성
    @Transactional
    public void createDummyProcessedPetitions() {
        log.info(">>>> [TEST] 처리현황 API(21대) 5개 가져와서 '처리 전' 상태로 저장 시작");
        try {
            String jsonResponse = openAssemblyClient.getProcessedPetitions(
                    assemblyApiKey, "json", 1, 5, "21", "국민동의청원", null
            );
            processAndSaveTestPetitions(jsonResponse, true);
        } catch (Exception e) {
            log.error("처리현황 테스트 데이터 생성 중 오류", e);
        }
    }

    // 2. [테스트용] 계류현황 데이터 생성
    @Transactional
    public void createDummyPendingPetitions() {
        log.info(">>>> [TEST] 계류현황 API 5개 가져와서 '소관위 미정' 상태로 저장 시작");
        try {
            String jsonResponse = openAssemblyClient.getPendingPetitions(
                    assemblyApiKey, "json", 1, 5, null
            );
            processAndSaveTestPetitions(jsonResponse, false);
        } catch (Exception e) {
            log.error("계류현황 테스트 데이터 생성 중 오류", e);
        }
    }

    // 공통 로직
    private void processAndSaveTestPetitions(String jsonResponse, boolean isProcessedData) throws Exception {
        // Selenium 설정
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

                log.info("테스트 데이터 생성 중: {}", row.getBillName());

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

                // =========================================================
                // ★ [수정됨] API 값이 있으면 넣고, 없으면 테스트용 기본값 사용
                // =========================================================

                // 1. 소관위 (Department)
                String department = row.getCurrCommittee();
                // API 값이 없거나 "null" 문자열이면 기본값 "-" 사용
                if (department == null || department.isBlank() || department.equals("null")) {
                    department = "-";
                }

                // 2. 회부일 (FinalDate)
                LocalDateTime finalDate = parseDate(row.getCommitteeDt());

                // [처리현황 테스트인 경우]
                // API에 날짜가 있으면 그대로 쓰고(finalDate),
                // 없으면(null) -> '업데이트 로직 테스트'를 위해 강제로 과거 날짜를 부여
                if (isProcessedData && finalDate == null) {
                    finalDate = LocalDateTime.now().minusMonths(6);
                }

                // [계류현황 테스트인 경우]
                // API에 날짜가 있으면 그대로 쓰고,
                // 없으면(null) -> '소관위/날짜 업데이트 로직 테스트'를 위해 null 상태 유지 (그래야 배치에서 잡힘)

                // 저장
                Petition petition = Petition.builder()
                        .title(row.getBillName())
                        .subTitle(aiData.getSubTitle())
                        .category("기타(테스트)")
                        .voteStartDate(startDate)
                        .voteEndDate(endDate)
                        .allows(50000)
                        .type(1)
                        .status(1)        // 1: 종료된 상태
                        .result("-")      // "-": 결과 미정
                        .department(department) // ★ API 값 우선 적용
                        .finalDate(finalDate)   // ★ API 값 우선 적용
                        .good(0)
                        .bad(0)
                        .url(targetUrl)
                        .petitionNeeds(aiData.getNeeds())
                        .petitionSummary(aiData.getSummary())
                        .positiveEx(String.join(",", aiData.getPositiveTags()))
                        .negativeEx(String.join(",", aiData.getNegativeTags()))
                        .build();

                petitionRepo.save(petition);
                log.info("저장 완료: {} (소관위: {}, 회부일: {})", petition.getTitle(), department, finalDate);

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

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || dateStr.equals("null")) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractPetitionContent(WebDriver driver) {
        try {
            WebElement body = driver.findElement(By.tagName("body"));
            String text = body.getText();
            return (text != null) ? text : "";
        } catch (Exception ignored) { return ""; }
    }
}