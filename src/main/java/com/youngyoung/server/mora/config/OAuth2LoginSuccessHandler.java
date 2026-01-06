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

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final UserRepo userRepo; // DB 조회를 위해 UserRepo 주입

    @Override
    @Transactional(readOnly = true)
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        log.info("OAuth2 로그인 성공. Principal: {}", authentication.getPrincipal());

        // 1. 로그인된 사용자 정보 가져오기
        SessionUser sessionUser = (SessionUser) authentication.getPrincipal();
        Optional<User> userOptional = Optional.ofNullable(userRepo.findById(sessionUser.getId()));

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid User ID:" + sessionUser.getId());
        }
        User user = userOptional.get();

        // 2. 신규/기존 유저 판단 (age가 0이면 신규 유저로 간주)
        boolean isNewUser = (user.getAge() == 0);

        // 3. 리다이렉트 경로 분기
        String redirectPath;
        if (isNewUser) {
            log.info("신규 가입 유저입니다. 추가 정보 입력 페이지로 리다이렉트합니다. User ID: {}", user.getId());
            redirectPath = "/signup?email=" + user.getEmail();
        } else {
            log.info("기존 유저입니다. 메인 페이지로 리다이렉트합니다. User ID: {}", user.getId());
            redirectPath = "/";
        }

        // 4. 돌아갈 주소(Origin) 찾기 (쿠키에서 확인)
        String targetOrigin = authorizationRequestRepository
                .getRedirectOrigin(request)
                .orElse("http://localhost:3000"); // 쿠키 없으면 localhost (개발용)

        log.info("🔙 리다이렉트 타겟 Origin: {}", targetOrigin);

        // 5. 인증 과정에서 사용된 임시 쿠키 삭제
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);

        // 6. 최종 목적지로 리다이렉트
        String finalUrl = targetOrigin + redirectPath;
        log.info("🚀 최종 리다이렉트 URL: {}", finalUrl);
        response.sendRedirect(finalUrl);
    }
}