package com.youngyoung.server.mora.config;

import com.youngyoung.server.mora.dto.SessionUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:c2lsdmVybmluZS10ZWNoLXNwcmluZy1ib290LWp3dC10dXRvcmlhbC1zZWNyZXQtc2lsdmVybmluZS10ZWNoLXNwcmluZy1ib290LWp3dC10dXRvcmlhbC1zZWNyZXQK}")
    private String secretKey;

    @Value("${jwt.expiration:3600000}")
    private long tokenValidityInMilliseconds;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 1. 토큰 생성
    public String createAccessToken(Authentication authentication) {
        // Principal에서 ID 등 식별자 가져오기
        String username = authentication.getName(); // 보통 SessionUser의 ID(UUID string)가 들어옴

        long now = (new Date()).getTime();
        Date validity = new Date(now + this.tokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(username) // 토큰 제목(Subject)에 UUID 저장
                .claim("auth", "ROLE_USER") // 권한 정보 저장 (필요 시 수정)
                .setIssuedAt(new Date())
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // 2. 토큰에서 인증 정보(Authentication) 꺼내기 (⭐️중요: SessionUser로 반환)
    public Authentication getAuthentication(String token) {
        // 토큰 복호화
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        // 권한 정보 가져오기
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get("auth").toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // Subject(UUID 문자열)를 UUID 객체로 변환
        UUID userId = UUID.fromString(claims.getSubject());

        // ⭐️ 핵심: UserController가 좋아하는 SessionUser 객체로 만들기
        // (JWT에는 이메일/속성 정보가 없으므로 빈 값으로 채움. API 호출 시 ID로 DB 조회하므로 괜찮음)
        SessionUser principal = new SessionUser(userId, "", false, null);

        // SecurityContext에 저장할 Authentication 객체 생성
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // 3. 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}