package office_mate_2605.common.employee.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.common.employee.domain.SystemRole;
import office_mate_2605.common.employee.dto.EmployeeCreateRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeCreateResponseDTO;
import office_mate_2605.common.employee.dto.EmployeeManagementUpdateRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeResignRequestDTO;
import office_mate_2605.common.employee.dto.EmployeeResponseDTO;
import office_mate_2605.common.employee.dto.EmployeeUpdateRequestDTO;
import office_mate_2605.common.employee.service.EmployeeService;
import office_mate_2605.management.auth.dto.AccountSecurityStatusResponseDTO;
import office_mate_2605.management.auth.service.AccountSecurityStatusService;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 직원 관리 API 요청을 처리하는 Controller. (작성자: 서주연)
 *
 * <p>직원 조회, 등록, 수정, 퇴사 처리, 계정 잠금 조회/해제 요청을 담당한다.
 * 실제 업무 규칙은 EmployeeService와 AccountSecurityStatusService에 위임한다.</p>
 */
@Log4j2
@RestController
@RequestMapping("/api/management/employees")
@RequiredArgsConstructor
public class EmployeeController {
    // URL 매핑, 요청 DTO 받기, 인증 사용자 정보 꺼내기, HTTP 응답 코드와 응답 DTO 반환

    private final EmployeeService employeeService;  // 직원 생성/수정/퇴사 같은 실제 업무 규칙
    private final AccountSecurityStatusService accountSecurityStatusService;  // 로그인 실패/계정 잠금 관리 규칙

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<List<EmployeeResponseDTO>> getEmployees() {
        // 직원 목록 조회 API
        // ADMIN, SUPER만 접근 가능
        return ResponseEntity.ok(employeeService.getEmployees());
    }

    @GetMapping("/organization")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmployeeResponseDTO>> getOrganizationEmployees() {
        // 조직도 화면용 직원 목록 API
        // 조직도는 사내 구성원이 서로의 소속을 확인하는 기능이므로 로그인한 직원이면 조회할 수 있게 두되,
        // 비밀번호 같은 민감 정보는 EmployeeResponse에 포함하지 않음
        return ResponseEntity.ok(employeeService.getOrganizationEmployees());
    }

    @GetMapping("/exists/employee-no")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<Map<String, Object>> existsEmployeeNo(@RequestParam String employeeNo) {
        // 직원 등록 화면의 사번 중복확인 API (신규 등록 시 입력한 사번이 사용 가능한지 실시간 체크)
        // @RequestParam은 ?employeeNo=BE002 같은 쿼리스트링 값을 받음
        boolean exists = employeeService.existsEmployeeNo(employeeNo);
        return ResponseEntity.ok(Map.of(
                "employeeNo", employeeNo,
                "exists", exists,
                "available", !exists
        ));
    }

    @GetMapping("/exists/email")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<Map<String, Object>> existsEmail(@RequestParam String email) {
        // 직원 등록 화면의 이메일 중복확인 API (신규 등록 시 입력한 이메일이 사용 가능한지 실시간 체크)
        boolean exists = employeeService.existsEmail(email);
        return ResponseEntity.ok(Map.of(
                "email", email,
                "exists", exists,
                "available", !exists
        ));
    }

    @GetMapping("/{employeeNo}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<EmployeeResponseDTO> getEmployee(@PathVariable String employeeNo) {
        // 직원 단건 조회 API
        // @PathVariable은 URL의 {employeeNo} 값을 메서드 파라미터로 넣어줌
        return ResponseEntity.ok(employeeService.getEmployee(employeeNo));
    }

    /* 신규 직원 등록 */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<EmployeeCreateResponseDTO> createEmployee(@Valid @RequestBody EmployeeCreateRequestDTO request,
                                                                    Authentication authentication
    ) {
        // JWT 검증이 끝난 현재 로그인 사용자를 꺼냄
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        // 서비스에 등록 요청 전달 (누가 등록하는지에 대한 정보 포함)
        EmployeeCreateResponseDTO response = employeeService.createEmployee(
                request,
                principal.getEmployeeNo(),
                SystemRole.valueOf(principal.getRole())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{employeeNo}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<EmployeeResponseDTO> updateEmployee(@PathVariable String employeeNo,
                                                              @Valid @RequestBody EmployeeUpdateRequestDTO request,
                                                              Authentication authentication
    ) {
        // 직원 기본 정보 수정

        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        // 수정 요청자의 역할을 전달하여 서비스 레이어에서 수정 권한 범위를 판단
        EmployeeResponseDTO response = employeeService.updateEmployee(
                employeeNo,
                request,
                SystemRole.valueOf(principal.getRole())
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{employeeNo}/management")
    @PreAuthorize("hasRole('SUPER')")
    public ResponseEntity<EmployeeResponseDTO> updateEmployeeManagementInfo(
            @PathVariable String employeeNo,
            @Valid @RequestBody EmployeeManagementUpdateRequestDTO request,
            Authentication authentication
    ) {
        // 관리 권한/상태 정보 수정
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        EmployeeResponseDTO response = employeeService.updateEmployeeManagementInfo(
                employeeNo,
                request,
                principal.getEmployeeNo(),
                SystemRole.valueOf(principal.getRole())
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{employeeNo}/resign")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<EmployeeResponseDTO> resignEmployee(@PathVariable String employeeNo,
                                                              @Valid @RequestBody EmployeeResignRequestDTO request
    ) {
        // 퇴사 처리는 status를 RESIGNED로 변경
        return ResponseEntity.ok(employeeService.resignEmployee(employeeNo, request));
    }

    @GetMapping("/{employeeNo}/security-status")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<AccountSecurityStatusResponseDTO> getEmployeeSecurityStatus(@PathVariable String employeeNo) {
        // 관리자 화면에서 직원의 로그인 실패 횟수와 계정 잠금 상태를 확인
        // 직원 기본 정보와 보안 상태는 분리해서 응답
        return ResponseEntity.ok(AccountSecurityStatusResponseDTO.from(
                accountSecurityStatusService.getSecurityStatus(employeeNo)
        ));
    }

    @PatchMapping("/{employeeNo}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<AccountSecurityStatusResponseDTO> unlockEmployeeAccount(@PathVariable String employeeNo,
                                                                                  Authentication authentication
    ) {
        // 로그인 실패 5회 이상으로 잠긴 계정을 관리자가 해제
        // 직원 기본 정보와 계정 보안 상태는 책임이 다르므로 별도 DTO로 응답
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(AccountSecurityStatusResponseDTO.from(
                accountSecurityStatusService.unlockAccount(employeeNo, principal.getEmployeeNo())
        ));
    }

    @DeleteMapping("/{employeeNo}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable String employeeNo) {
        // 직원 삭제는 물리 삭제가 아니라 deleted_at을 채우는 논리 삭제
        employeeService.deleteEmployee(employeeNo);
        // 삭제 성공 시 204 No Content 반환 (반환할 데이터가 없으므로)
        return ResponseEntity.noContent().build();
    }

    // 전역 예외 처리기
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        log.error("비즈니스 로직 오류 발생: {}", exception.getMessage());
        // Service에서 발생한 업무 오류를 400 Bad Request JSON으로 변환
        // 예: 중복 사번, 중복 이메일, 권한 없는 역할 부여
        return ResponseEntity.badRequest().body(Map.of(
                "error", "EMPLOYEE_CREATE_FAILED",
                "message", exception.getMessage()
        ));
    }

    /* 현재 로그인한 사용자의 정보를 반환 (웹소켓에서 사용) */
    @GetMapping("/me")
    public ResponseEntity<EmployeeResponseDTO> getMyInfo(@AuthenticationPrincipal EmployeePrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        // Service를 통해 DB에서 최신 정보를 가져오거나,
        // Principal에 담긴 정보만 필요하다면 바로 DTO로 변환해서 반환
        EmployeeResponseDTO myInfo = employeeService.getEmployeeDetail(principal.getEmployeeNo());

        return ResponseEntity.ok(myInfo);
    }

    /* 채팅용: 전체 활성 직원 목록 조회 */
    @GetMapping("/chat")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EmployeeResponseDTO>> getAllEmployeesForChat() {

        return ResponseEntity.ok(
                employeeService.getAllEmployeesForChat()
        );
    }

    // @Valid 검증 실패 시 Spring 기본 영어 메시지 대신 DTO에 정의한 한국어 메시지를 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("입력값을 확인해주세요.");

        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_FAILED",
                "message", message
        ));
    }

}
