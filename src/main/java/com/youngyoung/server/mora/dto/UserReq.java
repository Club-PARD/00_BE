package com.youngyoung.server.mora.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

public class UserReq {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class UserInfo{
        @JsonProperty("name")
        private String name;
        @JsonProperty("email")
        private String email;
        @JsonProperty("age")
        private Integer age;
        @JsonProperty("status")
        private Integer status;
    }
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class UserUpdateInfo{
        @JsonProperty("name")
        private String name;
        @JsonProperty("age")
        private Integer age;
        @JsonProperty("status")
        private Integer status;
    }
}
