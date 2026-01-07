package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.service.PetitionBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final PetitionBatchService petitionBatchService;

    @GetMapping("/test/batch")
    public String triggerBatch() {
        petitionBatchService.runBatch();
        return "배치 작업 수동 실행 완료!";
    }
}