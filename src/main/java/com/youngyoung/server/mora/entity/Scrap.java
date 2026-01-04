package com.youngyoung.server.mora.entity;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scrap {
    @Column(nullable = false)
    private Long petId;
    @Column(nullable = false)
    private Long userId;
}