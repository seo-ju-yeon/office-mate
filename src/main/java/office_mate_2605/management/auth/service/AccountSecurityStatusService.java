package office_mate_2605.management.auth.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.domain.SystemRole;
import office_mate_2605.common.employee.repository.EmployeeRepository;
import office_mate_2605.management.auth.domain.AccountSecurityStatus;
import office_mate_2605.management.auth.dto.AccountSecurityStatusPageResponseDTO;
import office_mate_2605.management.auth.dto.AccountSecurityStatusSearchRequestDTO;
import office_mate_2605.management.auth.repository.AccountSecurityStatusRepository;
import office_mate_2605.management.auth.repository.AccountSecurityStatusQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * 계정 보안 상태와 로그인 실패 잠금 정책을 처리하는 Service. (작성자: 서주연)
 *
 * <p>로그인 실패 횟수 누적, 잠금 기준 도달 시 계정 잠금,
 * 정상 로그인 성공 시 실패 횟수 초기화, 관리자 잠금 해제,
 * 계정 보안 관리 화면의 목록/단건 조회를 담당한다.</p>
 *
 * <p>존재하지 않는 사번에 대해서는 계정 존재 여부가 노출되지 않도록 조용히 처리하고,
 * 직원 기본 정보와 계정 보안 상태를 조합해 화면 응답 DTO를 생성한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AccountSecurityStatusService {
    // 계정 보안 상태의 비즈니스 규칙을 담당
    // 로그인 실패 횟수, 계정 잠금, 관리자 잠금 해제 정책은 Controller나 Filter가 아니라 Service에서 처리

    // 계정 잠금 기준 로그인 실패 횟수
    private static final int LOGIN_LOCK_THRESHOLD = 5;

    // 신규 계정 보안 상태 행을 명확하게 INSERT 처리하기 위해 사용
    // employee_no를 PK로 공유하는 Entity라 Repository save만 쓰면 신규/기존 판단이 꼬일 수 있음
    private final EntityManager entityManager;

    // 계정 보안 상태 조회, 생성, 잠금 상태 변경을 위해 사용
    private final AccountSecurityStatusRepository accountSecurityStatusRepository;

    // 직원 존재 여부 확인과 직원 기본 정보 조회를 위해 사용
    private final EmployeeRepository employeeRepository;

    // 계정 보안 관리 목록의 조건 검색과 DB 페이징 조회를 위해 사용
    private final AccountSecurityStatusQueryRepository accountSecurityStatusQueryRepository;

    // 로그인 실패를 기록하는 메서드
    // 존재하지 않는 사번은 계정 존재 여부 노출을 막기 위해 예외 없이 조용히 종료
    @Transactional
    public AccountSecurityStatus recordLoginFailure(String employeeNo) {
        // 입력된 사번을 조회 기준에 맞게 대문자로 정규화
        // null 또는 빈 문자열이면 로그인 실패 처리 대상이 아니므로 종료
        String normalizedEmployeeNo = normalizeEmployeeNoOrNull(employeeNo);
        if (normalizedEmployeeNo == null) {
            return null;
        }

        // 논리 삭제되지 않은 직원만 로그인 실패 기록 대상
        // 직원이 없으면 계정 존재 여부를 노출하지 않기 위해 null 반환
        return employeeRepository.findByEmployeeNoAndDeletedAtIsNull(normalizedEmployeeNo)
                .map(employee -> {
                    // 보안 상태 행이 없으면 새로 만들고, 있으면 기존 행 사용
                    AccountSecurityStatus securityStatus = getOrCreateSecurityStatus(employee);

                    // 실패 횟수 증가 후 기준 횟수 이상이면 Entity 내부에서 잠금 처리
                    securityStatus.recordLoginFailure(LOGIN_LOCK_THRESHOLD);
                    log.info("로그인 실패 기록. employeeNo={}, failCount={}, locked={}",
                            normalizedEmployeeNo,
                            securityStatus.getLoginFailCount(),
                            securityStatus.isAccountLocked());
                    return securityStatus;
                })
                .orElse(null);
    }

    // 계정 잠금 기준 횟수를 반환하는 메서드
    // 로그인 실패 응답과 화면 안내 문구에서 같은 기준값을 사용하기 위해 제공
    public int getLoginLockThreshold() {
        return LOGIN_LOCK_THRESHOLD;
    }

    // 정상 로그인 성공 시 연속 실패 횟수를 초기화하는 메서드
    // 잠긴 계정은 정상 로그인 자체가 차단되어야 하므로 잠금 상태이면 자동 초기화하지 않음
    @Transactional
    public void resetLoginFailures(String employeeNo) {
        // 사번이 비어 있으면 초기화 대상이 없으므로 종료
        String normalizedEmployeeNo = normalizeEmployeeNoOrNull(employeeNo);
        if (normalizedEmployeeNo == null) {
            return;
        }

        // 보안 상태 행이 있고 잠기지 않은 계정이면 실패 횟수 초기화
        accountSecurityStatusRepository.findById(normalizedEmployeeNo)
                .filter(securityStatus -> !securityStatus.isAccountLocked())
                .ifPresent(AccountSecurityStatus::resetLoginFailures);
    }

    // 계정 잠금 여부를 확인하는 메서드
    // 보안 상태 행이 없으면 실패 이력이 없는 계정으로 보고 잠기지 않은 상태로 판단
    @Transactional(readOnly = true)
    public boolean isAccountLocked(String employeeNo) {
        // 사번이 비어 있으면 잠긴 계정으로 판단하지 않음
        String normalizedEmployeeNo = normalizeEmployeeNoOrNull(employeeNo);
        if (normalizedEmployeeNo == null) {
            return false;
        }

        // 보안 상태가 존재하면 잠금 여부를 반환하고, 없으면 false 반환
        return accountSecurityStatusRepository.findById(normalizedEmployeeNo)
                .map(AccountSecurityStatus::isAccountLocked)
                .orElse(false);
    }

    // 계정 보안 관리 화면의 목록을 페이지 단위로 조회하는 메서드
    // 검색 조건 조합과 COUNT/LIMIT 조회는 Query Repository에 위임
    @Transactional(readOnly = true)
    public AccountSecurityStatusPageResponseDTO getAccountSecurityStatuses(AccountSecurityStatusSearchRequestDTO request) {
        // 요청 DTO에서 보정된 page/size 값을 계산함
        int page = request.normalizedPage();
        int size = request.normalizedSize();

        // 현재 페이지에 표시할 목록을 DB에서 조회함
        var content = accountSecurityStatusQueryRepository.findAll(request);

        // 페이지네이션 계산에 사용할 전체 건수를 DB에서 조회함
        long totalElements = accountSecurityStatusQueryRepository.count(request);

        // 목록과 페이지 정보를 하나의 응답 DTO로 묶어 반환함
        return AccountSecurityStatusPageResponseDTO.of(content, page, size, totalElements);
    }

    // 관리자 화면에서 직원의 계정 보안 상태를 조회하는 메서드
    // 아직 보안 상태 행이 없으면 기본 상태 행을 생성해 반환
    @Transactional
    public AccountSecurityStatus getSecurityStatus(String employeeNo) {
        // 조회 대상 직원이 실제 존재하는지 먼저 확인
        Employee employee = findEmployee(employeeNo);

        // 보안 상태 행이 없으면 새로 생성
        return getOrCreateSecurityStatus(employee);
    }

    // 관리자가 계정 잠금을 해제하는 메서드
    // 해제 후 실패 횟수도 0으로 초기화해 다시 정상 로그인할 수 있게 처리
    @Transactional
    public AccountSecurityStatus unlockAccount(String employeeNo, String adminEmployeeNo) {
        // 잠금 해제 대상 직원 조회
        Employee targetEmployee = findEmployee(employeeNo);

        // 잠금 해제를 수행하는 관리자 직원 조회
        Employee adminEmployee = findEmployee(adminEmployeeNo);

        // Controller 권한 검사와 별개로 Service에서도 관리자 권한 확인
        validateUnlockAdmin(adminEmployee);

        // 대상 직원의 보안 상태가 없으면 생성한 뒤 잠금 해제 처리
        AccountSecurityStatus securityStatus = getOrCreateSecurityStatus(targetEmployee);
        securityStatus.unlock(adminEmployee);
        return securityStatus;
    }

    // 직원의 계정 보안 상태를 조회하거나 없으면 새로 생성하는 메서드
    // 첫 로그인 실패 또는 관리 이벤트 시점에 보안 상태 행이 만들어질 수 있음
    private AccountSecurityStatus getOrCreateSecurityStatus(Employee employee) {
        return accountSecurityStatusRepository.findById(employee.getEmployeeNo())
                .orElseGet(() -> {
                    // account_security_status는 employee_no를 PK로 쓰기 때문에 새 Entity도 PK 값이 이미 들어감
                    // Repository save에 맡기면 JPA가 신규 INSERT가 아니라 기존 행 UPDATE로 판단할 수 있음
                    // 첫 로그인 실패 시 "row was updated or deleted" 오류가 날 수 있어 persist로 명확히 INSERT 처리
                    AccountSecurityStatus securityStatus = AccountSecurityStatus.builder()
                            .employee(employee)
                            .build();
                    entityManager.persist(securityStatus);
                    return securityStatus;
                });
    }

    // 사번으로 논리 삭제되지 않은 직원을 조회하는 메서드
    // 직원이 없으면 업무 예외를 발생
    private Employee findEmployee(String employeeNo) {
        return employeeRepository.findByEmployeeNoAndDeletedAtIsNull(normalizeEmployeeNo(employeeNo))
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));
    }

    // 잠금 해제 요청자가 ADMIN 또는 SUPER 권한인지 검증하는 메서드
    // Controller의 @PreAuthorize와 별개로 Service에서도 한 번 더 방어
    private void validateUnlockAdmin(Employee adminEmployee) {
        // 잠금 해제는 관리자 계정만 수행 가능
        if (adminEmployee.getRole() != SystemRole.ADMIN && adminEmployee.getRole() != SystemRole.SUPER) {
            throw new IllegalStateException("관리자만 계정 잠금을 해제할 수 있습니다.");
        }
    }

    // 필수 사번 입력값을 정규화하는 메서드
    // null 또는 빈 문자열이면 업무 예외를 발생
    private String normalizeEmployeeNo(String employeeNo) {
        if (employeeNo == null || employeeNo.isBlank()) {
            throw new IllegalArgumentException("사번을 입력해주세요.");
        }
        return employeeNo.trim().toUpperCase(Locale.ROOT);
    }

    // 선택 사번 입력값을 정규화하는 메서드
    // null 또는 빈 문자열이면 예외 대신 null 반환
    private String normalizeEmployeeNoOrNull(String employeeNo) {
        if (employeeNo == null || employeeNo.isBlank()) {
            return null;
        }
        return employeeNo.trim().toUpperCase(Locale.ROOT);
    }

}
