package office_mate_2605.management.audit_log.domain;

/**
 * 감사 대상 요청의 처리 결과를 정의하는 Enum. (작성자: 서주연)
 *
 * <p>API 요청 또는 로그인 처리의 성공/실패 여부를 audit_log.result에 기록할 때 사용한다.</p>
 */
public enum AuditResult {
    SUCCESS, // 요청 처리 성공
    FAIL // 요청 처리 실패
}
