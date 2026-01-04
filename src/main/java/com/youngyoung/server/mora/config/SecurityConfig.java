package com.youngyoung.server.mora.config;

import com.youngyoung.server.mora.service.PrincipalOauth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;

// ... imports ...

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final PrincipalOauth2UserService principalOauth2UserService;
    private final RefererFilter refererFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .addFilterBefore(refererFilter, OAuth2AuthorizationRequestRedirectFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/oauth2/**",
                                "/login/**",
                                // ✅ Swagger 허용 (이게 핵심)
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                // 👇 여기를 추가해주세요! (닉네임 체크 API 허용)
                                "/user/**",      // /user로 시작하는 요청을 임시로 다 열거나,
                                "/api/**",        // 혹은 /api/** 전체를 열어두는 것도 방법입니다.
                                "/petition/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(principalOauth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                );

        http.logout(logout -> logout
                .logoutUrl("/auth/google/logout") // 프론트가 호출할 주소
                .logoutSuccessHandler((request, response, authentication) -> {
                    // 프론트에서 보낸 redirect_uri 파라미터를 읽음
                    String targetUrl = request.getParameter("redirect_uri");

                    // 파라미터가 없으면 요청 온 곳(Referer)이나 기본값으로 설정
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
}