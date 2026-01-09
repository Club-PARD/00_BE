package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.service.PetitionBatchService;
import com.youngyoung.server.mora.service.PetitionBatchTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        return "처리현황 테스트 데이터 생성을 '백그라운드'에서 시작했습니다! (완료 여부는 서버 로그를 확인하세요)";
    }

    // 3. 계류현황 테스트 데이터 생성 (API)
    @GetMapping("/pendingBatch")
    public String triggerPendingBatch() {
        petitionBatchTestService.createDummyPendingPetitions();
        return "계류현황 테스트 데이터 생성을 '백그라운드'에서 시작했습니다! (완료 여부는 서버 로그를 확인하세요)";
    }

    // 4. 엑셀 상위 40개 크롤링 및 저장
    @GetMapping("/excel40")
    public String triggerExcelTop40() {
        petitionBatchTestService.createFromExcelTop40();
        return "엑셀 상위 40개 데이터 수집 및 저장 완료!";
    }

    // 5. 이메일 발송 테스트를 위한 데이터 초기화 API
    // 사용법: /test/prepare-email?email=my@gmail.com
    @GetMapping("/prepare-email")
    public String prepareEmailTest(@RequestParam("email") String email) {
        return petitionBatchTestService.resetPetitionForEmailTest(email);
    }

    // 6. ★ [추가] 청원24(cheongwon.go.kr) 단일 링크 크롤링 및 저장
    // 사용법: /test/cheongwon24?url=https://cheongwon.go.kr/...
    @GetMapping("/cheongwon24")
    public String createFromCheongwon24(@RequestParam("url") String url) {
        petitionBatchTestService.createFromCheongwon24(url);
        return "청원24 데이터 수집 및 저장 작업이 백그라운드에서 시작되었습니다. 로그를 확인하세요.";
    }
}