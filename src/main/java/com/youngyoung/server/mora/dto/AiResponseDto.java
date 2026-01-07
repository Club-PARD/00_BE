package com.youngyoung.server.mora.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiResponseDto {
    private String subTitle; // 훅킹 문구
    private String needs;    // 청원 개요
    private String summary;  // 청원 요약
    private List<String> positiveTags;
    private List<String> negativeTags;
    private List<LawDto> laws;

    @Getter
    @NoArgsConstructor
    public static class LawDto {
        private String name;
        private String content;
    }
}