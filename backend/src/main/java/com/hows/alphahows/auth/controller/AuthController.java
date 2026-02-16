package com.hows.alphahows.auth.controller;

import com.hows.alphahows.auth.dto.LoginRequest;
import com.hows.alphahows.auth.service.AuthService;
import com.hows.alphahows.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            User user = authService.login(request);

            // 로컬 로그인 성공 시 SecurityContext를 세션에 저장해서 로그인 상태를 유지한다.
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getEmail(),
                    null,
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            return ResponseEntity.ok("Login Successful: " + user.getNickname());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.ok(Map.of("authenticated", false));
        }

        Object principal = authentication.getPrincipal();
        String nickname = null;
        String email = null;

        if (principal instanceof OAuth2User oauth2User) {
            // 카카오 OAuth 사용자 정보에서 닉네임/이메일을 추출한다.
            Object kakaoAccountObj = oauth2User.getAttributes().get("kakao_account");
            if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
                Object profileObj = kakaoAccount.get("profile");
                if (profileObj instanceof Map<?, ?> profile) {
                    Object nickObj = profile.get("nickname");
                    if (nickObj != null) {
                        nickname = nickObj.toString();
                    }
                }
                Object emailObj = kakaoAccount.get("email");
                if (emailObj != null) {
                    email = emailObj.toString();
                }
            }

            if (nickname == null) {
                nickname = oauth2User.getName();
            }
        } else if (principal instanceof String username) {
            // 로컬 로그인은 principal이 이메일 문자열이므로 DB에서 닉네임을 조회한다.
            email = username;
            nickname = authService.findByEmail(username)
                    .map(User::getNickname)
                    .orElse(username);
        } else if (principal != null) {
            nickname = principal.toString();
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "nickname", nickname == null ? "User" : nickname,
                "email", email == null ? "" : email
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("authenticated", false));
    }
}
