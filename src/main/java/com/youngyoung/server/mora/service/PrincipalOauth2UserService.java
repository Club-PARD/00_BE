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

import java.util.Map;
import java.util.Optional;


@Service
@RequiredArgsConstructor
@Slf4j
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    private final UserRepo userRepo;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        // name은 여기서 가져다 쓰지 않습니다. (프론트에서 입력받을 것이므로)

        Optional<User> userOptional = userRepo.findByEmail(email);

        if (userOptional.isPresent()) {
            log.info("기존 회원 OAuth 로그인: {}", email);
            User user = userOptional.get();
            // 기존 회원은 ID와 함께 리턴
            return new SessionUser(user.getId(), user.getEmail(), false, attributes);
        } else {
            log.info("신규 회원 감지 (DB 저장 안함): {}", email);

            // DB에 저장하지 않습니다!
            // ID는 null로, isNew는 true로 설정해서 보냅니다.
            return new SessionUser(null, email, true, attributes);
        }
    }
}