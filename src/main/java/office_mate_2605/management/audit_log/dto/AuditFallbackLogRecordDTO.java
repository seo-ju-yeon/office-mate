package office_mate_2605.management.audit_log.dto;

/**
 * audit_fallback_log 미처리 행을 이관 작업에 전달하는 DTO. (작성자: 서주연)
 *
 * <p>Redis 저장 실패로 fallback 테이블에 보관된 감사 로그 원본 JSON과
 * fallback 저장 사유를 audit_log 재이관 단계로 전달한다.</p>
 */
public record AuditFallbackLogRecordDTO(
        Long id, // fallback 로그 PK
        String payload, // AuditLogEventDTO 원본 JSON
        String reason // fallback 저장 사유
) {
}
