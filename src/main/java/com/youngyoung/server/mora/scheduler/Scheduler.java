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

    @Scheduled(cron = "0 10 0 * * *") // 매일 자정
    public void scheduleRun() {
        petitionBatchService.runBatch();
    }

}
