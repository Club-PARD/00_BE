package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.dto.UserReq;
import com.youngyoung.server.mora.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}