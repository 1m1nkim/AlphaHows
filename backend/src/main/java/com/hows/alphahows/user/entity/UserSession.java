package com.hows.alphahows.user.entity;

import com.hows.alphahows.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 유저 로그인 세션 엔티티
 * JWT Refresh Token을 관리하여 로그인 유지 기능을 담당합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_sessions")
public class UserSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 세션 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 세션 주인

    @Column(name = "refresh_token", nullable = false)
    private String refreshToken; // 리프레시 토큰 값

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // 토큰 만료 시간

    @Builder
    public UserSession(User user, String refreshToken, LocalDateTime expiresAt) {
        this.user = user;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }
}
