package com.hows.alphahows.auth.service;

import com.hows.alphahows.user.entity.User;
import com.hows.alphahows.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 서비스로 유저 정보 가져오기
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // 2. 서비스 제공자 정보 가져오기 (kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        Map<String, Object> attributes = oAuth2User.getAttributes();
        log.info("OAuth2 User Info: {}", attributes);

        // 3. 카카오 데이터 파싱
        // Kakao는 "kakao_account" 안에 "email", "profile" 등이 들어있음
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        if (profile == null) {
            log.error("Kakao Profile is NULL! Check scope permission.");
        } else {
            log.info("Kakao Profile: {}", profile);
        }

        String email = (String) kakaoAccount.get("email");
        String nickname = (profile != null) ? (String) profile.get("nickname") : "Unknown";

        if (email == null) {
            // 이메일 권한이 없을 경우 id를 기반으로 임시 이메일 생성 혹은 예외 처리
            Long id = (Long) attributes.get("id");
            email = id + "@kakao.com";
            log.info("Email is null. Generated temp email: {}", email);
        }

        // 4. DB 저장 또는 업데이트
        User user = saveOrUpdate(email, nickname, "KAKAO");

        // 5. OAuth2User 반환 (Role 등을 설정)
        // 여기서는 간단히 attributes를 그대로 리턴
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole())),
                attributes,
                userNameAttributeName);
    }

    private User saveOrUpdate(String email, String nickname, String provider) {
        User user = userRepository.findByEmail(email)
                .map(entity -> {
                    // 이미 가입된 유저라면 닉네임 등을 업데이트 할 수 있음 (여기선 생략)
                    return entity;
                })
                .orElse(User.builder()
                        .email(email)
                        .nickname(nickname)
                        .password(null) // 소셜 로그인은 비번 없음
                        .provider(provider)
                        .role("USER")
                        .build());

        return userRepository.save(user);
    }
}
