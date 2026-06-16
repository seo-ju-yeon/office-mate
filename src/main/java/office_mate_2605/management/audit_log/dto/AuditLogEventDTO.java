package office_mate_2605.management.audit_log.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import office_mate_2605.management.audit_log.domain.AuditAction;
import office_mate_2605.management.audit_log.domain.AuditResult;

/**
 * 감사 로그 이벤트 데이터를 Redis 큐와 DB 저장 단계 사이에서 전달하는 DTO. (작성자: 서주연)
 *
 * <p>AOP 또는 로그인 핸들러가 수집한 요청자, 행위, 대상, HTTP 정보,
 * 처리 결과, 발생 시각을 담는다. Redis에 임시 저장된 뒤 배치 작업에서 audit_log 테이블로 이관된다.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEventDTO {

    private String traceId;  // API 요청 단위 추적 ID
    private String actorNo;  // 행위를 수행한 직원 사번
    private String actorRole;  // // 요청 당시 사용자 역할

    private AuditAction action;   // 감사 대상 행위
    private String targetType;  // 행위 대상 유형: EMPLOYEE, POST, PROJECT 등
    private String targetId;  // 행위 대상 데이터 ID

    private String httpMethod;  // 요청 HTTP 메서드
    private String requestUri;  // 요청 URI
    private String clientIp;  // 요청 클라이언트 IP
    private String userAgent;  // 요청 브라우저 또는 클라이언트 정보

    private AuditResult result;  // 요청 처리 결과
    private String reason;  // 실패 사유 또는 감사에 필요한 부가 설명

    // Redis 직렬화를 단순하게 유지하고, DB 저장 시 timestamptz로 변환
    private Long occurredAtEpochMillis;  // 실제 요청 발생 시각
}
