package office_mate_2605.common.employee.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 직원 계정과 기본 인사 정보를 관리하는 Entity. (작성자: 서주연)
 *
 * <p>사번, 비밀번호, 이름, 이메일, 부서, 직급, 시스템 권한, 재직 상태를 보관한다.
 * 직원 생성 후 임시 비밀번호 변경 여부, 휴직/복직/퇴사 처리, 논리 삭제 같은
 * 직원 생명주기 변경 규칙도 함께 담당한다.</p>
 */
@Entity
@Table(name = "employee")
@Getter
@NoArgsConstructor
public class Employee {
    // 식별자
    // 사번, 로그인 ID이며 변경이 불가능
    @Id
    @Column(name = "employee_no", length = 30)
    private String employeeNo;

    // BCrypt 해시 비밀번호 (인증 정보)
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    // 임시 비밀번호 변경 필요 여부
    // true이면 로그인 후 비밀번호 변경 페이지로 강제 이동
    @Column(name = "temp_password_required", nullable = false)
    private boolean tempPasswordRequired = true;

    // 직원 이름
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // 직원 이메일
    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    // 소속 부서
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)  // DB에 문자열 상수로 저장하여 가독성 확보
    @Column(name = "department", nullable = false)
    private DepartmentCode department;

    // 직급
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "position", nullable = false)
    private PositionCode position;

    // 시스템 전역 역할
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "role", nullable = false)
    private SystemRole role = SystemRole.USER;  // 시스템 접근 권한 (USER/ADMIN/SUPER)

    // 재직 상태 (기본값 ACTIVE)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private EmploymentStatus status = EmploymentStatus.ACTIVE;  // 재직/휴직/퇴직

    // 입사일
    @Column(name = "joined_on", nullable = false)
    private LocalDate joinedOn;

    // 퇴사일 (재직자는 null)
    @Column(name = "resigned_on")
    private LocalDate resignedOn;  // 퇴사 처리 시점 기록

    // 논리 삭제 시각 (실제 DELETE 대신 이 값이 채워지면 삭제된 데이터로 취급)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // 직원 최초 생성용 빌더 (생성 신규 직원 생성 영역)
    // password는 반드시 BCrypt 해시값을 전달
    @Builder
    private Employee(
            String employeeNo,
            String password,
            String name,
            String email,
            DepartmentCode department,
            PositionCode position,
            SystemRole role,
            LocalDate joinedOn
    ) {
        this.employeeNo = employeeNo;
        this.password = password;
        this.name = name;
        this.email = email;
        this.department = department;
        this.position = position;
        this.role = (role != null) ? role : SystemRole.USER;
        this.joinedOn = joinedOn;
        this.tempPasswordRequired = true; // 최초 생성 시 항상 임시 비밀번호 상태
        this.status = EmploymentStatus.ACTIVE; // 최초 생성 시 항상 재직 상태
    }

    // 임시 비밀번호를 새 비밀번호로 변경
    // 변경 후 temp_password_required 를 false 로 전환
    public void changePassword(String encodedNewPassword) {
        this.password = encodedNewPassword;
        this.tempPasswordRequired = false;
    }

    // 관리자가 직원 비밀번호를 임시 비밀번호로 초기화
    // temp_password_required 를 true 로 전환해 다음 로그인 시 변경을 강제
    public void resetToTempPassword(String encodedTempPassword) {
        this.password = encodedTempPassword;
        this.tempPasswordRequired = true;
    }

    // 기본 인사 정보를 수정
    // null 값이 전달된 필드는 기존 값을 유지함
    public void updateInfo(
            String name,  // 변경할 이름 (null 이면 유지)
            String email,  // 변경할 이메일 (null 이면 유지)
            DepartmentCode department,  // 변경할 부서 (null 이면 유지)
            PositionCode position  // 변경할 직급 (null 이면 유지)
    ) {
        if (name != null) this.name = name;
        if (email != null) this.email = email;
        if (department != null) this.department = department;
        if (position != null) this.position = position;
    }

    // 시스템 역할을 변경
    // ADMIN 이상 권한을 가진 직원만 호출해야 함 (Service 레이어에서 검증)
    public void changeRole(SystemRole newRole) {
        this.role = newRole;  // 변경할 시스템 역할
    }

    // 직원을 휴직 처리
    public void takeLeave() {
        if (this.status.isResigned()) {
            throw new IllegalStateException("퇴사한 직원은 휴직 처리할 수 없습니다. employeeNo=" + employeeNo);
        }
        this.status = EmploymentStatus.ON_LEAVE;
    }

    // 휴직 중인 직원을 복직 처리
    public void returnFromLeave() {
        if (this.status != EmploymentStatus.ON_LEAVE) {
            throw new IllegalStateException("휴직 상태가 아닌 직원은 복직 처리할 수 없습니다. employeeNo=" + employeeNo);
        }
        this.status = EmploymentStatus.ACTIVE;
    }

    // 직원을 퇴사 처리
    // resignedOn(퇴사일)을 기록한다.
    public void resign(LocalDate resignedOn) {
        if (this.status.isResigned()) {
            throw new IllegalStateException("이미 퇴사 처리된 직원입니다. employeeNo=" + employeeNo);
        }
        this.status = EmploymentStatus.RESIGNED;
        this.resignedOn = resignedOn;
    }

    // 관리자가 직원 재직 상태를 직접 변경
    // ACTIVE/ON_LEAVE로 되돌릴 때는 퇴사일을 비워 현재 상태와 모순되지 않게 유지
    public void changeEmploymentStatus(EmploymentStatus newStatus, LocalDate resignedOn) {
        if (newStatus == null) {
            return;
        }

        this.status = newStatus;
        if (newStatus == EmploymentStatus.RESIGNED) {
            this.resignedOn = resignedOn;
            return;
        }

        this.resignedOn = null;
    }

    // 논리 삭제 처리
    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;  // 삭제 처리 시각
    }

    // 논리 삭제 여부를 반환
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
