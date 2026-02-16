package com.hows.alphahows.config;

import com.hows.alphahows.auth.util.AuthPrincipalUtils;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Component
public class WebSocketPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        Principal principal = request.getPrincipal();
        String email = null;

        if (principal instanceof Authentication authentication) {
            email = AuthPrincipalUtils.resolveEmail(authentication);
        } else {
            email = AuthPrincipalUtils.resolveEmail(principal);
        }

        // 일부 환경에서 request.getPrincipal()이 비어 있으므로 세션 SecurityContext를 2차로 조회
        if ((email == null || email.isBlank()) && request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            if (session != null) {
                Object contextObj = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
                if (contextObj instanceof SecurityContext context) {
                    email = AuthPrincipalUtils.resolveEmail(context.getAuthentication());
                }
            }
        }

        String name = (email == null || email.isBlank())
                ? "anonymous-" + UUID.randomUUID()
                : email;

        return () -> name;
    }
}
