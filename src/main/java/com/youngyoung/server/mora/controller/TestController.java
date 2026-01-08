package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.service.PetitionBatchService;
import com.youngyoung.server.mora.service.PetitionBatchTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequestMapping("/test")
@RestController
@RequiredArgsConstructor
public class TestController {

    private final PetitionBatchService petitionBatchService;
    private final PetitionBatchTestService petitionBatchTestService;

    // 1. 메인 배치 수동 실행
    @GetMapping("/batch")
    public String triggerBatch() {
        petitionBatchService.runBatch();
        return "배치 작업 실행 완료! (로그 확인)";
    }

    // 2. 처리현황 테스트 데이터 생성 (API)
    @GetMapping("/processedBatch")
    public String triggerProcessedBatch() {
        petitionBatchTestService.createDummyProcessedPetitions();
        return "처리현황 테스트 데이터 5개 생성 완료!";
    }

    // 3. 계류현황 테스트 데이터 생성 (API)
    @GetMapping("/pendingBatch")
    public String triggerPendingBatch() {
        petitionBatchTestService.createDummyPendingPetitions();
        return "계류현황 테스트 데이터 5개 생성 완료!";
    }

    // 4. 엑셀 상위 40개 크롤링 및 저장
    @GetMapping("/excel40")
    public String triggerExcelTop40() {
        petitionBatchTestService.createFromExcelTop40();
        return "엑셀 상위 40개 데이터 수집 및 저장 완료!";
    }
}