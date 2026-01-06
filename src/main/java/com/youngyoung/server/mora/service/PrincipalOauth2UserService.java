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
@RequiredArgsConstructor
@Slf4j
public class PrincipalOauth2UserService
        extends DefaultOAuth2UserService {

    private final UserRepo userRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");

        Optional<User> userOptional = userRepo.findByEmail(email);

        if (userOptional.isPresent()) {
            log.info("기존 회원 OAuth 로그인");
            return new SessionUser(userOptional.get().getId());
        }

        log.info("신규 회원 OAuth 로그인");
        return oAuth2User; // SuccessHandler에서 판단
    }
}