package com.youngyoung.server.mora.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder // ★ 이 어노테이션이 있어야 .builder()를 쓸 수 있습니다.
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseDto {
    private String subTitle;
    private String needs;
    private String summary;
    private List<String> positiveTags;
    private List<String> negativeTags;
    private List<LawDto> laws;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LawDto {
        private String name;
        private String content;
    }
}