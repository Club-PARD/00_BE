package com.youngyoung.server.mora.service;

import com.youngyoung.server.mora.dto.SessionUser;
import com.youngyoung.server.mora.entity.User;
import com.youngyoung.server.mora.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    private final UserRepo userRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        // 1. 일단 구글이랑 통신해서 이메일은 알아냅니다.
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        String email = oAuth2User.getAttribute("email");

        // 2. DB 조회
        Optional<User> userOptional = userRepo.findByEmail(email);

        if (userOptional.isPresent()) {
            // ★ [핵심] 가입된 회원이면 구글 정보(oAuth2user)는 갖다 버립니다.
            // 우리 DB ID만 담은 초경량 객체를 세션에 넣습니다.
            UUID dbId = userOptional.get().getId();
            log.info("기존 회원 로그인. ID만 세션에 저장: " + dbId);

            return new SessionUser(dbId);
        } else {
            // 신규 회원은 ID가 없으니까, 회원가입 전까지만 임시로 구글 정보를 씁니다.
            log.info("신규 회원. 이메일 전달용 임시 객체 사용.");
            return oAuth2User;
        }
    }
}