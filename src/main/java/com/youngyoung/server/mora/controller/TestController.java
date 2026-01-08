package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.service.PetitionBatchService;
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

    @GetMapping("/batch")
    public String triggerBatch() {
        petitionBatchService.runBatch();
        return "배치 작업 수동 실행 완료!";
    }
}