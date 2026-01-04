package com.youngyoung.server.mora.dto;

import lombok.*;

import java.time.LocalDateTime;

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
}
