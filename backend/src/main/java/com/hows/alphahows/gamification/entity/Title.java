package com.hows.alphahows.gamification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

/**
 * 칭호(업적) 도감 엔티티
 * 시스템에 등록된 모든 칭호의 정의(메타 데이터)입니다.
 * 예: "작심삼일 탈출", "새벽형 인간" 등
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "titles")
public class Title {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 칭호 고유 ID

    @Column(nullable = false)
    private String name; // 칭호 이름

    private String description; // 칭호 설명 (획득 조건 힌트 등)

    @Column(name = "icon_url")
    private String iconUrl; // 칭호 아이콘 이미지 주소

    @Column(name = "condition_type")
    private String conditionType; // 획득 조건 타입 (STREAK, MISSION_COUNT 등)

    @Column(name = "condition_value")
    private int conditionValue; // 획득 조건 목표값 (예: 3일, 10회)

    @Builder
    public Title(String name, String description, String iconUrl, String conditionType, int conditionValue) {
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
    }
}
