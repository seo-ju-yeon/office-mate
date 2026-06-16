package office_mate_2605.security.repository;

import office_mate_2605.common.employee.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 로그인 검증에 필요한 직원 정보를 조회하는 Repository. (작성자: 서주연)
 *
 * <p>Spring Security 인증 과정에서 사번 기준으로 논리 삭제되지 않은 Employee를 조회한다.
 * APIUserDetailsService가 이 Repository를 통해 로그인 대상 직원을 찾고 UserDetails로 변환한다.</p>
 */
@Repository
public interface APIUserRepository extends JpaRepository<Employee, String> {
    // 로그인 검증 전용 직원 조회 Repository
    // Employee는 조회 대상 Entity, String은 employee_no PK 타입

    // 사번으로 논리 삭제되지 않은 직원 1명을 조회
    // 조회 결과가 없을 수 있으므로 Optional로 반환
    Optional<Employee> findByEmployeeNoAndDeletedAtIsNull(String employeeNo);
}
