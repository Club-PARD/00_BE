package com.youngyoung.server.mora.config;

import com.youngyoung.server.mora.dto.SessionUser; // SessionUser 임포트 필수!
import com.youngyoung.server.mora.repo.UserRepo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepo userRepo;

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

        // 🔥 여기가 핵심 수정 포인트!
        if (principal instanceof SessionUser) {
            // (1) SessionUser 타입이면 -> 이미 UserService에서 인증된 '기존 회원'임
            log.info("✅ SessionUser 감지됨 -> 기존 회원으로 처리");
            exists = true;
        } else if (principal instanceof OAuth2User) {
            // (2) 일반 OAuth2User 타입이면 -> '신규 회원'일 가능성 높음 (이메일로 확인)
            OAuth2User oAuth2User = (OAuth2User) principal;
            email = oAuth2User.getAttribute("email");

            // 혹시 모르니 DB 한 번 더 확인 (안전장치)
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

        // 3. 돌아갈 주소 찾기 (이전 코드와 동일)
        String targetOrigin = "http://localhost:3000";
        HttpSession session = request.getSession(false);
        if (session != null) {
            String referer = (String) session.getAttribute("PRE_LOGIN_REFERER");
            if (referer != null) {
                try {
                    java.net.URI uri = new java.net.URI(referer);
                    targetOrigin = uri.getScheme() + "://" + uri.getAuthority();
                    session.removeAttribute("PRE_LOGIN_REFERER");
                } catch (Exception e) { /* 무시 */ }
            }
        }

        // 4. 경로 결정
        String redirectPath;
        if (exists) {
            redirectPath = "/"; // 메인으로
            log.info("🚀 기존 회원 -> 메인 페이지로 이동");
        } else {
            redirectPath = "/signup?email="+email; // 회원가입으로
            log.info("✨ 신규 회원 -> 회원가입 페이지로 이동");
        }

        // 5. 리다이렉트
        String finalUrl = targetOrigin + redirectPath;
        response.sendRedirect(finalUrl);
    }
}