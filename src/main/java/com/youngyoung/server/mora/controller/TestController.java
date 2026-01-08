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

    //자정 확인
    @GetMapping("/batch")
    public String triggerBatch() {
        petitionBatchService.runBatch();
        return "배치 작업 수동 실행 완료!";
    }

    // 2. 처리현황 테스트 데이터 생성
    @GetMapping("/processedBatch")
    public String triggerProcessedBatch() {
        petitionBatchTestService.createDummyProcessedPetitions();
        return "처리현황 테스트 데이터 5개 생성 완료!";
    }

    // 3. 계류현황 테스트 데이터 생성
    @GetMapping("/pendingBatch")
    public String triggerPendingBatch() {
        petitionBatchTestService.createDummyPendingPetitions();
        return "계류현황 테스트 데이터 5개 생성 완료!";
    }
}