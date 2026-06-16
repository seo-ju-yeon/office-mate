package office_mate_2605.common.employee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.domain.EmployeeRole;
import office_mate_2605.common.employee.domain.EmploymentStatus;
import office_mate_2605.common.employee.domain.SystemRole;
import office_mate_2605.common.employee.dto.EmployeeCreateRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeCreateResponseDTO;
import office_mate_2605.common.employee.dto.EmployeeManagementUpdateRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeResignRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeResponseDTO;
import office_mate_2605.common.employee.dto.EmployeeUpdateRequestDTO;
import office_mate_2605.common.employee.repository.EmployeeRepository;
import office_mate_2605.common.employee.repository.EmployeeRoleRepository;
import office_mate_2605.management.auth.repository.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 직원 관리 비즈니스 규칙을 처리하는 Service. (작성자: 서주연)
 *
 * <p>직원 등록, 목록 조회, 조직도 조회, 기본 정보 수정, SUPER 전용 관리 정보 수정,
 * 퇴사 처리, 논리 삭제, 채팅용 직원 조회 같은 직원 생명주기 기능을 담당한다.</p>
 *
 * <p>사번/이메일 중복 검증, 역할 부여 권한 검증, 마지막 ACTIVE SUPER 보호,
 * employee.role과 employee_roles 동기화, 휴직/퇴사 계정의 refreshToken 폐기까지
 * 직원 관리와 보안에 연결된 핵심 업무 규칙을 한 곳에서 처리한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class EmployeeService {
    // 직원 관리의 비즈니스 규칙을 담당

    // 임시 비밀번호
    private static final String INITIAL_PASSWORD = "1111";

    // 직원 정보 조회, 저장, 중복 확인, 논리 삭제에 사용
    private final EmployeeRepository employeeRepository;
    // 직원 시스템 역할 매핑 저장과 동기화에 사용
    private final EmployeeRoleRepository employeeRoleRepository;
    // 휴직/퇴사/삭제 계정의 refresh token 폐기에 사용
    private final RefreshTokenRepository refreshTokenRepository;
    // 초기 비밀번호를 BCrypt 해시로 암호화
    private final PasswordEncoder passwordEncoder;

    // 신규 직원 등록
    @Transactional
    public EmployeeCreateResponseDTO createEmployee(EmployeeCreateRequestDTO request,
                                                    String creatorEmployeeNo,
                                                    SystemRole creatorRole) {
        String normalizedEmployeeNo = normalizeEmployeeNo(request.employeeNo());
        String normalizedEmail = normalizeEmail(request.email());

        // 1. 직원 등록 화면에서 사번과 이메일 중복확인을 완료했는지 먼저 확인
        validateEmployeeNoDuplicateChecked(request);
        validateEmailDuplicateChecked(request);

        // 2. 사번과 이메일 중복을 DB에서 다시 확인
        validateDuplicate(normalizedEmployeeNo, normalizedEmail);

        // 3. 요청 role이 비어 있으면 일반 직원 USER로 생성
        SystemRole targetRole = request.role() == null ? SystemRole.USER : request.role();

        // 4. 직원 생성자가 해당 역할을 부여할 수 있는지 확인
        validateRoleGrantPermission(creatorRole, targetRole);

        // 5. Employee Entity 생성
        // 초기 비밀번호 1111은 반드시 BCrypt로 암호화해서 저장
        Employee employee = Employee.builder()
                .employeeNo(normalizedEmployeeNo)
                .password(passwordEncoder.encode(INITIAL_PASSWORD))
                .name(request.name())
                .email(normalizedEmail)
                .department(request.department())
                .position(request.position())
                .role(targetRole)
                .joinedOn(request.joinedOn() == null ? LocalDate.now() : request.joinedOn())
                .build();

        // 6. employee 테이블에 저장
        Employee savedEmployee = employeeRepository.save(employee);

        EmployeeRole employeeRole = EmployeeRole.builder()
                .employee(savedEmployee)
                .roleCode(targetRole)  // USER, ADMIN, SUPER
                .grantedBy(creatorEmployeeNo)  // 권한을 부여한 사람
                .build();
        employeeRoleRepository.save(employeeRole);

        return EmployeeCreateResponseDTO.from(savedEmployee);
    }

    // 직원 목록 조회
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> getEmployees() {
        // 조회 결과 Entity 목록은 화면/API 응답용 DTO 목록으로 변환
        return employeeRepository.findByDeletedAtIsNullOrderByEmployeeNoAsc()
                .stream()
                .map(EmployeeResponseDTO::from)
                .toList();
    }

    // 조직도 화면에서 사용할 직원 목록 조회
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> getOrganizationEmployees() {
        // 퇴사자는 조직도 구성원으로 보여주지 않고, 화면 필터가 사용할 재직/휴직 직원만 내려줌
        // - 조직도 전체: ACTIVE 직원만 표시
        // - 부서별 보기: ACTIVE, ON_LEAVE 직원을 상태 필터로 나눠 표시
        return employeeRepository.findByStatusInAndDeletedAtIsNullOrderByDepartmentAscPositionDescEmployeeNoAsc(
                        List.of(EmploymentStatus.ACTIVE, EmploymentStatus.ON_LEAVE)
                )
                .stream()
                .map(EmployeeResponseDTO::from)
                .toList();
    }

    // 사번 중복확인
    @Transactional(readOnly = true)
    public boolean existsEmployeeNo(String employeeNo) {
        // employee 테이블 단일 조회이므로 JPA Repository의 existsByEmployeeNo를 사용
        return employeeRepository.existsByEmployeeNo(normalizeEmployeeNo(employeeNo));
    }

    // 이메일 중복확인
    @Transactional(readOnly = true)
    public boolean existsEmail(String email) {
        // 이메일은 대소문자로 인한 중복 허용을 막기 위해 소문자로 정규화 후 확인
        return employeeRepository.existsByEmail(normalizeEmail(email));
    }

    // 사번으로 직원 조회
    @Transactional(readOnly = true)
    public EmployeeResponseDTO getEmployee(String employeeNo) {
        // 사번으로 논리 삭제되지 않은 직원 1명을 조회
        Employee employee = findActiveEmployee(employeeNo);
        return EmployeeResponseDTO.from(employee);
    }

    // 직원 정보 수정
    @Transactional
    public EmployeeResponseDTO updateEmployee(String employeeNo,
                                              EmployeeUpdateRequestDTO request,
                                              SystemRole updaterRole) {
        String normalizedEmployeeNo = normalizeEmployeeNo(employeeNo);

        // 수정 대상 직원을 먼저 조회
        Employee employee = findActiveEmployee(normalizedEmployeeNo);

        // 이메일은 UNIQUE 제약이 있으므로 다른 직원과 중복되면 미리 차단
        if (request.email() != null && employeeRepository.existsByEmailAndEmployeeNoNot(request.email(), normalizedEmployeeNo)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 역할 변경 요청이 있을 때만 권한 검사를 수행
        if (request.role() != null) {
            throw new IllegalStateException("시스템 역할 변경은 직원 관리 전용 API를 사용해주세요.");
        }

        // 이름/이메일/부서/직급은 Entity 도메인 메서드로 변경
        // null 값은 Entity 내부에서 기존 값을 유지
        employee.updateInfo(
                request.name(),
                request.email(),
                request.department(),
                request.position()
        );

        return EmployeeResponseDTO.from(employee);
    }

    // SUPER 전용 직원 관리 정보 수정
    // 이름, 부서, 직급 같은 기본 정보와 시스템 권한, 재직 상태를 함께 변경
    // role/status 변경은 로그인 가능 여부와 권한 범위에 직접 영향을 주므로 일반 수정 API와 분리
    @Transactional
    public EmployeeResponseDTO updateEmployeeManagementInfo(String employeeNo,
                                                            EmployeeManagementUpdateRequestDTO request,
                                                            String actorEmployeeNo,
                                                            SystemRole actorRole) {
        // 직원 관리용 수정 API의 핵심 비즈니스 로직
        // 이름/부서/직급 같은 기본 정보 수정과 시스템 역할/재직 상태 변경은 보안 영향도가 다름
        // 특히 role, status는 로그인 가능 여부와 권한 범위에 직접 영향을 주므로 SUPER 전용 흐름에서만 처리
        validateSuperActor(actorRole);

        // 수정 대상 직원은 퇴사 상태일 수도 있으므로 ACTIVE가 아니라 삭제되지 않은 직원 기준으로 조회
        Employee employee = findActiveOrManagedEmployee(employeeNo);

        // 요청 DTO는 전체 필드를 보내는 관리용 폼이지만 null 가능성을 고려
        // null로 들어온 값은 기존 직원 값을 기본값으로 사용
        // 계산된 다음 역할과 상태는 마지막 ACTIVE SUPER 보호 검증에 사용
        SystemRole nextRole = request.role() == null ? employee.getRole() : request.role();
        EmploymentStatus nextStatus = request.status() == null ? employee.getStatus() : request.status();

        // 대상 직원이 마지막 ACTIVE SUPER라면 강등/휴직/퇴사 처리로 최고관리자가 사라지는 것을 방지
        validateAtLeastOneActiveSuperRemains(employee, nextRole, nextStatus);

        // 관리용 수정에서는 이메일과 사번을 변경하지 않음
        // 사번은 DB trigger로 변경 금지되어 있음
        // 이메일은 중복 검증과 계정 복구 흐름에 연결되므로 별도 기능으로 분리하는 편이 안전
        employee.updateInfo(
                request.name(),
                null,
                request.department(),
                request.position()
        );

        // employee.role은 직원의 기본 시스템 역할
        // employee_roles는 Spring Security 권한 조회 시 함께 사용되는 역할 매핑
        // 둘 중 하나만 바꾸면 로그인 권한이 꼬일 수 있으므로 항상 함께 동기화
        if (request.role() != null && employee.getRole() != request.role()) {
            employee.changeRole(request.role());
            syncEmployeeRoleMapping(employee, request.role(), actorEmployeeNo);
        }

        // 재직 상태 변경 규칙
        // RESIGNED로 변경하면 퇴사일을 기록하고, 요청에 퇴사일이 없으면 오늘 날짜를 사용
        // ACTIVE 또는 ON_LEAVE로 변경하면 퇴사일을 null로 비워 현재 상태와 모순되지 않게 유지
        if (request.status() != null) {
            LocalDate resignedOn = request.status() == EmploymentStatus.RESIGNED
                    ? (request.resignedOn() == null ? LocalDate.now() : request.resignedOn())
                    : null;
            employee.changeEmploymentStatus(request.status(), resignedOn);
            revokeRefreshTokensIfInactive(employee);
        }

        return EmployeeResponseDTO.from(employee);
    }

    // 퇴사 처리
    @Transactional
    public EmployeeResponseDTO resignEmployee(String employeeNo, EmployeeResignRequestDTO request) {
        // 퇴사 처리는 실제 DELETE가 아니라 status를 RESIGNED로 변경하는 것
        Employee employee = findActiveEmployee(employeeNo);
        validateAtLeastOneActiveSuperRemains(employee, employee.getRole(), EmploymentStatus.RESIGNED);

        // 퇴사일이 없으면 오늘 날짜를 기본값으로 사용
        LocalDate resignedOn = request.resignedOn() == null ? LocalDate.now() : request.resignedOn();
        employee.resign(resignedOn);
        revokeRefreshTokensIfInactive(employee);
        return EmployeeResponseDTO.from(employee);
    }

    // 직원 데이터 자체를 일반 관리 대상에서 제외
    // deleted_at이 채워져 조회 목록에서 숨김
    @Transactional
    public void deleteEmployee(String employeeNo) {
        Employee employee = findActiveEmployee(employeeNo);
        validateAtLeastOneActiveSuperRemains(employee, employee.getRole(), EmploymentStatus.RESIGNED);
        employee.softDelete(LocalDateTime.now());
        revokeRefreshTokens(employee.getEmployeeNo());
    }

    // 신규 직원 등록 전에 사번과 이메일 중복 여부를 검증하는 메서드
    // DB 기준으로 다시 확인해 프론트엔드 중복확인 우회를 차단
    private void validateDuplicate(String normalizedEmployeeNo, String normalizedEmail) {
        // 사번은 로그인 ID라서 중복되면 안 됨
        if (employeeRepository.existsByEmployeeNo(normalizedEmployeeNo)) {
            throw new IllegalArgumentException("이미 사용 중인 사번입니다.");
        }

        // 이메일도 비밀번호 찾기에 쓰이므로 중복을 막음
        if (employeeRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
    }

    // 직원 등록 전에 사번 중복확인을 반드시 거쳤는지 검증하는 메서드
    // 화면에서 중복확인 버튼을 누르지 않은 요청은 서버에서도 차단
    private void validateEmployeeNoDuplicateChecked(EmployeeCreateRequestDTO request) {
        if (!Boolean.TRUE.equals(request.employeeNoDuplicateChecked())) {
            // true이면 통과, false이면 차단, null이어도 NullPointerException 없이 차단
            throw new IllegalStateException("직원 등록 전 사번 중복확인을 완료해주세요.");
        }
    }

    // 직원 등록 전에 이메일 중복확인을 반드시 거쳤는지 검증하는 메서드
    // 화면에서 중복확인 버튼을 누르지 않은 요청은 서버에서도 차단
    private void validateEmailDuplicateChecked(EmployeeCreateRequestDTO request) {
        if (!Boolean.TRUE.equals(request.emailDuplicateChecked())) {
            // true이면 통과, false이면 차단, null이어도 NullPointerException 없이 차단
            throw new IllegalStateException("직원 등록 전 이메일 중복확인을 완료해주세요.");
        }
    }

    // 직원 생성자가 요청한 시스템 역할을 부여할 수 있는지 검증하는 메서드
    // SUPER는 모든 역할을 부여할 수 있고 ADMIN은 USER 역할만 생성 가능
    private void validateRoleGrantPermission(SystemRole creatorRole, SystemRole targetRole) {
        // SUPER는 모든 시스템 역할을 부여할 수 있다.
        if (creatorRole == SystemRole.SUPER) {
            return;
        }

        // ADMIN도 직원 등록은 가능하지만 관리자/최고관리자 계정은 임의 생성하지 못하게 막음
        if (targetRole != SystemRole.USER) {
            throw new IllegalStateException("ADMIN은 USER 역할 직원만 생성할 수 있습니다.");
        }
    }

    // 직원 관리 정보 수정 요청자가 SUPER 권한인지 검증하는 메서드
    // 시스템 권한과 재직 상태 변경은 최고관리자 전용 흐름으로 제한
    private void validateSuperActor(SystemRole actorRole) {
        // 직원 관리 정보 수정은 SUPER 전용
        // ADMIN에게 일부 수정 권한을 열면 SUPER 승격/강등, 마지막 SUPER 보호 같은 예외 규칙이 복잡해짐
        // 현재 단계에서는 최고관리자만 role/status 변경을 수행할 수 있게 제한
        if (actorRole != SystemRole.SUPER) {
            throw new IllegalStateException("SUPER 권한만 직원 관리 정보를 수정할 수 있습니다.");
        }
    }

    // 직원 권한/상태 변경 후에도 최소 1명의 ACTIVE SUPER가 남는지 검증하는 메서드
    // 마지막 최고관리자가 강등, 휴직, 퇴사, 삭제되어 권한 복구가 불가능해지는 상황을 차단
    private void validateAtLeastOneActiveSuperRemains(Employee targetEmployee,
                                                      SystemRole nextRole,
                                                      EmploymentStatus nextStatus) {
        // 시스템 운영을 위해 최소 1명의 로그인 가능한 SUPER 계정은 항상 남아 있어야 함
        // 현재 대상 직원이 SUPER이고 ACTIVE 상태이며 논리 삭제되지 않은 계정이면 보호 대상
        // 이 직원이 강등, 휴직, 퇴사, 삭제되면 ACTIVE SUPER 수가 줄 수 있으므로 남은 수 확인
        boolean targetCurrentlyActiveSuper = targetEmployee.getRole() == SystemRole.SUPER
                && targetEmployee.getStatus() == EmploymentStatus.ACTIVE
                && !targetEmployee.isDeleted();

        // 변경 이후에도 대상 직원이 ACTIVE SUPER라면 시스템 최고관리자 수가 줄지 않음
        // 이 경우에는 추가 검증 없이 통과
        boolean targetRemainsActiveSuper = nextRole == SystemRole.SUPER
                && nextStatus == EmploymentStatus.ACTIVE;

        if (!targetCurrentlyActiveSuper || targetRemainsActiveSuper) {
            return;
        }

        long activeSuperCount = employeeRepository.countByRoleAndStatusAndDeletedAtIsNull(
                SystemRole.SUPER,
                EmploymentStatus.ACTIVE
        );

        // 현재 ACTIVE SUPER가 1명뿐인데 그 1명을 강등/휴직/퇴사/삭제하면 권한 복구가 어려움
        // 이후 누구도 ADMIN을 SUPER로 승격하거나 권한 정책을 복구할 수 없으므로 차단
        if (activeSuperCount <= 1) {
            throw new IllegalStateException("최소 1명의 ACTIVE SUPER 계정은 유지되어야 합니다.");
        }
    }

    // employee.role과 employee_roles 권한 매핑을 같은 역할로 동기화하는 메서드
    // 역할 변경 시 오래된 권한 매핑을 제거하고 새 역할 매핑을 저장
    private void syncEmployeeRoleMapping(Employee employee, SystemRole role, String actorEmployeeNo) {
        // 현재 프로젝트는 employee.role을 기본 역할로 사용하면서 employee_roles도 로그인 권한 조회에 함께 포함
        // 역할 변경 시 기존 매핑을 지우고 새 역할 하나만 저장해 오래된 권한 흔적 제거
        // employee.role과 employee_roles를 같은 단일 역할 상태로 맞춰 권한 판단을 단순하게 유지
        employeeRoleRepository.deleteByIdEmployeeNo(employee.getEmployeeNo());
        EmployeeRole employeeRole = EmployeeRole.builder()
                .employee(employee)
                .roleCode(role)
                .grantedBy(actorEmployeeNo)
                .build();
        employeeRoleRepository.save(employeeRole);
    }

    // 직원이 ACTIVE 상태가 아니면 refresh token을 폐기하는 메서드
    // 휴직/퇴사 계정이 새 accessToken을 발급받지 못하게 차단
    private void revokeRefreshTokensIfInactive(Employee employee) {
        // accessToken은 stateless라서 즉시 DB에서 지울 수 없음
        // 대신 휴직/퇴사 계정의 refresh token을 폐기해 새 accessToken 재발급을 막음
        // 기존 accessToken은 TokenCheckFilter가 다음 API 요청에서 ACCOUNT_INACTIVE로 차단
        if (employee.getStatus() != EmploymentStatus.ACTIVE) {
            revokeRefreshTokens(employee.getEmployeeNo());
        }
    }

    // 직원의 유효한 refresh token을 폐기 처리하는 메서드
    // 물리 삭제 대신 revokedAt을 채워 인증 이력을 남김
    private void revokeRefreshTokens(String employeeNo) {
        // refresh token은 물리 삭제하지 않고 revokedAt을 채워 감사 흔적을 남김
        // AuthService의 로그아웃 처리와 같은 정책
        refreshTokenRepository.findByEmployeeEmployeeNoAndRevokedAtIsNull(employeeNo)
                .forEach(refreshToken -> refreshToken.revoke());
    }

    // 논리 삭제되지 않은 직원을 사번으로 조회하는 메서드
    // 찾지 못하면 직원 없음 예외를 발생
    private Employee findActiveEmployee(String employeeNo) {
        // 여러 메서드에서 공통으로 쓰는 직원 조회 로직이다.
        // 찾지 못하면 Service 단계에서 의미 있는 예외 메시지를 던진다.
        return employeeRepository.findByEmployeeNoAndDeletedAtIsNull(normalizeEmployeeNo(employeeNo))
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));
    }

    // 관리 화면에서 수정할 수 있는 직원을 사번으로 조회하는 메서드
    // 휴직/퇴사 상태도 복구 대상이 될 수 있으므로 논리 삭제 여부만 확인
    private Employee findActiveOrManagedEmployee(String employeeNo) {
        // 관리용 수정에서는 휴직/퇴사 상태인 직원도 다시 ACTIVE로 복구할 수 있어야 함
        // 그래서 재직자만 의미하는 이름 대신 논리 삭제되지 않은 직원 전체를 관리 대상으로 조회
        return employeeRepository.findByEmployeeNoAndDeletedAtIsNull(normalizeEmployeeNo(employeeNo))
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));
    }

    // 사번 입력값의 공백을 제거하고 대문자로 정규화하는 메서드
    // null 또는 빈 문자열이면 업무 예외를 발생
    private String normalizeEmployeeNo(String employeeNo) {
        if (employeeNo == null || employeeNo.isBlank()) {
            throw new IllegalArgumentException("사번을 입력해주세요.");
        }
        return employeeNo.trim().toUpperCase(Locale.ROOT);
    }

    // 이메일 입력값의 공백을 제거하고 소문자로 정규화하는 메서드
    // null 또는 빈 문자열이면 업무 예외를 발생
    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // 채팅용: 전체 활성 직원 목록 조회
    public List<EmployeeResponseDTO> getAllEmployeesForChat() {
        // 휴직, 퇴직자 제외 직원들만 조회
        return employeeRepository
                .findByStatusAndDeletedAtIsNullOrderByDepartmentAscPositionDescEmployeeNoAsc(
                        EmploymentStatus.ACTIVE
                )
                .stream()
                .map(EmployeeResponseDTO::from)
                .toList();
    }

    // 채팅용: 특정 사번으로 직원 상세 정보 조회
    public EmployeeResponseDTO getEmployeeDetail(String employeeNo) {
        return employeeRepository.findByEmployeeNoAndDeletedAtIsNull(employeeNo)
                .map(emp -> new EmployeeResponseDTO(
                        emp.getEmployeeNo(),
                        emp.getName(),
                        emp.getEmail(),
                        emp.getDepartment(),
                        emp.getPosition(),
                        emp.getRole(),
                        emp.getStatus(),
                        emp.getJoinedOn(),
                        emp.getResignedOn(),
                        emp.isTempPasswordRequired()
                ))
                .orElse(null);
    }
}
