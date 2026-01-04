package com.youngyoung.server.mora.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LawsLink {
    @Column(nullable = false)
    private Long petId;
    @Column(nullable = false)
    private Long lawId;
}
