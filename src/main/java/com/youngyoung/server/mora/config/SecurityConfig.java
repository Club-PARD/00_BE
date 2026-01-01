package com.youngyoung.server.mora.config;

import com.youngyoung.server.mora.service.PrincipalOauth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final PrincipalOauth2UserService principalOauth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 1. CSRF 해제
        http.csrf(AbstractHttpConfigurer::disable);

        // 2. CORS 설정 적용
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // 3. 권한 설정 (모든 요청 허용)
        http.authorizeHttpRequests(au -> au
                .anyRequest().permitAll()
        );

        // 4. OAuth2 로그인 설정
        http.oauth2Login(oauth -> oauth
                // ★ 핵심 수정: defaultSuccessUrl(...) 라인을 삭제했습니다.
                // 성공 핸들러(oAuth2LoginSuccessHandler)에게 모든 리다이렉트 권한을 넘깁니다.
                .successHandler(oAuth2LoginSuccessHandler)

                // 사용자 정보 가져오는 서비스 설정
                .userInfoEndpoint(userInfo -> userInfo.userService(principalOauth2UserService))
        );

        //로그아웃
        http.logout(logout -> logout
                .logoutUrl("/auth/google/logout")
                .logoutSuccessUrl("http://localhost:3000")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
        );

        return http.build();
    }

    // CORS 설정 Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}