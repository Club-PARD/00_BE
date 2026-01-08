package com.youngyoung.server.mora.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenApiDto {

    // 계류현황 루트
    @JsonProperty("nvqbafvaajdiqhehi")
    private List<HeadRowWrapper> pendingList;

    // 처리현황 루트
    @JsonProperty("ncryefyuaflxnqbqo")
    private List<HeadRowWrapper> processedList;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeadRowWrapper {
        private List<Row> row; // 실제 데이터 리스트
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        @JsonProperty("BILL_NAME")
        private String billName;

        @JsonProperty("CURR_COMMITTEE") // 소관위
        private String currCommittee;

        @JsonProperty("COMMITTEE_DT") // 위원회 회부일 (String: "2024-01-01")
        private String committeeDt;

        @JsonProperty("PROC_RESULT_CD") // 의결 결과
        private String procResultCd;
    }
}