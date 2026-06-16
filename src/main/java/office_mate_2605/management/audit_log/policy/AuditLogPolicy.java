package office_mate_2605.management.audit_log.policy;

import lombok.Getter;
import office_mate_2605.management.audit_log.domain.AuditAction;
import org.springframework.util.AntPathMatcher;

import java.util.Map;

/**
 * 감사 로그를 남길 API의 매칭 조건과 기록 정책을 표현하는 객체. (작성자: 서주연)
 *
 * <p>HTTP 메서드, URI 패턴, 감사 행위, 대상 유형, 대상 ID 추출 변수를 보관한다.
 * AOP는 현재 요청이 이 정책과 일치하는지 확인한 뒤 감사 로그 이벤트를 생성한다.</p>
 */
@Getter
public class AuditLogPolicy {
    // 어떤 API 요청을 감사 로그로 남길지를 표현하는 정책 객체

    // URI 패턴 비교와 path variable 추출에 사용하는 Spring 경로 맻이 도구
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // 감사 대상으로 등록한 HTTP 메서드 (GET, POST, PUT, PATCH 등등)
    private final String httpMethod;
    // 감사 대상으로 등록할 URI 패턴
    private final String uriPattern;
    // audit_log.action에 저장할 감사 행위
    private final AuditAction action;
    // audit_log.target_type 저장할 감사 대상 유형
    private final String targetType;
    // URI path variable 중 audit_log.target_id로 저장할 변수명
    private final String targetIdPathVariable;

    // 감사 대상 API 한 건의 매칭 조건과 로그 기록 값을 담는 정책 객체
    private AuditLogPolicy(String httpMethod,
                           String uriPattern,
                           AuditAction action,
                           String targetType,
                           String targetIdPathVariable) {
        this.httpMethod = httpMethod;
        this.uriPattern = uriPattern;
        this.action = action;
        this.targetType = targetType;
        this.targetIdPathVariable = targetIdPathVariable;
    }

    // 감사 대상 API 정책을 한 줄로 등록하기 위한 생성 메서드
    public static AuditLogPolicy of(String httpMethod,
                                    String uriPattern,
                                    AuditAction action,
                                    String targetType) {
        return new AuditLogPolicy(httpMethod, uriPattern, action, targetType, "");
    }

    // URI path variable을 targetId로 남겨야 하는 경우 사용
    public static AuditLogPolicy of(String httpMethod,
                                    String uriPattern,
                                    AuditAction action,
                                    String targetType,
                                    String targetIdPathVariable) {
        return new AuditLogPolicy(httpMethod, uriPattern, action, targetType, targetIdPathVariable);
    }

    // 현재 요청이 이 정책의 HTTP method와 URI 패턴에 해당하는지 확인
    public boolean matches(String requestMethod, String requestUri) {
        return httpMethod.equalsIgnoreCase(requestMethod)
                && PATH_MATCHER.match(uriPattern, requestUri);
    }

    // targetIdPathVariable이 지정된 경우 URI에서 해당 값을 뽑아 audit_log.target_id로 사용
    // 예: /api/employees/{employeeNo}에서 employeeNo 값을 targetId로 추출
    public String resolveTargetId(String requestUri) {
        if (targetIdPathVariable == null || targetIdPathVariable.isBlank()) {
            return null;
        }

        Map<String, String> pathVariables =
                PATH_MATCHER.extractUriTemplateVariables(uriPattern, requestUri);

        return pathVariables.get(targetIdPathVariable);
    }
}
