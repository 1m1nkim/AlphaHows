package com.hows.alphahows.auth.util;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

public final class AuthPrincipalUtils {

    private AuthPrincipalUtils() {
    }

    public static String resolveEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return resolveEmail(authentication.getPrincipal());
    }

    public static String resolveEmail(Principal principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof Authentication authentication) {
            return resolveEmail(authentication);
        }
        return principal.getName();
    }

    @SuppressWarnings("unchecked")
    public static String resolveEmail(Object principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof String username) {
            return username;
        }
        if (principal instanceof OAuth2User oauth2User) {
            Object kakaoAccountObj = oauth2User.getAttributes().get("kakao_account");
            if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
                Object emailObj = kakaoAccount.get("email");
                if (emailObj != null) {
                    return emailObj.toString();
                }
            }

            Object idObj = oauth2User.getAttributes().get("id");
            if (idObj != null) {
                return idObj + "@kakao.com";
            }

            return Optional.ofNullable(oauth2User.getName())
                    .map(name -> name + "@kakao.com")
                    .orElse(null);
        }
        return null;
    }
}
