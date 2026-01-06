package com.youngyoung.server.mora.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class PetitionRes {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class PetitionInfo{
        private String title;
        private Integer type;
        private String petitionSummary;
        private Integer status;
        private String category;
        private LocalDateTime voteStartDate;
        private LocalDateTime voteEndDate;
        private String result;
        private String positiveEx;
        private String negativeEx;
        private Integer good;
        private Integer bad;
        private Integer allows;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CardNewsInfo{
        private Long id;
        private String title;
        private Integer type;
        private Integer status;
        private String category;
        private LocalDateTime voteStartDate;
        private LocalDateTime voteEndDate;
        private Integer allows;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CardNewsTotal{
        private Integer totalElements;
        private Integer totalPages;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class NewsInfo{
        private String title;
        private String url;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class LawsInfo{
        private String title;
        private String summary;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CommentInfo {
        private Long id;
        private String name;
        private String body;
        private Boolean check;
    }
}
