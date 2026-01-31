package com.hows.alphahows.auth.controller;

import com.hows.alphahows.auth.dto.LoginRequest;
import com.hows.alphahows.auth.service.AuthService;
import com.hows.alphahows.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = authService.login(request);
            // 간소화를 위해 성공 메시지와 유저 정보 반환 (실무에서는 JWT 또는 세션 쿠키 설정)
            return ResponseEntity.ok().body("Login Successful: " + user.getNickname());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
