package com.youngyoung.server.mora.dto;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Getter
public class SessionUser implements OAuth2User, Serializable {

    private final UUID id;
    private final String email;       // [추가] 핸들러나 토큰 생성기에서 이메일이 필요함
    private final boolean isNew;
    private final Map<String, Object> attributes; // [추가] 구글이 준 원본 정보 저장

    public SessionUser(UUID id, String email, boolean isNew, Map<String, Object> attributes) {
        this.id = id;
        this.email = email;
        this.isNew = isNew;
        this.attributes = attributes;
    }

    // --- OAuth2User 인터페이스 구현 ---

    @Override
    public Map<String, Object> getAttributes() {
        return attributes; // 이제 "구글 정보 여기 있어요" 라고 정직하게 반환
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 일단 기본 권한 설정 (필요 시 DB에서 role 가져와서 ROLE_USER 등으로 변경 가능)
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getName() {
        return String.valueOf(id); // Principal의 이름은 ID로 유지
    }
}