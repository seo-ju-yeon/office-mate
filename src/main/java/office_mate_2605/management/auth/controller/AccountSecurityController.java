package office_mate_2605.management.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.auth.dto.AccountSecurityStatusPageResponseDTO;
import office_mate_2605.management.auth.dto.AccountSecurityStatusResponseDTO;
import office_mate_2605.management.auth.dto.AccountSecurityStatusSearchRequestDTO;
import office_mate_2605.management.auth.service.AccountSecurityStatusService;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 관리자 계정 보안 관리 API를 처리하는 Controller. (작성자: 서주연)
 *
 * <p>ADMIN 또는 SUPER 권한 사용자가 직원별 로그인 실패 횟수와 계정 잠금 상태를 조회하고,
 * 로그인 실패 기준 초과로 잠긴 계정을 수동 해제할 수 있도록 요청/응답을 담당한다.</p>
 */
@Log4j2
@RestController
@RequestMapping("/api/management/account-security")
@RequiredArgsConstructor
public class AccountSecurityController {
    // 계정 보안 관리 Controller
    // 로그인 실패 횟수 조회와 계정 잠금 해제 규칙은 AccountSecurityStatusService에서 처리

    private final AccountSecurityStatusService accountSecurityStatusService;  // 로그인 실패 횟수/계정 잠금 관리

    // 직원별 계정 보안 상태 목록을 조회하는 메서드
    // 페이지 정보, 잠긴 계정만 보기, 사번/이름 검색 조건을 쿼리 파라미터로 받음
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<AccountSecurityStatusPageResponseDTO> getAccountSecurityStatuses(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean lockedOnly,
            @RequestParam(required = false) String keyword
    ) {
        // 예: /api/management/account-security?page=0&size=20&lockedOnly=true&keyword=BE001
        log.info("--- 계정 보안 상태 목록 조회 요청 page={}, size={}, lockedOnly={}, keyword={} ---",
                page, size, lockedOnly, keyword);

        // Controller는 요청 파라미터를 검색 조건 DTO로 묶고, 실제 조회 흐름은 Service에 위임
        AccountSecurityStatusSearchRequestDTO request = new AccountSecurityStatusSearchRequestDTO(
                page,
                size,
                lockedOnly,
                keyword
        );
        return ResponseEntity.ok(accountSecurityStatusService.getAccountSecurityStatuses(request));
    }

    // 로그인 실패로 잠긴 직원 계정을 관리자가 수동 해제하는 메서드
    // 현재 로그인한 관리자 사번을 함께 전달해 해제 처리자를 기록
    @PatchMapping("/{employeeNo}/unlock")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<AccountSecurityStatusResponseDTO> unlockAccount(@PathVariable String employeeNo,
                                                                          Authentication authentication
    ) {
        // 로그인 실패 5회 이상으로 잠긴 계정을 관리자가 해제
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(AccountSecurityStatusResponseDTO.from(
                accountSecurityStatusService.unlockAccount(employeeNo, principal.getEmployeeNo())
        ));
    }

    // 계정 보안 관리 API에서 발생한 업무 예외를 400 JSON 응답으로 변환하는 메서드
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        // Service에서 발생한 업무 오류를 400 JSON 응답으로 변환
        return ResponseEntity.badRequest().body(Map.of(
                "error", "ACCOUNT_SECURITY_FAILED",
                "message", exception.getMessage()
        ));
    }
}
