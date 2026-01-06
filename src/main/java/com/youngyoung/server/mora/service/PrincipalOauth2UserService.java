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
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrincipalOauth2UserService
        extends DefaultOAuth2UserService {

    private final UserRepo userRepo;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        Optional<User> userOptional = userRepo.findByEmail(email);

        if (userOptional.isPresent()) {
            log.info("기존 회원 OAuth 로그인: {}", email);
            User user = userOptional.get();
            // 필요 시 이름 등 정보 업데이트
            // user.updateName(name);
            return new SessionUser(user.getId(), false);
        } else {
            log.info("신규 회원 OAuth 로그인: {}", email);

            User newUser = User.builder()
                    .name(name)
                    .email(email)
                    .age(0) // 기본값 설정
                    .status(1) // 기본값 설정 (1: 활성)
                    .build();

            User savedUser = userRepo.save(newUser);
            log.info("신규 회원 저장 완료: {}", savedUser.getId());

            return new SessionUser(savedUser.getId(), true);
        }
    }
}