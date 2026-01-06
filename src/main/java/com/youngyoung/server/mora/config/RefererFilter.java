
package com.youngyoung.server.mora.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RefererFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 구글 로그인 시도를 감지
        if (request.getRequestURI().startsWith("/oauth2/authorization")) {
            // "어디서 왔니?" (Referer 헤더 읽기)
            String referer = request.getHeader("Referer");
            if (referer != null) {
                // 세션에 저장해두기 (나중에 SuccessHandler에서 꺼내 쓰려고)
                HttpSession session = request.getSession();
                session.setAttribute("PRE_LOGIN_REFERER", referer);
            }
        }
        filterChain.doFilter(request, response);
    }
}
