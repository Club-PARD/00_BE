package com.youngyoung.server.mora.dto;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Getter
public class SessionUser implements OAuth2User, Serializable {

    private final UUID id;
    private final boolean isNew;

    public SessionUser(UUID id, boolean isNew) {
        this.id = id;
        this.isNew = isNew;
    }

    // --- Spring Security가 "구글 정보 내놔" 하면 "없어"라고 대답하는 부분 ---

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.emptyMap(); // "구글 정보? 그런 거 안 키워." (빈 맵 반환)
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList(); // "권한? 몰라." (빈 리스트 반환)
    }

    @Override
    public String getName() {
        return String.valueOf(id); // "이름이 뭐야?" -> "내 ID 번호다."
    }

    public UUID getId() {
        return id;
    }

    public boolean isNew() {
        return isNew;
    }
}