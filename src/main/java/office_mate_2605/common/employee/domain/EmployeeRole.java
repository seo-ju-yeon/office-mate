package office_mate_2605.common.employee.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 직원에게 부여된 추가 시스템 역할을 관리하는 Entity. (작성자: 서주연)
 *
 * <p>employee_no와 role_code를 복합 PK로 사용해 같은 직원에게 같은 역할이
 * 중복 부여되지 않도록 관리한다. 역할 부여 시각과 부여 관리자 사번을 기록해
 * 권한 변경 이력을 추적할 수 있게 한다.</p>
 */
@Entity
@Table(name = "employee_roles")
@Getter
@NoArgsConstructor
public class EmployeeRole {

    @EmbeddedId
    private EmployeeRoleId id;

    /*
     * 역할을 부여받은 직원.
     * 읽기 전용 연관관계. 역할 부여는 항상 직원 조회 후 서비스 레이어에서 처리한다.
     */
    @MapsId("employeeNo")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no")
    private Employee employee;

    /* 역할 부여 시각. */
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    /*
     * 역할을 부여한 관리자 사번.
     * 관리자에 의한 부여가 아닌 경우 null 가능.
     */
    @Column(name = "granted_by", length = 30)
    private String grantedBy;

    // =========================================================================
    // 생성자 (Builder)
    // =========================================================================

    @Builder
    private EmployeeRole(Employee employee, SystemRole roleCode, String grantedBy) {
        this.id = new EmployeeRoleId(employee.getEmployeeNo(), roleCode);
        this.employee = employee;
        this.grantedAt = LocalDateTime.now();
        this.grantedBy = grantedBy;
    }

    // =========================================================================
    // 복합 PK
    // =========================================================================

    /*
     * employee_roles 복합 PK.
     * (employee_no, role_code) 조합으로 같은 역할 중복 부여를 방지한다.
     */
    @Embeddable
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @EqualsAndHashCode
    public static class EmployeeRoleId implements Serializable {

        @Column(name = "employee_no", length = 30)
        private String employeeNo;

        @Enumerated(EnumType.STRING)
        @JdbcTypeCode(SqlTypes.NAMED_ENUM)
        @Column(name = "role_code")
        private SystemRole roleCode;

        public EmployeeRoleId(String employeeNo, SystemRole roleCode) {
            this.employeeNo = employeeNo;
            this.roleCode = roleCode;
        }
    }
}
