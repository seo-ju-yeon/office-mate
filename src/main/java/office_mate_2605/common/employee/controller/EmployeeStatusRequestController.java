package office_mate_2605.common.employee.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.common.employee.domain.SystemRole;
import office_mate_2605.common.employee.dto.EmployeeStatusRequestCreateRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeStatusRequestDecisionRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeStatusRequestResponseDTO;
import office_mate_2605.common.employee.dto.ReturnFromLeaveRequestDTO;
import office_mate_2605.common.employee.service.EmployeeStatusRequestService;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 직원 재직 상태 변경 신청 API를 처리하는 Controller. (작성자: 서주연)
 *
 * <p>직원이 마이페이지에서 휴직/퇴사 신청을 생성하고,
 * 휴직 중인 직원이 로그인 전 복직 신청을 제출할 수 있도록 요청을 받는다.</p>
 *
 * <p>SUPER 권한 사용자는 관리 화면에서 승인 대기 신청을 조회하고,
 * 신청 유형에 따라 휴직, 퇴사, 복직 처리를 승인하거나 반려할 수 있다.
 * 실제 상태 변경 규칙과 마지막 SUPER 보호 같은 업무 검증은
 * EmployeeStatusRequestService에 위임한다.</p>
 */
@Log4j2
@RestController
@RequiredArgsConstructor
public class EmployeeStatusRequestController {
    // 직원의 휴직, 퇴사, 복직 신청 프로세스 관리

    private final EmployeeStatusRequestService statusRequestService;

    @PostMapping("/api/auth/return-from-leave/request")
    public ResponseEntity<EmployeeStatusRequestResponseDTO> createReturnFromLeaveRequest(
            @Valid @RequestBody ReturnFromLeaveRequestDTO request
    ) {
        // 로그인 전 복직 신청
        // 휴직자는 accessToken을 받을 수 없으므로 permitAll 경로로 열어두되,
        // Service에서 사번 + 비밀번호를 다시 검증해 본인만 신청할 수 있게함
        EmployeeStatusRequestResponseDTO response = statusRequestService.createReturnFromLeaveRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/my/status-requests")
    @PreAuthorize("isAuthenticated()")  // 로그인한 직원이 신청을 만듦
    public ResponseEntity<EmployeeStatusRequestResponseDTO> createMyStatusRequest(
            @Valid @RequestBody EmployeeStatusRequestCreateRequestDTO request,
            Authentication authentication
    ) {
        // 본인 휴직/퇴사 신청
        // JWT(Principal)에서 로그인 사용자 직접 추출 (우회 방지)
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        // 로그인 정보 기반으로 안전하게 신청 처리
        EmployeeStatusRequestResponseDTO response = statusRequestService.createRequest(
                principal.getEmployeeNo(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/my/status-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmployeeStatusRequestResponseDTO>> getMyStatusRequests(Authentication authentication) {
        // 내 신청 이력 조회
        // 신청한 결재 승인 확인용 (PENDING/APPROVED/REJECTED)
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(statusRequestService.getMyRequests(principal.getEmployeeNo()));
    }

    @GetMapping("/api/management/status-requests")
    @PreAuthorize("hasRole('SUPER')")
    public ResponseEntity<List<EmployeeStatusRequestResponseDTO>> getPendingStatusRequests(Authentication authentication) {
        // 승인 대기 목록 조회
        // Controller(PreAuthorize) 1차 필터링, Service 내부 Role 재검증으로 2차 방어
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(statusRequestService.getPendingRequests(SystemRole.valueOf(principal.getRole())));
    }

    @PatchMapping("/api/management/status-requests/{requestId}/approve")
    @PreAuthorize("hasRole('SUPER')")
    public ResponseEntity<EmployeeStatusRequestResponseDTO> approveStatusRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody EmployeeStatusRequestDecisionRequestDTO request,
            Authentication authentication
    ) {
        // 결제 처리 (승인)
        // 승인하면 Service에서 신청 유형에 따라 직원 상태를 ON_LEAVE 또는 RESIGNED로 변경
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        EmployeeStatusRequestResponseDTO response = statusRequestService.approveRequest(
                requestId,
                request,
                principal.getEmployeeNo(),
                SystemRole.valueOf(principal.getRole())
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/api/management/status-requests/{requestId}/reject")
    @PreAuthorize("hasRole('SUPER')")
    public ResponseEntity<EmployeeStatusRequestResponseDTO> rejectStatusRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody EmployeeStatusRequestDecisionRequestDTO request,
            Authentication authentication
    ) {
        // 결제 처리 (반려)
        // 반려는 신청 이력만 REJECTED로 바꾸고 직원의 실제 재직 상태는 변경하지 않음
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        EmployeeStatusRequestResponseDTO response = statusRequestService.rejectRequest(
                requestId,
                request,
                principal.getEmployeeNo(),
                SystemRole.valueOf(principal.getRole())
        );
        return ResponseEntity.ok(response);
    }

    // 예외 핸들러
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        // 이미 처리된 요청(PENDING), SUPER가 아닌 사용자의 승인 시도, 마지막 ACTIVE SUPER 보호
        return ResponseEntity.badRequest().body(Map.of(
                "error", "EMPLOYEE_STATUS_REQUEST_FAILED",
                "message", exception.getMessage()
        ));
    }
}
