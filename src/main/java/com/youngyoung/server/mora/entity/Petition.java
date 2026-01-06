package com.youngyoung.server.mora.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Petition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String title;
    @Column(nullable = false)
    private Integer type;
    @Column(nullable = false)
    private Integer status;
    @Column(nullable = false)
    private String petitionSummary;
    @Column(nullable = false)
    private String result;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false)
    private LocalDateTime voteStartDate;
    @Column(nullable = false)
    private LocalDateTime voteEndDate;
    @Column(nullable = false)
    private LocalDateTime lastUpdateDate;
    @Column(nullable = false)
    private String positiveEx;
    @Column(nullable = false)
    private String negativeEx;
    @Column(nullable = false)
    private Integer good=0;
    @Column(nullable = false)
    private Integer bad=0;
    @Column(nullable = false)
    private Integer allows;

    public long getId() {
        return id;
    }

    public void updateLikes(Integer likes) {
        if(likes>0) this.good += likes;
        else this.bad -= likes;
    }
}
