package com.hows.alphahows.gamification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

/**
 * 미션 원본(문제은행) 엔티티
 * 시스템이 사용자에게 줄 수 있는 모든 미션의 목록입니다.
 * 예: "이불 개기", "물 마시기"
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "missions")
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 미션 고유 ID

    @Column(nullable = false)
    private String content; // 미션 내용 (예: 약 먹었나요?)

    private String category; // 미션 카테고리 (HEALTH, LIFE, MIND)

    @Column(name = "reward_xp")
    private int rewardXp; // 성공 시 지급할 경험치

    @Builder
    public Mission(String content, String category, int rewardXp) {
        this.content = content;
        this.category = category;
        this.rewardXp = rewardXp;
    }
}
