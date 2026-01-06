package com.youngyoung.server.mora.config;

import com.youngyoung.server.mora.service.PrincipalOauth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final PrincipalOauth2UserService principalOauth2UserService;
    private final RefererFilter refererFilter;
    // 🔥 추가됨: 쿠키 기반 인가 요청 저장소
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

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
                        // 🔥 추가됨: 인증 요청을 세션 대신 쿠키에 저장하도록 설정
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
}