package com.youngyoung.server.mora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

public class PetitionRes {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class CardNewsInfo{
        private String id;
        private String title;
        private Integer type;
        private Integer status;
        private Integer views;
        private String category;
        private LocalDateTime voteStartDate;
        private LocalDateTime voteEndDate;
        private Integer totalPages;
        private Integer totalElements;
        private Integer allows;
    }
}
