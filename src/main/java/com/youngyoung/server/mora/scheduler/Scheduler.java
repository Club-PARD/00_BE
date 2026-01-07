package com.youngyoung.server.mora.scheduler;

import com.youngyoung.server.mora.service.PetitionBatchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Scheduler {

    private final PetitionBatchService petitionBatchService;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    public void scheduleRun() {
        petitionBatchService.runBatch();
    }

    // ★ 추가: 서버 시작되자마자 테스트로 한 번 실행!
//    @PostConstruct
//    public void initTest() {
//        System.out.println("서버 시작! 테스트용 배치 즉시 실행...");
//        petitionBatchService.runBatch();
//    }
}
