package office_mate_2605.management.employee.repository;

import office_mate_2605.common.employee.domain.DepartmentCode;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.domain.PositionCode;
import office_mate_2605.common.employee.domain.SystemRole;
import office_mate_2605.common.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class EmployeeRepositoryTest {

    /*
     * Repository 테스트 목적:
     * - Employee Entity가 employee 테이블에 정상 저장되는지 확인
     * - Spring Data JPA 메서드 이름 기반 쿼리가 의도대로 동작하는지 확인
     *
     * @SpringBootTest는 실제 Spring Boot 설정을 로딩한다.
     * @Transactional은 테스트가 끝난 뒤 DB 변경을 롤백해 테스트 데이터가 남지 않게 해준다.
     */

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void saveEmployeeAndFindByEmployeeNo() {
        // given: 저장할 테스트 직원 사번을 준비한다.
        String employeeNo = "T_REPO_001";

        // given: Employee Entity를 생성한다.
        // 테스트에서는 비밀번호 암호화 자체가 목적이 아니므로 임의 문자열을 넣는다.
        Employee employee = Employee.builder()
                .employeeNo(employeeNo)
                .password("encoded-password")
                .name("Repository Test User")
                .email("repo.test@example.com")
                .department(DepartmentCode.BACKEND)
                .position(PositionCode.STAFF)
                .role(SystemRole.USER)
                .joinedOn(LocalDate.now())
                .build();

        // when: JPA Repository로 employee 테이블에 저장한다.
        employeeRepository.save(employee);

        // when: 논리 삭제되지 않은 직원을 사번으로 다시 조회한다.
        Employee found = employeeRepository.findByEmployeeNoAndDeletedAtIsNull(employeeNo)
                .orElseThrow();

        // then: 저장한 값이 그대로 조회되는지 확인한다.
        assertEquals(employeeNo, found.getEmployeeNo());
        assertEquals("Repository Test User", found.getName());
        assertEquals(DepartmentCode.BACKEND, found.getDepartment());
        assertTrue(found.isTempPasswordRequired());

        // 테스트 리포트에서 눈으로 확인할 수 있는 출력값이다.
        System.out.println("[RepositoryTest] saved employeeNo = " + found.getEmployeeNo());
        System.out.println("[RepositoryTest] saved department = " + found.getDepartment());
    }

    @Test
    void existsByEmailAndEmployeeNoNotReturnsTrueForOtherEmployee() {
        // given: 이메일 중복 검사용 직원을 하나 저장한다.
        Employee employee = Employee.builder()
                .employeeNo("T_REPO_002")
                .password("encoded-password")
                .name("Email Owner")
                .email("repo.email.owner@example.com")
                .department(DepartmentCode.FRONTEND)
                .position(PositionCode.STAFF)
                .role(SystemRole.USER)
                .joinedOn(LocalDate.now())
                .build();

        employeeRepository.save(employee);

        // then: 다른 직원이 같은 이메일을 쓰려고 하면 true가 나와야 한다.
        assertTrue(employeeRepository.existsByEmailAndEmployeeNoNot("repo.email.owner@example.com", "OTHER_EMP"));

        // then: 같은 직원이 자기 이메일을 유지하는 경우는 false가 나와야 한다.
        assertFalse(employeeRepository.existsByEmailAndEmployeeNoNot("repo.email.owner@example.com", "T_REPO_002"));

        System.out.println("[RepositoryTest] duplicate email check = true for OTHER_EMP");
        System.out.println("[RepositoryTest] same employee email check = false for T_REPO_002");
    }
}
