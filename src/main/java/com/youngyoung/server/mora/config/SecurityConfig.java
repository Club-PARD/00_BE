package com.youngyoung.server.mora.config;

import com.youngyoung.server.mora.service.PrincipalOauth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final PrincipalOauth2UserService principalOauth2UserService;
    private final RefererFilter refererFilter;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ★ 1. 여기에 cors 설정을 반드시 명시해야 합니다.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .addFilterBefore(refererFilter, OAuth2AuthorizationRequestRedirectFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/oauth2/**",
                                "/login/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/user/signUp",
                                "/user/check/**",
                                "/api/**",
                                "/petition/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository)
                        )
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(principalOauth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                );

        http.logout(logout -> logout
                .logoutUrl("/auth/google/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    String targetUrl = request.getParameter("redirect_uri");
                    if (targetUrl == null || targetUrl.isEmpty()) {
                        targetUrl = request.getHeader("Referer");
                    }
                    if (targetUrl == null || targetUrl.isEmpty()) {
                        targetUrl = "http://192.168.0.182.nip.io:3000";
                    }
                    response.sendRedirect(targetUrl);
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
        );
        return http.build();
    }

    // ★ 2. Security 전용 CORS 설정 빈 등록
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 주소를 정확히 입력 (맨 마지막 슬래시 / 는 빼세요!)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://54.180.32.70.nip.io:3000",
                "http://192.168.0.182.nip.io:3000"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true); // 쿠키를 받기 위해 필수

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}