package com.youngyoung.server.mora.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://192.168.0.182.nip.io:3000",
                        "http://172.17.213.32.nip.io:3000", // 테스트 중인 IP 추가
                        "http://localhost:3000"      // 로컬 테스트용
                )
                .allowedMethods("GET","POST","PATCH","DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}