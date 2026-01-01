package com.youngyoung.server.mora.controller;

import com.youngyoung.server.mora.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class ClientController {
    private final UserService userService;

    @PostMapping("/signUp")
    public ResponseEntity<?> signUp(@RequestBody UserReq.UserInfo userInfo) {
        try {
            log.info("회원가입 요청 데이터 확인: " + clientInfo.toString());
            Integer check = userService.save(clientInfo);
            if (check == 1) return ResponseEntity.ok("회원가입 함수 문제");
            else return ResponseEntity.ok(200);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}