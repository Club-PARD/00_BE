package com.youngyoung.server.mora.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Optional;

@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";

    // 프론트엔드와 맞춘 쿠키 이름
    public static final String REDIRECT_ORIGIN_PARAM_COOKIE_NAME = "redirect_origin";

    private static final int cookieExpireSeconds = 180;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                .map(cookie -> deserialize(cookie, OAuth2AuthorizationRequest.class))
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequestCookies(request, response);
            return;
        }

        // 1. OAuth2 요청 정보를 쿠키에 저장
        Cookie cookie = new Cookie(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialize(authorizationRequest));
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(cookieExpireSeconds);
        response.addCookie(cookie);

        // 2. 리다이렉트할 Origin 저장 로직 수정 (파라미터 우선 -> 없으면 Referer)
        String redirectOrigin = request.getParameter("origin");

        // 파라미터가 없으면 Referer 헤더에서 추출 시도
        if (!StringUtils.hasText(redirectOrigin)) {
            String referer = request.getHeader("Referer");
            if (StringUtils.hasText(referer)) {
                try {
                    java.net.URI uri = new java.net.URI(referer);
                    redirectOrigin = uri.getScheme() + "://" + uri.getAuthority();
                } catch (Exception e) {
                    // Referer 파싱 실패 시 무시
                }
            }
        }

        // 3. 찾은 Origin이 있다면 쿠키에 저장
        if (StringUtils.hasText(redirectOrigin)) {
            Cookie redirectCookie = new Cookie(REDIRECT_ORIGIN_PARAM_COOKIE_NAME, redirectOrigin);
            redirectCookie.setPath("/");
            redirectCookie.setHttpOnly(true);
            redirectCookie.setMaxAge(cookieExpireSeconds);
            // HTTPS 환경이라면 아래 설정을 true로 하는 것이 좋습니다.
            redirectCookie.setSecure(true);
            response.addCookie(redirectCookie);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        deleteCookie(request, response, REDIRECT_ORIGIN_PARAM_COOKIE_NAME);
    }

    public Optional<String> getRedirectOrigin(HttpServletRequest request) {
        return getCookie(request, REDIRECT_ORIGIN_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);
    }

    // --- Helper Methods ---

    private Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }

    private String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    private <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}