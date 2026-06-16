package office_mate_2605.common.employee.repository;

import office_mate_2605.common.employee.domain.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 직원별 시스템 역할 매핑을 관리하는 Repository. (작성자: 서주연)
 *
 * <p>employee_roles 테이블의 복합키를 기준으로 직원에게 부여된 역할을 조회하거나 삭제한다.
 * 로그인 시 Spring Security 권한을 구성하고, 직원 역할 변경 시 기존 권한 매핑을 정리하는 데 사용한다.</p>
 */
@Repository
public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, EmployeeRole.EmployeeRoleId> {
    /* 직원-역할 매핑 */

    // 특정 직원의 모든 권한 조회
    // findById + EmployeeNo: 복합키(Id) 내부의 특정 필드(employeeNo)를 조건으로 조회
    // APIUserDetailsService에서 로그인 사용자의 권한(Authorities)을 로드할 때 사용
    List<EmployeeRole> findByIdEmployeeNo(String employeeNo);

    // 특정 직원의 모든 권한 제거
    // 직원의 권한을 수정할 때, 기존 매핑을 싹 지우고 새로 입력하기 위한 용도
    // 시스템 역할 변경 시 employee.role과 employee_roles를 같은 값으로 동기화하기 위해 사용
    void deleteByIdEmployeeNo(String employeeNo);
}
