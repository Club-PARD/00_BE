package com.youngyoung.server.mora.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "openAssemblyClient", url = "https://open.assembly.go.kr/portal/openapi")
public interface OpenAssemblyClient {

    // 1. 계류현황 API (★ AGE 추가됨)
    @GetMapping("/nvqbafvaajdiqhehi")
    String getPendingPetitions(
            @RequestParam("KEY") String key,
            @RequestParam("Type") String type,
            @RequestParam("pIndex") int pIndex,
            @RequestParam("pSize") int pSize,
            @RequestParam("AGE") String age, // ★ 여기 추가!
            @RequestParam(value = "BILL_NAME", required = false) String billName
    );

    // 2. 처리현황 API (기존 유지)
    @GetMapping("/ncryefyuaflxnqbqo")
    String getProcessedPetitions(
            @RequestParam("KEY") String key,
            @RequestParam("Type") String type,
            @RequestParam("pIndex") int pIndex,
            @RequestParam("pSize") int pSize,
            @RequestParam("AGE") String age,
            @RequestParam(value = "APPROVER", required = false) String approver,
            @RequestParam(value = "BILL_NAME", required = false) String billName
    );
}