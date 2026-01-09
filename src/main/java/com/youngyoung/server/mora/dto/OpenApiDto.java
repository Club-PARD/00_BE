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

    @JsonProperty("nvqbafvaajdiqhehi")
    private List<HeadRowWrapper> pendingList;

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

        @JsonProperty("PROPOSER")
        private String proposer;

        // ★ [추가] 소개의원 (여기에 "국민동의청원"이라고 적혀있음)
        @JsonProperty("APPROVER")
        private String approver;

        @JsonProperty("CURR_COMMITTEE")
        private String currCommittee;

        @JsonProperty("PROPOSE_DT")
        private String proposeDt;

        @JsonProperty("COMMITTEE_DT")
        private String committeeDt;

        @JsonProperty("LINK_URL")
        private String linkUrl;

        @JsonProperty("PROC_RESULT_CD")
        private String procResultCd;
    }
}