package com.hows.alphahows.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDate;

/**
 * 유저 스트릭(연속 출석) 정보 엔티티
 * 자주 변동되는 점수와 출석 정보를 User 테이블과 분리하여 관리합니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_streaks")
public class UserStreak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 스트릭 고유 ID

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 주인 유저

    @Column(name = "current_streak")
    private int currentStreak; // 현재 연속 출석일

    @Column(name = "max_streak")
    private int maxStreak; // 최대 연속 출석일 (기록용)

    private int points; // 보유 포인트 (재화)

    @Column(name = "last_mission_date")
    private LocalDate lastMissionDate; // 마지막으로 미션을 성공한 날짜 (오늘 출석 여부 판단용)

    @Builder
    public UserStreak(User user, int currentStreak, int maxStreak, int points, LocalDate lastMissionDate) {
        this.user = user;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.points = points;
        this.lastMissionDate = lastMissionDate;
    }
}
