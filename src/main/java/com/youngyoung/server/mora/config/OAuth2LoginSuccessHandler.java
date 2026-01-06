package com.youngyoung.server.mora.config;

import com.youngyoung.server.mora.dto.SessionUser;
import com.youngyoung.server.mora.repo.UserRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepo userRepo;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        // 1. 로그인된 사용자 객체 가져오기
        Object principal = authentication.getPrincipal();
        boolean exists = false;
        String email = null;

        if (principal instanceof SessionUser) {
            log.info("✅ SessionUser 감지됨 -> 기존 회원으로 처리");
            exists = true;
        } else if (principal instanceof OAuth2User) {
            OAuth2User oAuth2User = (OAuth2User) principal;
            email = oAuth2User.getAttribute("email");

            if (email != null) {
                exists = userRepo.findByEmail(email).isPresent();
            }
        }

        // 2. 신규 회원일 때만 쿠키 굽기 (기존 회원은 쿠키 필요 없음)
        if (!exists && email != null) {
            Cookie emailCookie = new Cookie("oauth_email", email);
            emailCookie.setPath("/");
            emailCookie.setHttpOnly(false);
            emailCookie.setMaxAge(60 * 5);
            response.addCookie(emailCookie);
        }

        // 🔥 3. 돌아갈 주소 찾기 (쿠키에서 확인)
        String targetOrigin = authorizationRequestRepository
                .getRedirectOrigin(request)
                .orElse("http://localhost:3000"); // 쿠키 없으면 localhost (비상용)

        log.info("🔙 리다이렉트 타겟 Origin: {}", targetOrigin);


        // 4. 경로 결정
        String redirectPath;
        if (exists) {
            redirectPath = "/"; // 메인으로
        } else {
            redirectPath = "/signup?email=" + email; // 회원가입으로
        }

        // 🔥 5. 인증 과정에서 구운 임시 쿠키(state 정보, redirect_origin 등) 삭제
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        // 6. 리다이렉트
        String finalUrl = targetOrigin + redirectPath;
        log.info("🚀 최종 리다이렉트 URL: {}", finalUrl);
        response.sendRedirect(finalUrl);
    }
}