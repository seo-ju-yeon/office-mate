package office_mate_2605.management.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import office_mate_2605.common.employee.domain.Employee;

import java.time.LocalDateTime;

/**
 * 직원 계정의 로그인 실패와 잠금 상태를 관리하는 Entity. (작성자: 서주연)
 *
 * <p>employee 테이블과 분리해 연속 로그인 실패 횟수, 계정 잠금 여부,
 * 마지막 실패 시각, 잠금/해제 시각, 해제 관리자를 기록한다.
 * 로그인 보안 정책과 관리자 잠금 해제 이력을 담당한다.</p>
 */
@Entity
@Table(name = "account_security_status")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountSecurityStatus {
    // 직원 계정 보안 상태 엔티티
    // 로그인 실패 횟수와 계정 잠금 여부는 인사 기본 정보가 아니므로 employee 테이블과 분리해서 관리
    // 계정 잠금은 로그인 보안 정책에 해당하며 관리자 해제 이력도 함께 기록

    // 보안 상태를 관리할 직원 사번
    // employee_no를 PK이자 employee 테이블 FK로 사용
    @Id
    @Column(name = "employee_no", length = 30)
    private String employeeNo;

    // 보안 상태를 관리할 직원 Entity
    // 지연 로딩으로 직원 상세 정보가 필요할 때만 조회
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId  // 외래키를 기본키로 지정하여 부모 엔티티의 식별자를 공유
    @JoinColumn(name = "employee_no", nullable = false)
    private Employee employee;

    // 연속 로그인 실패 횟수
    // 정상 로그인 또는 관리자 잠금 해제 시 0으로 초기화
    @Column(name = "login_fail_count", nullable = false)
    private int loginFailCount = 0;

    // 계정 잠금 여부
    // true이면 관리자가 잠금을 해제하기 전까지 로그인 불가
    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    // 마지막 로그인 실패 시각
    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    // 로그인 실패 횟수가 기준을 초과해 계정이 잠긴 시각
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    // 관리자가 계정 잠금을 해제한 시각
    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

    // 계정 잠금을 해제한 관리자
    // 어떤 관리자가 잠금을 풀었는지 추적하기 위해 기록
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unlocked_by")
    private Employee unlockedBy;

    // 보안 상태 생성 시각
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 보안 상태 마지막 변경 시각
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 직원 계정 보안 상태 최초 생성용 빌더
    // 최초 생성 시 실패 횟수 0회, 잠금 해제 상태로 시작
    @Builder
    private AccountSecurityStatus(Employee employee) {
        this.employee = employee;
        this.employeeNo = employee.getEmployeeNo();
        this.loginFailCount = 0;
        this.accountLocked = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 로그인 실패를 기록하는 메서드
    // 실패 횟수가 잠금 기준 이상이면 계정을 잠금 상태로 전환
    public void recordLoginFailure(int lockThreshold) {
        this.loginFailCount++;
        this.lastFailedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.loginFailCount >= lockThreshold) {
            lock();
        }
    }

    // 정상 로그인 성공 시 연속 실패 횟수를 초기화하는 메서드
    // 잠금 상태는 관리자만 해제해야 하므로 accountLocked는 변경하지 않음
    public void resetLoginFailures() {
        this.loginFailCount = 0;
        this.lastFailedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    // 관리자가 계정 잠금을 해제하는 메서드
    // 다시 정상 로그인할 수 있도록 실패 횟수와 잠금 관련 시각도 함께 초기화
    public void unlock(Employee unlockedBy) {
        this.accountLocked = false;
        this.loginFailCount = 0;
        this.lockedAt = null;
        this.lastFailedAt = null;
        this.unlockedAt = LocalDateTime.now();
        this.unlockedBy = unlockedBy;
        this.updatedAt = LocalDateTime.now();
    }

    // 계정을 잠금 상태로 전환하는 메서드
    // 최초 잠금 시각만 기록하고 기존 lockedAt이 있으면 유지
    private void lock() {
        this.accountLocked = true;
        if (this.lockedAt == null) {
            this.lockedAt = LocalDateTime.now();
        }
    }
}
