package com.hows.alphahows.gamification.entity;

import com.hows.alphahows.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일일 할당 미션 엔티티
 * 특정 날짜에 특정 유저에게 할당된 미션입니다. (User - Mission 연결)
 * 성공 여부와 수행 시간을 기록하여 스트릭 판별에 사용됩니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "daily_missions")
public class DailyMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 수행해야 할 유저

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id")
    private Mission mission; // 할당된 미션 원본

    @Column(name = "mission_date", nullable = false)
    private LocalDate missionDate; // 미션이 할당된 날짜

    @Column(name = "is_completed")
    private boolean isCompleted; // 미션 성공 여부

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 미션 성공 시간

    @Builder
    public DailyMission(User user, Mission mission, LocalDate missionDate, boolean isCompleted,
            LocalDateTime completedAt) {
        this.user = user;
        this.mission = mission;
        this.missionDate = missionDate;
        this.isCompleted = isCompleted;
        this.completedAt = completedAt;
    }
}
