package com.hows.alphahows.auth.controller;

import com.hows.alphahows.auth.dto.LoginRequest;
import com.hows.alphahows.auth.service.AuthService;
import com.hows.alphahows.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Map;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            User user = authService.login(request);

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
        String role = "USER";

        if (principal instanceof OAuth2User oauth2User) {
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

            if (email != null) {
                role = authService.findByEmail(email)
                        .map(User::getRole)
                        .orElse("USER");
            }
        } else if (principal instanceof String username) {
            email = username;
            User user = authService.findByEmail(username).orElse(null);
            if (user != null) {
                nickname = user.getNickname();
                role = user.getRole();
            } else {
                nickname = username;
            }
        } else if (principal != null) {
            nickname = principal.toString();
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "nickname", nickname == null ? "User" : nickname,
                "email", email == null ? "" : email,
                "role", role
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
