package com.hows.alphahows.user.entity;

import com.hows.alphahows.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 약 복용 기록 엔티티
 * 유저가 약을 복용한 시간, 체감 효과, 지속 시간 등을 상세하게 기록합니다.
 * 단순 미션 성공 여부와는 별개로, 치료 데이터를 쌓는 용도입니다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "medication_logs")
public class MedicationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기록 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 작성자

    @Column(name = "taken_at", nullable = false)
    private LocalDateTime takenAt; // 약을 실제로 복용한 시간

    private int effectiveness; // 체감 효과 점수 (1~5점)

    @Column(name = "duration_minutes")
    private int durationMinutes; // 약효가 지속된 시간 (분 단위)

    private String memo; // 부작용이나 특이사항 메모

    @Builder
    public MedicationLog(User user, LocalDateTime takenAt, int effectiveness, int durationMinutes, String memo) {
        this.user = user;
        this.takenAt = takenAt;
        this.effectiveness = effectiveness;
        this.durationMinutes = durationMinutes;
        this.memo = memo;
    }
}
