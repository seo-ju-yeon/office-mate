package office_mate_2605.management.audit_log.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.audit_log.domain.AuditAction;
import office_mate_2605.management.audit_log.domain.AuditResult;
import office_mate_2605.management.audit_log.dto.AuditLogEventDTO;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 로그인 성공/실패 감사 로그를 생성하는 Service. (작성자: 서주연)
 *
 * <p>로그인은 Controller가 아니라 Security Filter 흐름에서 처리되므로,
 * 일반 AOP 감사 로그와 별도로 로그인 결과를 AuditLogEventDTO로 만들어 Redis 큐에 저장한다.
 * 로그인 실패 시에도 Filter에 남긴 사번을 이용해 시도자를 추적한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AuditLoginLogService {

    // APILoginFilter에서 로그인 시도 사번을 보관할 때 사용하는 key
    private static final String AUDIT_LOGIN_EMPLOYEE_NO = "auditLoginEmployeeNo";

    // audit_log.reason 컬럼 길이에 맞춰 실패 사유를 제한
    private static final int REASON_MAX_LENGTH = 500;

    // 로그인 감사 로그 이벤트를 Redis 임시 큐에 저장하는 서비스
    private final AuditLogRedisService auditLogRedisService;

    // 성공 기록
    public void recordSuccess(HttpServletRequest request, EmployeePrincipal principal) {
        // 로그인 성공 정보를 감사 로그 이벤트로 만들어 Redis에 저장
        appendLoginLog(
                request,
                principal.getEmployeeNo(),
                principal.getRole(),
                AuditResult.SUCCESS,
                null
        );
    }

    // 실패 기록
    public void recordFail(HttpServletRequest request, Exception exception) {
        // 인증 실패 시점에는 Principal이 없으므로 Filter가 보관한 사번을 사용
        String actorNo = getLoginEmployeeNo(request);
        String reason = createFailReason(exception);

        appendLoginLog(
                request,
                actorNo,
                null,
                AuditResult.FAIL,
                reason
        );
    }

    private void appendLoginLog(HttpServletRequest request,
                                String actorNo,
                                String actorRole,
                                AuditResult result,
                                String reason) {
        // 로그인 성공/실패 정보를 감사 로그 이벤트로 만들어 Redis 큐에 저장
        // request와 로그인 결과를 AuditLogEventDTO로 조립한 뒤 Redis 임시 큐에 저장
        try {
            AuditLogEventDTO event = AuditLogEventDTO.builder()
                    .traceId(UUID.randomUUID().toString())
                    .actorNo(actorNo)
                    .actorRole(actorRole)
                    .action(AuditAction.LOGIN)
                    .targetType("AUTH")
                    .targetId(actorNo)
                    .httpMethod(request.getMethod())
                    .requestUri(request.getRequestURI())
                    .clientIp(getClientIp(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .result(result)
                    .reason(reason)
                    .occurredAtEpochMillis(System.currentTimeMillis())
                    .build();

            auditLogRedisService.append(event);
        } catch (Exception e) {
            // 감사 로그 저장 실패가 로그인 응답 실패로 이어지지 않도록 내부에서 처리
            log.error("--- 로그인 감사 로그 저장 실패. actorNo={}, reason={} ---", actorNo, e.getMessage(), e);
        }
    }

    private String getLoginEmployeeNo(HttpServletRequest request) {
        // 로그인 실패 시에도 사번을 남기기 위해 Filter에서 저장한 값을 꺼냄
        Object employeeNo = request.getAttribute(AUDIT_LOGIN_EMPLOYEE_NO);

        return employeeNo == null ? null : String.valueOf(employeeNo);
    }

    private String getClientIp(HttpServletRequest request) {
        // 프록시 환경에서는 X-Forwarded-For의 첫 번째 값을 실제 클라이언트 IP로 본다.
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String createFailReason(Exception exception) {
        // 예외 클래스명과 메시지만 짧게 저장
        String reason = exception.getClass().getSimpleName() + ": " + exception.getMessage();

        if (reason.length() <= REASON_MAX_LENGTH) {
            return reason;
        }

        return reason.substring(0, REASON_MAX_LENGTH);
    }
}
