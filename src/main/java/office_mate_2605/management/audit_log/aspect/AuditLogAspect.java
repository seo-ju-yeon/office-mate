package office_mate_2605.management.audit_log.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.audit_log.domain.AuditResult;
import office_mate_2605.management.audit_log.dto.AuditLogEventDTO;
import office_mate_2605.management.audit_log.policy.AuditLogPolicy;
import office_mate_2605.management.audit_log.policy.AuditLogPolicyRegistry;
import office_mate_2605.management.audit_log.service.AuditLogRedisService;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Controller 요청을 가로채 감사 로그 이벤트를 생성하는 AOP 컴포넌트. (작성자: 서주연)
 *
 * <p>AuditLogPolicyRegistry에 등록된 API 요청만 감사 대상으로 판단하고,
 * 요청자, 권한, HTTP 정보, 대상 리소스, 성공/실패 결과를 AuditLogEventDTO로 조립한다.
 * 생성된 이벤트는 Redis 임시 큐에 저장해 원래 API 응답 흐름과 감사 로그 DB 저장을 분리한다.</p>
 */
@Log4j2
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    // audit_log.reason 컬럼 길이에 맞춰 실패 사유를 제한
    private static final int REASON_MAX_LENGTH = 500;

    // 생성한 감사 로그 이벤트를 Redis 임시 큐에 저장하는 서비스
    private final AuditLogRedisService auditLogRedisService;
    // 감사 대상 API 정책을 중앙에서 관리하는 Registry
    private final AuditLogPolicyRegistry auditLogPolicyRegistry;

    // Controller 요청을 감싸되, Registry에 등록된 API만 감사 로그로 남김
    @Around("within(office_mate_2605..controller..*)")
    public Object writeAuditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = getCurrentRequest();

        // HTTP 요청이 아닌 실행 흐름은 감사 대상에서 제외
        if (request == null) {
            return joinPoint.proceed();
        }

        // 현재 요청이 중앙 정책 목록에 등록되어 있는지 확인
        AuditLogPolicy policy = auditLogPolicyRegistry
                .find(request.getMethod(), request.getRequestURI())
                .orElse(null);

        // 정책에 없는 API는 원래 메서드만 실행
        if (policy == null) {
            return joinPoint.proceed();
        }

        log.info("감사 로그 AOP 진입. action={}, targetType={}", policy.getAction(), policy.getTargetType());

        // 실제 요청이 발생한 시각을 먼저 기록
        long occurredAt = System.currentTimeMillis();

        try {
            // 원래 Controller/Service 메서드를 실행
            Object result = joinPoint.proceed();

            // 메서드가 정상 종료되면 SUCCESS 감사 이벤트를 만듦
            AuditLogEventDTO event = createEvent(
                    request,
                    policy,
                    AuditResult.SUCCESS,
                    null,
                    occurredAt
            );
            auditLogRedisService.append(event);

            // 원래 메서드의 반환값을 그대로 돌려줌
            return result;
        } catch (Throwable throwable) {
            // 예외가 발생해도 실패 감사 이벤트는 남김
            AuditLogEventDTO event = createEvent(
                    request,
                    policy,
                    AuditResult.FAIL,
                    createFailReason(throwable),
                    occurredAt
            );
            auditLogRedisService.append(event);

            // 감사 로그 기록 후 원래 예외 흐름은 유지함
            throw throwable;
        }
    }

    // 중앙 정책, 요청 객체, SecurityContext에서 audit_log에 필요한 값을 모아 이벤트를 만듦
    private AuditLogEventDTO createEvent(HttpServletRequest request,
                                         AuditLogPolicy policy,
                                         AuditResult result,
                                         String reason,
                                         long occurredAt) {
        // 현재 로그인 사용자 정보를 가져옴
        EmployeePrincipal principal = getCurrentPrincipal();

        // 수집한 값을 Redis에 저장할 감사 로그 이벤트로 조립
        return AuditLogEventDTO.builder()
                .traceId(UUID.randomUUID().toString())
                .actorNo(principal == null ? null : principal.getEmployeeNo())
                .actorRole(principal == null ? null : principal.getRole())
                .action(policy.getAction())
                .targetType(policy.getTargetType())
                .targetId(policy.resolveTargetId(request.getRequestURI()))
                .httpMethod(request.getMethod())
                .requestUri(request.getRequestURI())
                .clientIp(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .result(result)
                .reason(reason)
                .occurredAtEpochMillis(occurredAt)
                .build();
    }

    // 현재 HTTP 요청에서 method, uri, ip, user-agent 등을 추출하기 위해 request를 가져옴
    private HttpServletRequest getCurrentRequest() {
        // RequestContextHolder는 현재 스레드에 연결된 HTTP 요청 정보를 제공
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return attributes == null ? null : attributes.getRequest();
    }

    // TokenCheckFilter가 SecurityContext에 넣어둔 현재 로그인 사용자 정보를 가져옴
    private EmployeePrincipal getCurrentPrincipal() {
        // 현재 요청의 인증 정보를 SecurityContext에서 꺼냄
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 비로그인 요청이거나 프로젝트의 Principal 타입이 아니면 사용자 정보는 비워둠
        if (authentication == null || !(authentication.getPrincipal() instanceof EmployeePrincipal principal)) {
            return null;
        }

        return principal;
    }

    // 프록시나 로드밸런서를 거친 경우 X-Forwarded-For의 첫 번째 IP를 실제 클라이언트 IP로 본다
    private String getClientIp(HttpServletRequest request) {
        // 프록시 환경에서는 X-Forwarded-For에 원본 IP가 담길 수 있음
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    // DB reason 컬럼 길이에 맞춰 예외 정보를 짧게 남김
    private String createFailReason(Throwable throwable) {
        // 예외 클래스명과 메시지만 reason에 저장
        String reason = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();

        // DB 컬럼 길이를 넘지 않도록 잘라냄
        if (reason.length() <= REASON_MAX_LENGTH) {
            return reason;
        }

        return reason.substring(0, REASON_MAX_LENGTH);
    }
}
