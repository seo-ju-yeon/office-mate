package office_mate_2605.common.employee.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 직원 재직 상태 변경 신청 정보를 관리하는 Entity. (작성자: 서주연)
 *
 * <p>직원이 요청한 휴직, 퇴사, 복직 신청의 신청자, 신청 유형, 사유,
 * 처리 상태, 결재자, 처리 시각, 처리 코멘트를 보관한다.
 * 승인/반려는 한 번만 처리되도록 Entity 내부에서 상태 전환 규칙을 검증한다.</p>
 */
@Entity
@Table(name = "employee_status_request")
@Getter
@NoArgsConstructor
public class EmployeeStatusRequest {
    // 직원의 휴직/퇴사 신청 Entity
    // 신청 데이터와 승인/반려 상태 전환 규칙을 가짐

    // 신청 PK, employee_status_request 테이블의 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 신청자 정보, 휴직/퇴사/복직을 요청한 직원
    // 지연로딩으로 필요할 때만 직원 상세 정보를 가져옴
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no", nullable = false)
    private Employee employee;

    // 신청 유형, 휴직/퇴사/복직 중 어떤 상태 변경 요청인지 구분
    // PostgreSQL employment_request_type enum과 매핑
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "request_type", nullable = false)
    private EmploymentRequestType requestType;

    // 신청 사유, 신청자가 작성한 휴직/퇴사/복직 요청 설명
    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    // 신청 시각, 신청 Entity 생성 시 서버 시각으로 자동 기록
    // 생성 이후 수정되지 않도록 updatable=false 설정
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    // 신청 처리 상태, 최초 생성 시 PENDING으로 시작
    // 승인/반려 후 APPROVED 또는 REJECTED로 변경
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private RequestStatus status;

    // 결재자 정보, 신청을 승인 또는 반려한 SUPER 직원
    // 아직 처리 전이면 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private Employee decidedBy;

    // 승인/반려 처리 시각
    // 아직 PENDING 상태이면 null
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    // 승인/반려 처리 코멘트, SUPER가 남긴 처리 사유 또는 안내 문구
    @Column(name = "decision_comment", columnDefinition = "text")
    private String decisionComment;

    // EmployeeStatusRequest.builder() 호출 시 사용하는 생성자
    // 신청은 항상 PENDING 상태로 시작하고, 신청 시각은 서버 시각으로 기록
    @Builder
    private EmployeeStatusRequest(Employee employee,
                                  EmploymentRequestType requestType,
                                  String reason) {
        this.employee = employee;
        this.requestType = requestType;
        this.reason = reason;
        this.requestedAt = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
    }

    // SUPER가 신청을 승인 처리
    public void approve(Employee decidedBy, String decisionComment) {
        decide(RequestStatus.APPROVED, decidedBy, decisionComment);
    }

    // SUPER가 신청을 반려 처리
    public void reject(Employee decidedBy, String decisionComment) {
        decide(RequestStatus.REJECTED, decidedBy, decisionComment);
    }

    // 화면이나 Service에서 승인 대기 상태 여부를 읽기 쉽게 확인하기 위한 편의 메서드
    public boolean isPending() {
        return this.status != null && this.status.isPending();
    }

    // 승인/반려 공통 처리 로직
    // 이미 처리된 신청을 다시 승인/반려하면 이력 데이터가 꼬이므로 Entity 단계에서 한 번 더 차단
    private void decide(RequestStatus nextStatus, Employee decidedBy, String decisionComment) {
        if (!isPending()) {
            throw new IllegalStateException("이미 처리된 신청입니다.");
        }
        if (decidedBy == null) {
            throw new IllegalArgumentException("신청 처리자는 필수입니다.");
        }

        this.status = nextStatus;
        this.decidedBy = decidedBy;
        this.decidedAt = LocalDateTime.now();
        this.decisionComment = decisionComment;
    }
}
