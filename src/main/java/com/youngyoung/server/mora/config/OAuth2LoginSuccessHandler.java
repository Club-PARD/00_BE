
package com.youngyoung.server.mora.config;

import com.youngyoung.server.mora.dto.SessionUser;
import com.youngyoung.server.mora.entity.User;
import com.youngyoung.server.mora.repo.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;

// (임포트 생략: JwtTokenProvider 등 필요)

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final UserRepo userRepo;
    private final JwtTokenProvider jwtTokenProvider; // 👈 [추가] JWT 생성기 주입 필요

    // ... (이전 코드와 동일)

    @Override
    @Transactional(readOnly = true)
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        SessionUser sessionUser = (SessionUser) authentication.getPrincipal();
        boolean isNewUser = sessionUser.isNew();

        String targetOrigin = authorizationRequestRepository
                .getRedirectOrigin(request)
                .orElse("https://00-fe.vercel.app/"); // 프론트 주소

        // 1. 신규 유저인 경우 (DB에 없음)
        if (isNewUser) {
            log.info("신규 회원입니다. 회원가입 페이지로 이동합니다. Email: {}", sessionUser.getEmail());
            // 쿼리 파라미터로 이메일만 넘겨줍니다. 토큰은 아직 없습니다.
            String redirectUrl = targetOrigin + "/signup?email=" + sessionUser.getEmail();

            authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
            response.sendRedirect(redirectUrl);
            return;
        }

        // 2. 기존 유저인 경우 (DB에 있음)
        // 여기서 JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(authentication);

        log.info("기존 유저 로그인 성공. 토큰 발급 완료.");
        String finalUrl = targetOrigin + "/?token=" + accessToken;

        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        response.sendRedirect(finalUrl);
    }
}