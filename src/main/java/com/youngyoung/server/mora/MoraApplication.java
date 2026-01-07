package com.youngyoung.server.mora;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication
public class MoraApplication {

    //서울시간
   @PostConstruct
    public void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        System.out.println("현재 시간: " + new java.util.Date()); // 로그로 확인
    }

    public static void main(String[] args) {
        SpringApplication.run(MoraApplication.class, args);
    }
}