package com.youngyoung.server.mora.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.Optional;

@Slf4j
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";
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

        // 1. OAuth2 요청 저장
        String serializedAuthRequest = serialize(authorizationRequest);
        addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serializedAuthRequest, cookieExpireSeconds);

        // 2. [중요] 프론트에서 보낸 'origin' 파라미터 캡쳐
        String redirectOrigin = request.getParameter("origin");

        // 파라미터가 없으면 Referer라도 확인
        if (!StringUtils.hasText(redirectOrigin)) {
            String referer = request.getHeader("Referer");
            if (StringUtils.hasText(referer)) {
                try {
                    // Referer에서 origin만 추출 (예: http://172.17...:3000)
                    java.net.URI uri = new java.net.URI(referer);
                    redirectOrigin = uri.getScheme() + "://" + uri.getAuthority();
                } catch (Exception e) {
                    // 무시
                }
            }
        }

        // 3. Origin 쿠키 저장 (환경에 따라 Secure 자동 조절)
        if (StringUtils.hasText(redirectOrigin)) {
            log.info("OAuth2 요청 Origin 저장: {}", redirectOrigin);
            // http로 시작하면 Secure 끄고, https면 Secure 켬
            boolean isHttps = redirectOrigin.toLowerCase().startsWith("https");
            addCookie(response, REDIRECT_ORIGIN_PARAM_COOKIE_NAME, redirectOrigin, cookieExpireSeconds, isHttps);
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

    // --- Helper Methods (핵심 변경) ---

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // 기본값: 일단 false로 시작 (로컬 테스트 호환성 위함)
        addCookie(response, name, value, maxAge, false);
    }

    private void addCookie(HttpServletResponse response, String name, String value, int maxAge, boolean isSecure) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .maxAge(maxAge);

        if (isSecure) {
            // 배포 환경 (HTTPS)
            builder.secure(true).sameSite("None");
        } else {
            // 로컬/IP 환경 (HTTP) -> Secure를 꺼야 쿠키가 저장됨!
            builder.secure(false).sameSite("Lax");
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    ResponseCookie deleteCookie = ResponseCookie.from(name, "")
                            .path("/")
                            .httpOnly(true)
                            .maxAge(0)
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
                }
            }
        }
    }

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

    private String serialize(Object object) {
        return Base64.getUrlEncoder().encodeToString(SerializationUtils.serialize(object));
    }

    private <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}