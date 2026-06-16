package office_mate_2605.common.employee.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서 기준 정보를 관리하는 Entity. (작성자: 서주연)
 *
 * <p>PostgreSQL department_code enum 값을 부서 PK로 사용하며,
 * 화면 표시명과 정렬 순서를 함께 보관한다.
 * 직원 등록, 조직도, 직원 조회 화면에서 부서 정보를 일관되게 표시하기 위한 기준 데이터이다.</p>
 */
@Entity
@Table(name = "departments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class Department {

    // 부서 코드. department_code enum 값을 PK로 사용
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "code")
    private DepartmentCode code;

    // 화면 표시명 (예: 프론트엔드팀, 백엔드팀 등)
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    // 화면 표시 순서 (숫자가 작을수록 먼저 표시)
    @Column(name = "sort_order", nullable = false)
    private Short sortOrder;
}
