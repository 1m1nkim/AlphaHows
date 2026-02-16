package com.hows.alphahows.user.entity;

import com.hows.alphahows.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 사용자 정보 엔티티
 * 서비스의 핵심 유저 정보를 관리합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 사용자 고유 ID

    @Column(nullable = false, unique = true)
    private String email; // 로그인 아이디 (이메일)

    @Column(nullable = false)
    private String nickname; // 앱 내 표시되는 닉네임

    @Column(nullable = false)
    private String provider; // 소셜 로그인 제공자 (GOOGLE, KAKAO, LOCAL)

    @Column(nullable = false)
    private String role; // 권한 (USER, ADMIN)

    @Column(nullable = true)
    private String password; // 로컬 로그인을 위한 비밀번호 (소셜 로그인은 null)

}
