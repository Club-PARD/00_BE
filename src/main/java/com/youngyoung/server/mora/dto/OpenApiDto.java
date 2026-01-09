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

    // 1. 계류현황 리스트
    @JsonProperty("nvqbafvaajdiqhehi")
    private List<HeadRowWrapper> pendingList;

    // 2. 처리현황 리스트
    @JsonProperty("ncryefyuaflxnqbqo")
    private List<HeadRowWrapper> processedList;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HeadRowWrapper {
        private List<Row> row;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        @JsonProperty("BILL_NAME")
        private String billName;

        // ★ [추가됨] 청원인 (예: "김태인외 50,060인")
        @JsonProperty("PROPOSER")
        private String proposer;

        @JsonProperty("CURR_COMMITTEE") // 소관위
        private String currCommittee;

        @JsonProperty("PROPOSE_DT") // 제안일
        private String proposeDt;

        @JsonProperty("COMMITTEE_DT") // 회부일
        private String committeeDt;

        @JsonProperty("LINK_URL") // 상세 URL
        private String linkUrl;

        @JsonProperty("PROC_RESULT_CD") // 의결 결과
        private String procResultCd;
    }
}