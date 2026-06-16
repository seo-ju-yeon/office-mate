package office_mate_2605.management.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.auth.dto.AuthMeResponseDTO;
import office_mate_2605.management.auth.dto.AuthResultResponseDTO;
import office_mate_2605.management.auth.dto.PasswordChangeRequestDTO;
import office_mate_2605.management.auth.dto.PasswordResetConfirmRequestDTO;
import office_mate_2605.management.auth.dto.PasswordResetIssueRequestDTO;
import office_mate_2605.management.auth.service.AuthService;
import office_mate_2605.management.auth.service.PasswordResetService;
import office_mate_2605.security.cookie.RefreshTokenCookieProvider;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

/**
 * 로그인 이후 인증 관련 API와 비밀번호 재설정 API를 처리하는 Controller. (작성자: 서주연)
 *
 * <p>로그인 자체는 APILoginFilter에서 처리하고,
 * 이 컨트롤러는 현재 사용자 조회, 로그아웃, 비밀번호 변경,
 * 로그인 전 비밀번호 찾기/재설정 요청을 담당한다.</p>
 */
@Log4j2
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    // 로그인 자체는 APILoginFilter가 처리
    // AuthController는 로그인 이후 필요한 인증 관련 API를 담당

    // /me는 현재 로그인한 직원 정보 확인
    // /logout은 refresh token 폐기
    // /change-password는 임시 비밀번호 1111을 개인 비밀번호로 변경

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    // 로그인 전 비밀번호 재설정 임시 비밀번호 발급을 요청하는 메서드
    // 사번과 이메일이 DB 정보와 일치하면 임시 비밀번호를 메일로 발송
    @PostMapping("/password-reset/request")
    public ResponseEntity<AuthResultResponseDTO> requestPasswordReset(@Valid @RequestBody PasswordResetIssueRequestDTO request) {
        return ResponseEntity.ok(passwordResetService.issueTempPassword(request));
    }

    // 메일로 받은 임시 비밀번호를 검증하고 새 비밀번호로 변경하는 메서드
    // 로그인 전에도 호출되어야 하므로 SecurityConfig에서 permitAll 처리
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<AuthResultResponseDTO> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequestDTO request) {
        return ResponseEntity.ok(passwordResetService.confirmResetPassword(request));
    }

    // 현재 로그인한 직원의 인증 정보를 조회하는 메서드
    // JWT 검증 후 Authentication에 담긴 EmployeePrincipal을 응답 DTO로 변환
    @GetMapping("/me")
    public ResponseEntity<AuthMeResponseDTO> me(Authentication authentication) {
        // Authentication은 Spring Security가 현재 요청의 로그인 정보를 담아 전달하는 객체
        // TokenCheckFilter가 JWT 검증에 성공하면 Authentication 객체에 EmployeePrincipal 저장
        // Controller에서는 DB를 다시 조회하지 않고 현재 로그인 사용자 정보 사용 가능
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(authService.getMe(principal));
    }

    // 현재 로그인한 사용자의 refresh token을 폐기하고 로그아웃 처리하는 메서드
    // DB refresh token 폐기와 브라우저 HttpOnly Cookie 삭제를 함께 수행
    @PostMapping("/logout")
    public ResponseEntity<AuthResultResponseDTO> logout(Authentication authentication,
                                                       HttpServletResponse response) {
        // JWT Access Token은 stateless라서 서버가 직접 삭제할 수 없음
        // 대신 refresh token을 DB에서 폐기해서 이후 재발급 차단
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        AuthResultResponseDTO result = authService.logout(principal);

        // refreshToken은 HttpOnly Cookie에 있으므로 로그아웃 시 브라우저 Cookie도 함께 삭제
        // DB revoke가 최종 방어선이지만 Cookie를 지워야 브라우저에도 로그아웃 상태 즉시 반영
        refreshTokenCookieProvider.deleteRefreshTokenCookie(response);

        return ResponseEntity.ok(result);
    }

    // 임시 비밀번호 상태의 사용자가 새 비밀번호로 변경하는 메서드
    // 최초 로그인 후 강제 비밀번호 변경 흐름에서 예외적으로 허용되는 API
    @PostMapping("/change-password")
    public ResponseEntity<AuthResultResponseDTO> changePassword(Authentication authentication,
                                                                @RequestBody PasswordChangeRequestDTO request) {
        // 임시 비밀번호 상태인 직원은 TokenCheckFilter에서 대부분의 API 접근 차단
        // 이 API는 예외로 허용되어 최초 로그인 후 비밀번호 변경 완료 가능
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(authService.changePassword(principal, request));
    }

    // @Valid 검증 실패 시 Spring 기본 메시지 대신 필드별 사용자 안내 문구를 반환하는 메서드
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

    // 인증 관련 API에서 발생한 업무 예외를 400 JSON 응답으로 변환하는 메서드
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> handleBadRequest(RuntimeException exception) {
        // Service에서 발생한 입력 오류를 JSON 응답으로 변환
        // 예: 현재 비밀번호 불일치, 새 비밀번호 길이 부족
        return ResponseEntity.badRequest().body(Map.of(
                "error", "AUTH_FAILED",
                "message", exception.getMessage()
        ));
    }
}
