package com.youngyoung.server.mora.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class AiResponseDto {
    private String needs; // 청원 개요
    private String summary; // 한줄 요약
    private List<String> positiveTags; // 긍정 키워드
    private List<String> negativeTags; // 부정 키워드
    private List<LawDto> laws; // 관련 법안 목록

    @Getter
    @NoArgsConstructor
    public static class LawDto {
        private String name; // 법안 명칭
        private String content; // 조문 내용
    }
}