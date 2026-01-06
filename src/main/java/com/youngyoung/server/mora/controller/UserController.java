package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.dto.SessionUser;
import com.youngyoung.server.mora.dto.UserReq;
import com.youngyoung.server.mora.dto.UserRes;
import com.youngyoung.server.mora.entity.User;
import com.youngyoung.server.mora.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/signUp")
    public ResponseEntity<?> signUp(@RequestBody UserReq.UserInfo userInfo) {
        try {
            log.info("회원가입 요청 데이터 확인: " + userInfo.toString());
            Integer check = userService.save(userInfo);
            if (check == 1) return ResponseEntity.ok("회원가입 함수 문제");
            else return ResponseEntity.ok(200);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    @GetMapping("/check/{id}")
    public ResponseEntity<?> signUp(@PathVariable String id) {
        try {
            Integer check = userService.check(id);
            if (check == 1) return ResponseEntity.status(302).body("중복된 아이디");
            else return ResponseEntity.ok("사용 가능한 닉네임");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    //내정보 조회
    @GetMapping("/me")
    public ResponseEntity<?> mySetting(@AuthenticationPrincipal OAuth2User oAuth2User) {
        try {
            if (oAuth2User instanceof SessionUser) {
                SessionUser sessionUser = (SessionUser) oAuth2User;
                UUID myId = sessionUser.getId();
                UserRes.UserInfo result = userService.getUser(myId);
                return ResponseEntity.ok(result);}
            else{
                return ResponseEntity.status(401).body("로그인 해주세요.");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    //스크랩 조회
    @GetMapping("/scrap")
    public ResponseEntity<?> myScrap(@AuthenticationPrincipal OAuth2User oAuth2User) {
        try {
            if (oAuth2User instanceof SessionUser) {
                SessionUser sessionUser = (SessionUser) oAuth2User;
                UUID myId = sessionUser.getId();
                List<UserRes.ScrapInfo> result = userService.getScrap(myId);
                return ResponseEntity.ok(result);}
            else{
                return ResponseEntity.status(401).body("로그인 해주세요.");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    //user 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal OAuth2User oAuth2User) {
        try {
            if (oAuth2User instanceof SessionUser) {
                SessionUser sessionUser = (SessionUser) oAuth2User;
                UUID myId = sessionUser.getId();
                userService.deleteUser(myId);
                return ResponseEntity.ok(200);}
            else{
                return ResponseEntity.status(401).body("로그인 해주세요.");
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}