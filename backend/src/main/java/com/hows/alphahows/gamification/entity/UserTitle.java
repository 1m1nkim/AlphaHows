package com.hows.alphahows.gamification.entity;

import com.hows.alphahows.common.BaseTimeEntity;
import com.hows.alphahows.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 유저 획득 칭호 엔티티
 * 유저가 실제로 획득한 칭호 목록입니다. (User - Title의 연결 테이블)
 * '장착 중' 상태를 관리하여 프로필에 표시할 칭호를 결정합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_titles")
public class UserTitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 획득한 유저

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "title_id")
    private Title title; // 획득한 칭호 정보

    @Column(name = "is_equipped")
    private boolean isEquipped; // 현재 장착 중인지 여부

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt; // 획득한 날짜

    @Builder
    public UserTitle(User user, Title title, boolean isEquipped, LocalDateTime acquiredAt) {
        this.user = user;
        this.title = title;
        this.isEquipped = isEquipped;
        this.acquiredAt = acquiredAt;
    }
}
