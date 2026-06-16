package office_mate_2605.dashboard.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 직원별 알림 Entity (작성자: 강수현)

 * <p> 프로젝트 참여, 할당한 업무에 대한 알람 정보를 관리합니다.
 * is_read 칼럼의 값을 기반으로 알람 전송 여부를 관리하는 데이터 모델입니다. </p>
 */

@Entity
@Table(name = "notification")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id; // 알림 PK

    @Column(name = "employee_no", length = 30, nullable = false)
    private String employeeNo; // 알림 수신 직원 사번

    @Column(name = "title", length = 200, nullable = false)
    private String title; // 알림 제목

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // 알림 상세 메시지

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", columnDefinition = "notification_ref_type")
    private NotificationRefType refType; // 알림 참조 대상 유형

    @Column(name = "ref_id", length = 80)
    private String refId; // 참조 대상 ID

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false; // 알림 읽음 여부

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt; // 알림 생성 시각
}
