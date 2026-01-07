package com.youngyoung.server.mora;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling // 스케줄러 쓰시니까 이거 있을 겁니다
@SpringBootApplication
public class MoraApplication {

    // ★ [핵심] 서버 켜지자마자 타임존을 '서울'로 고정
    @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.out.println("현재 시간: " + new java.util.Date()); // 로그로 확인
    }

    public static void main(String[] args) {
        SpringApplication.run(MoraApplication.class, args);
    }
}