package office_mate_2605.common.employee.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.domain.EmployeeStatusRequest;
import office_mate_2605.common.employee.domain.EmploymentRequestType;
import office_mate_2605.common.employee.domain.EmploymentStatus;
import office_mate_2605.common.employee.domain.RequestStatus;
import office_mate_2605.common.employee.domain.SystemRole;
import office_mate_2605.common.employee.dto.EmployeeStatusRequestCreateRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeStatusRequestDecisionRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeStatusRequestResponseDTO;
import office_mate_2605.common.employee.dto.ReturnFromLeaveRequestDTO;
import office_mate_2605.common.employee.repository.EmployeeRepository;
import office_mate_2605.common.employee.repository.EmployeeStatusRequestRepository;
import office_mate_2605.management.auth.repository.RefreshTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * 직원 재직 상태 변경 신청 비즈니스 규칙을 처리하는 Service. (작성자: 서주연)
 *
 * <p>직원의 휴직/퇴사 신청 생성, 휴직자의 로그인 전 복직 신청 생성,
 * 본인 신청 이력 조회, SUPER의 승인 대기 목록 조회, 승인/반려 처리를 담당한다.</p>
 *
 * <p>신청 중복 방지, 신청 가능 상태 검증, 마지막 ACTIVE SUPER 보호,
 * 승인 시 실제 Employee 재직 상태 변경, 휴직/퇴사 계정의 refreshToken 폐기 같은
 * 상태 변경 흐름의 핵심 검증과 후처리를 수행한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class EmployeeStatusRequestService {
    // 직원의 휴직/퇴사 신청 담당

    private final EmployeeStatusRequestRepository statusRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // 직원 본인이 휴직 또는 퇴사 신청을 생성하는 메서드
    // 신청 가능 상태와 중복 신청 여부를 검증한 뒤 PENDING 신청 이력을 저장
    @Transactional
    public EmployeeStatusRequestResponseDTO createRequest(String employeeNo,
                                                          EmployeeStatusRequestCreateRequestDTO request) {
        String normalizedEmployeeNo = normalizeEmployeeNo(employeeNo);

        // 신청자는 논리 삭제되지 않은 직원이어야 함
        Employee employee = findManagedEmployee(normalizedEmployeeNo);

        // 퇴사자/휴직자의 중복 휴직 신청처럼 업무적으로 말이 되지 않는 신청을 막음
        validateRequestableEmployee(employee, request.requestType());

        // 한 직원에게 PENDING 신청은 하나만 허용
        // 휴직 신청과 퇴사 신청을 동시에 올리면 SUPER가 어떤 신청을 먼저 처리해야 하는지 모호해짐
        if (statusRequestRepository.existsByEmployeeEmployeeNoAndStatus(normalizedEmployeeNo, RequestStatus.PENDING)) {
            throw new IllegalStateException("이미 처리 대기 중인 신청이 있습니다.");
        }

        EmployeeStatusRequest statusRequest = EmployeeStatusRequest.builder()
                .employee(employee)
                .requestType(request.requestType())
                .reason(request.reason())
                .build();

        return EmployeeStatusRequestResponseDTO.from(statusRequestRepository.save(statusRequest));
    }

    // 휴직 상태 직원이 로그인 전 복직 신청을 생성하는 메서드
    // accessToken 없이 접근하므로 사번과 비밀번호를 검증한 뒤 신청 이력을 저장
    @Transactional
    public EmployeeStatusRequestResponseDTO createReturnFromLeaveRequest(ReturnFromLeaveRequestDTO request) {
        String normalizedEmployeeNo = normalizeEmployeeNo(request.employeeNo());

        // 휴직자는 일반 로그인이 막히므로 accessToken 없이 이 API에 접근
        // 그래서 사번과 비밀번호를 다시 검증해 본인만 복직 신청을 만들 수 있게 함
        Employee employee = findManagedEmployee(normalizedEmployeeNo);
        if (!passwordEncoder.matches(request.password(), employee.getPassword())) {
            throw new IllegalArgumentException("사번 또는 비밀번호가 올바르지 않습니다.");
        }

        if (employee.getStatus() != EmploymentStatus.ON_LEAVE) {
            throw new IllegalStateException("휴직 상태의 직원만 복직 신청을 할 수 있습니다.");
        }

        if (statusRequestRepository.existsByEmployeeEmployeeNoAndStatus(normalizedEmployeeNo, RequestStatus.PENDING)) {
            throw new IllegalStateException("이미 처리 대기 중인 신청이 있습니다.");
        }

        EmployeeStatusRequest statusRequest = EmployeeStatusRequest.builder()
                .employee(employee)
                .requestType(EmploymentRequestType.RETURN_FROM_LEAVE)
                .reason(request.reason())
                .build();

        return EmployeeStatusRequestResponseDTO.from(statusRequestRepository.save(statusRequest));
    }

    // 직원 본인의 재직 상태 변경 신청 이력을 최신순으로 조회하는 메서드
    @Transactional(readOnly = true)
    public List<EmployeeStatusRequestResponseDTO> getMyRequests(String employeeNo) {
        String normalizedEmployeeNo = normalizeEmployeeNo(employeeNo);

        return statusRequestRepository.findByEmployeeEmployeeNoOrderByRequestedAtDesc(normalizedEmployeeNo)
                .stream()
                .map(EmployeeStatusRequestResponseDTO::from)
                .toList();
    }

    // SUPER가 승인 대기 중인 재직 상태 변경 신청 목록을 조회하는 메서드
    @Transactional(readOnly = true)
    public List<EmployeeStatusRequestResponseDTO> getPendingRequests(SystemRole actorRole) {
        validateSuperActor(actorRole);

        return statusRequestRepository.findByStatusOrderByRequestedAtAsc(RequestStatus.PENDING)
                .stream()
                .map(EmployeeStatusRequestResponseDTO::from)
                .toList();
    }

    // SUPER가 휴직/퇴사/복직 신청을 승인 처리하는 메서드
    // 신청 이력을 승인 상태로 바꾸고 직원의 실제 재직 상태까지 함께 변경
    @Transactional
    public EmployeeStatusRequestResponseDTO approveRequest(Long requestId,
                                                           EmployeeStatusRequestDecisionRequestDTO request,
                                                           String actorEmployeeNo,
                                                           SystemRole actorRole) {
        validateSuperActor(actorRole);

        // id만으로 조회하지 않고 PENDING 조건을 함께 사용
        // 이미 다른 SUPER가 처리한 신청을 다시 승인하는 상황을 Service 진입 단계에서 차단
        EmployeeStatusRequest statusRequest = findPendingRequest(requestId);
        Employee actor = findManagedEmployee(actorEmployeeNo);
        Employee targetEmployee = statusRequest.getEmployee();

        // 승인 직전에도 신청자의 현재 상태를 다시 확인한다.
        validateRequestableEmployee(targetEmployee, statusRequest.getRequestType());

        EmploymentStatus nextStatus = resolveApprovedStatus(statusRequest.getRequestType());
        validateAtLeastOneActiveSuperRemains(targetEmployee, targetEmployee.getRole(), nextStatus);

        // 신청 이력을 먼저 승인 상태로 남긴 뒤, 직원의 실제 재직 상태를 변경한다.
        statusRequest.approve(actor, request.decisionComment());
        applyApprovedStatus(targetEmployee, statusRequest.getRequestType());

        // 휴직/퇴사로 바뀐 직원은 더 이상 자유롭게 활동하면 안 됨
        // refresh token을 폐기하고 남아 있는 access token은 TokenCheckFilter가 다음 API 요청에서 차단
        revokeRefreshTokensIfInactive(targetEmployee);

        return EmployeeStatusRequestResponseDTO.from(statusRequest);
    }

    // SUPER가 휴직/퇴사/복직 신청을 반려 처리하는 메서드
    // 직원 재직 상태는 변경하지 않고 신청 이력만 REJECTED로 변경
    @Transactional
    public EmployeeStatusRequestResponseDTO rejectRequest(Long requestId,
                                                          EmployeeStatusRequestDecisionRequestDTO request,
                                                          String actorEmployeeNo,
                                                          SystemRole actorRole) {
        validateSuperActor(actorRole);

        EmployeeStatusRequest statusRequest = findPendingRequest(requestId);
        Employee actor = findManagedEmployee(actorEmployeeNo);

        // 반려는 직원 상태를 바꾸지 않고 신청 이력만 REJECTED로 변경한다.
        statusRequest.reject(actor, request.decisionComment());

        return EmployeeStatusRequestResponseDTO.from(statusRequest);
    }

    // 신청자가 현재 신청 유형을 요청할 수 있는 상태인지 검증하는 메서드
    // 퇴사자 신청, ACTIVE 직원의 복직 신청, 휴직자의 중복 휴직 신청 같은 잘못된 흐름을 차단
    private void validateRequestableEmployee(Employee employee, EmploymentRequestType requestType) {
        // 이미 퇴사한 직원은 휴직/퇴사/복직 신청 대상이 아님
        // 논리 삭제 직원은 findManagedEmployee에서 이미 걸러짐
        if (employee.getStatus() == EmploymentStatus.RESIGNED) {
            throw new IllegalStateException("퇴사 상태의 직원은 재직 상태 변경을 신청할 수 없습니다.");
        }

        // 복직 신청은 휴직 상태에서만 가능
        // ACTIVE 직원이 복직 신청을 올리면 승인 후 상태 변화가 없어 이력 의미가 흐려짐
        if (requestType == EmploymentRequestType.RETURN_FROM_LEAVE) {
            if (employee.getStatus() != EmploymentStatus.ON_LEAVE) {
                throw new IllegalStateException("휴직 상태의 직원만 복직 신청을 할 수 있습니다.");
            }
            return;
        }

        // 휴직 신청은 현재 재직 중인 직원만 가능
        // 휴직 중인 직원이 다시 휴직 신청을 올리면 승인 후 상태 변화가 없어 업무 이력이 애매해짐
        if (requestType == EmploymentRequestType.LEAVE && employee.getStatus() != EmploymentStatus.ACTIVE) {
            throw new IllegalStateException("재직 중인 직원만 휴직 신청을 할 수 있습니다.");
        }
    }

    // 승인된 신청 유형을 실제 직원 재직 상태로 변환하는 메서드
    // 휴직 신청은 ON_LEAVE, 퇴사 신청은 RESIGNED, 복직 신청은 ACTIVE로 매핑
    private EmploymentStatus resolveApprovedStatus(EmploymentRequestType requestType) {
        return switch (requestType) {
            case LEAVE -> EmploymentStatus.ON_LEAVE;
            case RESIGN -> EmploymentStatus.RESIGNED;
            case RETURN_FROM_LEAVE -> EmploymentStatus.ACTIVE;
        };
    }

    // 승인된 신청 유형에 따라 직원 Entity의 실제 재직 상태를 변경하는 메서드
    // Entity 도메인 메서드를 사용해 잘못된 상태 전환을 한 번 더 방어
    private void applyApprovedStatus(Employee employee, EmploymentRequestType requestType) {
        // 신청 유형에 따라 employee의 실제 재직 상태를 변경

        // 휴직 신청은 Employee.takeLeave()로 처리
        // RESIGNED 직원 휴직 같은 잘못된 전환은 Entity에서도 한 번 더 방어
        if (requestType == EmploymentRequestType.LEAVE) {
            employee.takeLeave();
            return;
        }

        // 복직 신청은 휴직 중인 직원을 ACTIVE 상태로 복구
        // 휴직 상태가 아닌 직원 복직은 Entity에서도 한 번 더 방어
        if (requestType == EmploymentRequestType.RETURN_FROM_LEAVE) {
            employee.returnFromLeave();
            return;
        }

        // 퇴사 신청은 현재 희망 퇴사일 컬럼이 없으므로 승인일을 퇴사일로 기록
        // 추후 희망 퇴사일이 필요하면 employee_status_request에 requested_resigned_on 추가 고려
        employee.resign(LocalDate.now());
    }

    // 아직 처리 대기 중인 신청을 ID로 조회하는 메서드
    // 이미 승인/반려된 신청을 다시 처리하지 못하도록 PENDING 조건으로 조회
    private EmployeeStatusRequest findPendingRequest(Long requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("신청 ID는 필수입니다.");
        }

        return statusRequestRepository.findByIdAndStatus(requestId, RequestStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("처리 대기 중인 신청을 찾을 수 없습니다."));
    }

    // 신청 승인/반려 처리자가 SUPER 권한인지 검증하는 메서드
    private void validateSuperActor(SystemRole actorRole) {
        if (actorRole != SystemRole.SUPER) {
            throw new IllegalStateException("SUPER 권한만 휴직/퇴사 신청을 처리할 수 있습니다.");
        }
    }

    // 직원 상태 변경 후에도 최소 1명의 ACTIVE SUPER가 남는지 검증하는 메서드
    // 마지막 최고관리자가 휴직/퇴사 처리되어 권한 관리가 불가능해지는 상황을 차단
    private void validateAtLeastOneActiveSuperRemains(Employee targetEmployee,
                                                      SystemRole nextRole,
                                                      EmploymentStatus nextStatus) {
        // 마지막 ACTIVE SUPER가 휴직/퇴사 승인으로 사라지면 권한 관리나 다른 관리자 승격이 불가능해짐
        boolean targetCurrentlyActiveSuper = targetEmployee.getRole() == SystemRole.SUPER
                && targetEmployee.getStatus() == EmploymentStatus.ACTIVE
                && !targetEmployee.isDeleted();
        boolean targetRemainsActiveSuper = nextRole == SystemRole.SUPER
                && nextStatus == EmploymentStatus.ACTIVE;

        if (!targetCurrentlyActiveSuper || targetRemainsActiveSuper) {
            return;
        }

        long activeSuperCount = employeeRepository.countByRoleAndStatusAndDeletedAtIsNull(
                SystemRole.SUPER,
                EmploymentStatus.ACTIVE
        );
        if (activeSuperCount <= 1) {
            throw new IllegalStateException("최소 1명의 ACTIVE SUPER 계정은 유지되어야 합니다.");
        }
    }

    // 직원이 ACTIVE 상태가 아니면 refresh token을 폐기하는 메서드
    // 휴직/퇴사 계정이 새 accessToken을 재발급받지 못하게 차단
    private void revokeRefreshTokensIfInactive(Employee employee) {
        if (employee.getStatus() != EmploymentStatus.ACTIVE) {
            refreshTokenRepository.findByEmployeeEmployeeNoAndRevokedAtIsNull(employee.getEmployeeNo())
                    .forEach(refreshToken -> refreshToken.revoke());
        }
    }

    // 논리 삭제되지 않은 직원 정보를 조회하는 메서드
    // 휴직자는 복직/퇴사 신청 대상이 될 수 있으므로 ACTIVE 상태만 조회하지 않음
    private Employee findManagedEmployee(String employeeNo) {
        // 신청자/처리자는 논리 삭제되지 않은 직원이어야 함
        // 휴직자는 퇴사 신청이 가능해야 하므로 ACTIVE만 조회하지 않음
        return employeeRepository.findByEmployeeNoAndDeletedAtIsNull(normalizeEmployeeNo(employeeNo))
                .orElseThrow(() -> new IllegalArgumentException("직원을 찾을 수 없습니다."));
    }

    // 사번 입력값의 공백을 제거하고 대문자로 정규화하는 메서드
    // null 또는 빈 문자열이면 업무 예외를 발생시켜 잘못된 조회를 차단
    private String normalizeEmployeeNo(String employeeNo) {
        if (employeeNo == null || employeeNo.isBlank()) {
            throw new IllegalArgumentException("사번을 입력해주세요.");
        }
        return employeeNo.trim().toUpperCase(Locale.ROOT);
    }
}
