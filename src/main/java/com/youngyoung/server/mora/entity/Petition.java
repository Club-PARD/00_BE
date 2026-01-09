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
    private String subTitle;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String petitionNeeds;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String petitionSummary;
    @Column(nullable = false)
    private String result;
    @Column(nullable = false)
    private String category;

    private LocalDateTime finalDate;
    @Column(nullable = false)
    private LocalDateTime voteStartDate;
    @Column(nullable = false)
    private LocalDateTime voteEndDate;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String positiveEx;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String negativeEx;
    @Column(nullable = false)
    private Integer good=0;
    @Column(nullable = false)
    private Integer bad=0;
    @Column(nullable = false)
    private Integer allows;
    @Column(nullable = false)
    private String url;
    @Column(nullable = false)
    private String department;

    public long getId() {
        return id;
    }

    //좋아요 <-> 싫어요 교차
    public void updateLikes(Integer likes) {
        this.good -= likes;
        this.bad -= likes;
    }

    //좋아요나 싫어요 처음 누름
    public void plusLikes(Integer likes) {
        if(likes>0) this.good += likes;
        else this.bad += likes;
    }

    //짝수 번 눌러서 삭제해야함
    public void deleteLikes(Integer likes) {
        if(likes>0) this.good -= likes;
        else this.bad -= likes;
    }

    //동의자 수 업데이트용 편의 메소드
    public void updateAllows(Integer allows) {
        this.allows = allows;
    }

    public void updateStatus(Integer status) { this.status = status; }
    public void updateFinalDateAndDept(LocalDateTime date, String dept) {
        this.finalDate = date;
        this.department = dept;
    }
    public void updateResult(String result) { this.result = result; }

    // 투표 기간 업데이트 편의 메소드
    public void updateVoteDates(LocalDateTime start, LocalDateTime end) {
        this.voteStartDate = start;
        this.voteEndDate = end;
    }
}
