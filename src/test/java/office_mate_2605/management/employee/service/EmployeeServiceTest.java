package office_mate_2605.management.employee.service;

import office_mate_2605.common.employee.domain.DepartmentCode;
import office_mate_2605.common.employee.domain.EmployeeRole;
import office_mate_2605.common.employee.domain.EmploymentStatus;
import office_mate_2605.common.employee.domain.PositionCode;
import office_mate_2605.common.employee.domain.SystemRole;
import office_mate_2605.common.employee.dto.EmployeeCreateRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeCreateResponseDTO;
import office_mate_2605.common.employee.dto.EmployeeManagementUpdateRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeUpdateRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeResponseDTO;
import office_mate_2605.common.employee.repository.EmployeeRepository;
import office_mate_2605.common.employee.repository.EmployeeRoleRepository;
import office_mate_2605.common.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class EmployeeServiceTest {

    /*
     * Service 테스트 목적:
     * - Controller 없이 EmployeeService를 직접 호출해 업무 규칙을 검증한다.
     * - 직원 생성 시 employee와 employee_roles가 함께 저장되는지 확인한다.
     * - ADMIN이 ADMIN 계정을 만들 수 없다는 권한 규칙을 확인한다.
     *
     * 테스트는 @Transactional 덕분에 끝나면 롤백된다.
     */

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeRoleRepository employeeRoleRepository;

    @Test
    void createEmployeeSavesEmployeeAndRoleMapping() {
        // given: 직원 생성 요청 DTO를 만든다.
        EmployeeCreateRequestDTO request = new EmployeeCreateRequestDTO(
                "T_SVC_001",
                "Service Test User",
                "service.test@example.com",
                DepartmentCode.BACKEND,
                PositionCode.ASSISTANT_MANAGER,
                SystemRole.USER,
                LocalDate.now(),
                true,
                true
        );

        // when: SUPER 권한 사용자가 직원을 생성한다고 가정한다.
        EmployeeCreateResponseDTO response = employeeService.createEmployee(request, null, SystemRole.SUPER);

        // then: 응답 DTO의 핵심 값이 요청과 일치하는지 확인한다.
        assertEquals("T_SVC_001", response.employeeNo());
        assertEquals(SystemRole.USER, response.role());
        assertTrue(response.tempPasswordRequired());

        // then: employee 테이블에 실제 저장되었는지 확인한다.
        assertTrue(employeeRepository.existsByEmployeeNo("T_SVC_001"));

        // then: employee_roles 테이블에도 기본 역할 USER가 저장되었는지 확인한다.
        assertTrue(employeeRoleRepository.existsById(new EmployeeRole.EmployeeRoleId("T_SVC_001", SystemRole.USER)));

        // then: 초기 비밀번호 1111이 평문이 아니라 암호화된 값으로 저장되었는지 확인한다.
        String savedPassword = employeeRepository.findById("T_SVC_001").orElseThrow().getPassword();
        assertNotEquals("1111", savedPassword);

        System.out.println("[ServiceTest] created employeeNo = " + response.employeeNo());
        System.out.println("[ServiceTest] created role = " + response.role());
        System.out.println("[ServiceTest] tempPasswordRequired = " + response.tempPasswordRequired());
    }

    @Test
    void adminCannotCreateAdminEmployee() {
        // given: ADMIN 역할 직원을 만들려는 요청을 준비한다.
        EmployeeCreateRequestDTO request = new EmployeeCreateRequestDTO(
                "T_SVC_002",
                "Blocked Admin User",
                "blocked.admin@example.com",
                DepartmentCode.MANAGEMENT_SUPPORT,
                PositionCode.MANAGER,
                SystemRole.ADMIN,
                LocalDate.now(),
                true,
                true
        );

        // when & then:
        // ADMIN 권한 사용자가 ADMIN 계정을 만들려고 하면 예외가 발생해야 한다.
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> employeeService.createEmployee(request, null, SystemRole.ADMIN)
        );

        assertEquals("ADMIN은 USER 역할 직원만 생성할 수 있습니다.", exception.getMessage());
        System.out.println("[ServiceTest] blocked message = " + exception.getMessage());
    }

    @Test
    void createEmployeeRequiresEmployeeNoDuplicateCheck() {
        // given: 사번 중복확인을 하지 않은 직원 생성 요청을 준비한다.
        // employeeNoDuplicateChecked가 false이면 화면에서 중복확인 버튼을 누르지 않은 상태로 본다.
        EmployeeCreateRequestDTO request = new EmployeeCreateRequestDTO(
                "T_SVC_004",
                "No Duplicate Check User",
                "no.check@example.com",
                DepartmentCode.BACKEND,
                PositionCode.STAFF,
                SystemRole.USER,
                LocalDate.now(),
                false,
                false
        );

        // when & then:
        // 중복확인을 하지 않았다면 SUPER라도 직원 생성을 진행할 수 없어야 한다.
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> employeeService.createEmployee(request, null, SystemRole.SUPER)
        );

        assertEquals("직원 등록 전 사번 중복확인을 완료해주세요.", exception.getMessage());
        System.out.println("[ServiceTest] duplicate check required message = " + exception.getMessage());
    }

    @Test
    void createEmployeeNormalizesEmployeeNoToUpperCase() {
        // given: 소문자가 섞인 사번으로 직원 생성 요청을 만든다.
        EmployeeCreateRequestDTO request = new EmployeeCreateRequestDTO(
                "t_svc_005",
                "Lower Case Employee No",
                "lower.employee.no@example.com",
                DepartmentCode.BACKEND,
                PositionCode.STAFF,
                SystemRole.USER,
                LocalDate.now(),
                true,
                true
        );

        // when: 직원을 생성한다.
        EmployeeCreateResponseDTO response = employeeService.createEmployee(request, null, SystemRole.SUPER);

        // then: DB 기준 사번은 대문자로 저장되어야 한다.
        assertEquals("T_SVC_005", response.employeeNo());
        assertTrue(employeeRepository.existsByEmployeeNo("T_SVC_005"));
        assertTrue(employeeService.existsEmployeeNo("t_svc_005"));
    }

    @Test
    void updateEmployeeChangesBasicInfo() {
        // given: 수정 대상 직원을 먼저 생성한다.
        EmployeeCreateRequestDTO createRequest = new EmployeeCreateRequestDTO(
                "T_SVC_003",
                "Before Name",
                "before.name@example.com",
                DepartmentCode.FRONTEND,
                PositionCode.STAFF,
                SystemRole.USER,
                LocalDate.now(),
                true,
                true
        );
        employeeService.createEmployee(createRequest, null, SystemRole.SUPER);

        // given: 이름, 이메일, 부서, 직급을 바꾸는 수정 요청을 만든다.
        // role은 null이므로 기존 USER 역할을 유지한다.
        EmployeeUpdateRequestDTO updateRequest = new EmployeeUpdateRequestDTO(
                "After Name",
                "after.name@example.com",
                DepartmentCode.BACKEND,
                PositionCode.MANAGER,
                null
        );

        // when: SUPER 권한 사용자가 직원을 수정한다고 가정한다.
        EmployeeResponseDTO response = employeeService.updateEmployee("T_SVC_003", updateRequest, SystemRole.SUPER);

        // then: 수정된 값이 응답에 반영되어야 한다.
        assertEquals("After Name", response.name());
        assertEquals("after.name@example.com", response.email());
        assertEquals(DepartmentCode.BACKEND, response.department());
        assertEquals(PositionCode.MANAGER, response.position());

        System.out.println("[ServiceTest] updated name = " + response.name());
        System.out.println("[ServiceTest] updated position = " + response.position());
    }

    @Test
    void superCanUpdateManagementInfoAndSyncRoleMapping() {
        // given: 관리 수정 대상 직원을 USER로 생성한다.
        EmployeeCreateRequestDTO createRequest = new EmployeeCreateRequestDTO(
                "T_SVC_006",
                "Managed User",
                "managed.user@example.com",
                DepartmentCode.FRONTEND,
                PositionCode.STAFF,
                SystemRole.USER,
                LocalDate.now(),
                true,
                true
        );
        employeeService.createEmployee(createRequest, "SUPER001", SystemRole.SUPER);

        EmployeeManagementUpdateRequestDTO managementRequest = new EmployeeManagementUpdateRequestDTO(
                "Managed Admin",
                DepartmentCode.BACKEND,
                PositionCode.MANAGER,
                SystemRole.ADMIN,
                EmploymentStatus.ON_LEAVE,
                null
        );

        // when: SUPER 권한자가 이름/부서/직급/권한/재직상태를 변경한다.
        EmployeeResponseDTO response = employeeService.updateEmployeeManagementInfo(
                "T_SVC_006",
                managementRequest,
                "SUPER001",
                SystemRole.SUPER
        );

        // then: 관리 정보와 시스템 역할이 함께 변경되어야 한다.
        assertEquals("Managed Admin", response.name());
        assertEquals(DepartmentCode.BACKEND, response.department());
        assertEquals(PositionCode.MANAGER, response.position());
        assertEquals(SystemRole.ADMIN, response.role());
        assertEquals(EmploymentStatus.ON_LEAVE, response.status());

        // then: employee_roles도 기존 USER 매핑이 제거되고 ADMIN 매핑으로 동기화되어야 한다.
        assertFalse(employeeRoleRepository.existsById(new EmployeeRole.EmployeeRoleId("T_SVC_006", SystemRole.USER)));
        assertTrue(employeeRoleRepository.existsById(new EmployeeRole.EmployeeRoleId("T_SVC_006", SystemRole.ADMIN)));
    }

    @Test
    void adminCannotUpdateManagementInfo() {
        // given: 관리 수정 대상 직원을 USER로 생성한다.
        EmployeeCreateRequestDTO createRequest = new EmployeeCreateRequestDTO(
                "T_SVC_007",
                "Admin Block Target",
                "admin.block.target@example.com",
                DepartmentCode.BACKEND,
                PositionCode.STAFF,
                SystemRole.USER,
                LocalDate.now(),
                true,
                true
        );
        employeeService.createEmployee(createRequest, "SUPER001", SystemRole.SUPER);

        EmployeeManagementUpdateRequestDTO managementRequest = new EmployeeManagementUpdateRequestDTO(
                "Blocked Change",
                DepartmentCode.MARKETING,
                PositionCode.MANAGER,
                SystemRole.ADMIN,
                EmploymentStatus.ACTIVE,
                null
        );

        // when & then: ADMIN은 SUPER 전용 관리 수정 API의 비즈니스 규칙을 통과할 수 없다.
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> employeeService.updateEmployeeManagementInfo(
                        "T_SVC_007",
                        managementRequest,
                        "ADMIN001",
                        SystemRole.ADMIN
                )
        );

        assertEquals("SUPER 권한만 직원 관리 정보를 수정할 수 있습니다.", exception.getMessage());
    }

    @Test
    void organizationEmployeesExcludeResignedAndIncludeActiveAndOnLeave() {
        // given: 조직도 노출 정책을 확인하기 위해 재직/휴직/퇴사 직원을 각각 만든다.
        employeeService.createEmployee(new EmployeeCreateRequestDTO(
                "T_ORG_ACTIVE",
                "Org Active",
                "org.active@example.com",
                DepartmentCode.BACKEND,
                PositionCode.STAFF,
                SystemRole.USER,
                LocalDate.now(),
                true,
                true
        ), "SUPER001", SystemRole.SUPER);

        employeeService.createEmployee(new EmployeeCreateRequestDTO(
                "T_ORG_LEAVE",
                "Org Leave",
                "org.leave@example.com",
                DepartmentCode.BACKEND,
                PositionCode.STAFF,
                SystemRole.USER,
                LocalDate.now(),
                true,
                true
        ), "SUPER001", SystemRole.SUPER);

        employeeService.createEmployee(new EmployeeCreateRequestDTO(
                "T_ORG_RESIGNED",
                "Org Resigned",
                "org.resigned@example.com",
                DepartmentCode.BACKEND,
                PositionCode.STAFF,
                SystemRole.USER,
                LocalDate.now(),
                true,
                true
        ), "SUPER001", SystemRole.SUPER);

        // given: 조직도 API는 ACTIVE/ON_LEAVE만 내려주어야 하므로 상태를 나눠 세팅한다.
        employeeService.updateEmployeeManagementInfo(
                "T_ORG_LEAVE",
                new EmployeeManagementUpdateRequestDTO(
                        "Org Leave",
                        DepartmentCode.BACKEND,
                        PositionCode.STAFF,
                        SystemRole.USER,
                        EmploymentStatus.ON_LEAVE,
                        null
                ),
                "SUPER001",
                SystemRole.SUPER
        );
        employeeService.updateEmployeeManagementInfo(
                "T_ORG_RESIGNED",
                new EmployeeManagementUpdateRequestDTO(
                        "Org Resigned",
                        DepartmentCode.BACKEND,
                        PositionCode.STAFF,
                        SystemRole.USER,
                        EmploymentStatus.RESIGNED,
                        LocalDate.now()
                ),
                "SUPER001",
                SystemRole.SUPER
        );

        // when: 조직도용 직원 목록을 조회한다.
        List<EmployeeResponseDTO> organizationEmployees = employeeService.getOrganizationEmployees();

        // then: 재직/휴직 직원은 포함하고, 퇴사 직원은 제외해야 한다.
        assertTrue(organizationEmployees.stream().anyMatch(emp -> emp.employeeNo().equals("T_ORG_ACTIVE")));
        assertTrue(organizationEmployees.stream().anyMatch(emp -> emp.employeeNo().equals("T_ORG_LEAVE")));
        assertFalse(organizationEmployees.stream().anyMatch(emp -> emp.employeeNo().equals("T_ORG_RESIGNED")));
    }
}
