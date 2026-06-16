package office_mate_2605.security.handler;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.auth.domain.AccountSecurityStatus;
import office_mate_2605.management.auth.service.AccountSecurityStatusService;
import office_mate_2605.management.audit_log.service.AuditLoginLogService;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;
import java.util.Map;

/**
 * 로그인 실패 후 JSON 응답과 계정 잠금 처리를 담당하는 Handler. (작성자: 서주연)
 *
 * <p>Spring Security 인증 실패 시 로그인 실패 감사 로그를 남기고,
 * 로그인 실패 횟수를 누적해 기준 횟수 이상이면 계정을 잠금 상태로 전환한다.
 * 클라이언트에는 일반 실패와 계정 잠금을 구분한 JSON 응답을 내려준다.</p>
 */
@Log4j2
@RequiredArgsConstructor
public class APILoginFailureHandler implements AuthenticationFailureHandler {
    // APILoginFailureHandler : 로그인 인증 실패 후 JSON 응답을 담당
    // APILoginFilter -> 아이디/비밀번호를 AuthenticationManager에 넘기고,
    // APIUserDetailsService + PasswordEncoder 검증에 실패하면 이 핸들러가 호출
    // 여기서 로그인 실패 횟수를 기록하고, 계정 잠금 여부에 맞는 응답을 클라이언트에게 내려줌

    // AccountSecurityStatusService는 로그인 실패 횟수 증가와 계정 잠금 상태 확인에 사용
    private final AccountSecurityStatusService accountSecurityStatusService;

    // 로그인 실패 이력을 감사 로그 Redis 큐에 저장할 때 사용
    private final AuditLoginLogService auditLoginLogService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 로그인 실패 시 자동으로 호출되는 메서드
        log.info("--- APILoginFailureHandler.onAuthenticationFailure() 진입 ---");

        // 로그인 실패 사유와 시도 사번을 감사 로그로 남김
        auditLoginLogService.recordFail(request, exception);

        // APILoginFilter에서 request attribute에 저장한 정규화된 사번을 꺼냄
        // request body는 한 번 읽으면 다시 읽기 어려우므로 Filter 단계에서 미리 저장해둔다.
        String employeeNo = (String) request.getAttribute("employeeNo");

        // 한글 메시지가 깨지지 않도록 응답 인코딩을 UTF-8로 명시
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

        // 이미 잠긴 계정은 실패 횟수를 더 증가시키지 않음
        // 잠금 해제는 관리자만 할 수 있으므로 사용자 로그인 시도만으로 상태가 바뀌면 안됨
        if (exception instanceof LockedException) {
            writeAccountLockedResponse(response, null);
            return;
        }

        // 비밀번호 불일치 같은 일반 로그인 실패는 연속 실패 횟수에 반영
        AccountSecurityStatus securityStatus = accountSecurityStatusService.recordLoginFailure(employeeNo);

        // 방금 실패로 잠금 기준에 도달했다면 계정 잠금 응답을 내려줌
        if (accountSecurityStatusService.isAccountLocked(employeeNo)) {
            writeAccountLockedResponse(response, securityStatus);
            return;
        }

        // 아직 잠금 기준에 도달하지 않았다면 일반 로그인 실패 응답을 내려줌
        writeLoginFailedResponse(response, securityStatus);
    }

    private void writeAccountLockedResponse(HttpServletResponse response,
                                            AccountSecurityStatus securityStatus) throws IOException {
        // 계정 잠금 상태는 423 Locked로 응답
        // 화면에서 현재 실패 횟수와 잠금 기준을 안내할 수 있도록 함께 내려줌
        response.setStatus(423);
        response.getWriter().println(new Gson().toJson(Map.of(
                "error", "ACCOUNT_LOCKED",
                "message", "로그인 실패 5회 이상으로 계정이 잠겼습니다. 관리자에게 문의해주세요.",
                "loginFailCount", securityStatus == null
                        ? accountSecurityStatusService.getLoginLockThreshold()
                        : securityStatus.getLoginFailCount(),
                "lockThreshold", accountSecurityStatusService.getLoginLockThreshold()
        )));
    }

    private void writeLoginFailedResponse(HttpServletResponse response,
                                          AccountSecurityStatus securityStatus) throws IOException {
        // 일반 로그인 실패는 401 Unauthorized로 응답
        // Spring Security 내부 예외 메시지를 그대로 보여주지 않고 화면용 문구로 통일
        response.setStatus(401);
        response.getWriter().println(new Gson().toJson(Map.of(
                "error", "LOGIN_FAILED",
                "message", "사번 또는 비밀번호가 올바르지 않습니다.",
                "loginFailCount", securityStatus == null ? 0 : securityStatus.getLoginFailCount(),
                "lockThreshold", accountSecurityStatusService.getLoginLockThreshold()
        )));
    }
}
