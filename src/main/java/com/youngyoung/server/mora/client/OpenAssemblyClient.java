package com.youngyoung.server.mora.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

// url은 공통 도메인만 넣습니다.
@FeignClient(name = "openAssemblyClient", url = "https://open.assembly.go.kr/portal/openapi")
public interface OpenAssemblyClient {

    // 1. 계류현황 API
    @GetMapping("/nvqbafvaajdiqhehi")
    String getPendingPetitions(
            @RequestParam("KEY") String key,
            @RequestParam("Type") String type, // json
            @RequestParam("pIndex") int pIndex,
            @RequestParam("pSize") int pSize,
            @RequestParam(value = "BILL_NAME", required = false) String billName
    );

    // 2. 처리현황 API
    @GetMapping("/ncryefyuaflxnqbqo")
    String getProcessedPetitions(
            @RequestParam("KEY") String key,
            @RequestParam("Type") String type,
            @RequestParam("pIndex") int pIndex,
            @RequestParam("pSize") int pSize,
            @RequestParam(value = "BILL_NAME", required = false) String billName
    );
}