package office_mate_2605.management.audit_log.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.audit_log.dto.AuditLogEventDTO;
import office_mate_2605.management.audit_log.repository.AuditFallbackLogJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis 저장 실패 감사 로그를 fallback 테이블에 보관하는 Service. (작성자: 서주연)
 *
 * <p>AuditLogEventDTO를 JSON payload로 직렬화한 뒤 audit_fallback_log 테이블에 저장한다.
 * fallback 저장 실패가 원래 API 요청 실패로 번지지 않도록 예외는 내부에서 처리한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AuditLogFallbackService {
    // Redis 저장 실패 시 감사 로그 유실을 막기 위한 fallback 저장 Service

    private static final int REASON_MAX_LENGTH = 300;

    // AuditLogEventDTO를 audit_fallback_log.payload에 저장할 JSON 문자열로 변환하기 위해 사용
    private final ObjectMapper objectMapper;

    // 변환된 JSON payload와 실패 사유를 audit_fallback_log에 저장하기 위해 사용
    private final AuditFallbackLogJdbcRepository auditFallbackLogJdbcRepository;

    // Redis에 저장하지 못한 감사 로그 이벤트를 fallback 테이블에 저장하는 메서드
    // 저장 실패가 원래 API 요청 흐름으로 전파되지 않도록 내부에서 예외 처리
    @Transactional(transactionManager = "auditTxManager")
    public void saveFallback(AuditLogEventDTO event, String reason) {
        try {
            // 감사 로그 이벤트 원본을 jsonb 컬럼에 저장할 JSON 문자열로 변환
            String payloadJson = objectMapper.writeValueAsString(event);

            // reason 컬럼 길이에 맞춰 실패 사유를 제한
            String safeReason = truncateReason(reason);

            // fallback 테이블에 원본 payload와 저장 실패 사유 기록
            auditFallbackLogJdbcRepository.save(payloadJson, safeReason);
            log.warn("--- 감사 로그 fallback 저장 완료. traceId={}, reason={} ---",
                    event.getTraceId(), safeReason);
        } catch (JsonProcessingException e) {
            // JSON 변환 실패 시 fallback 저장 자체가 불가능하므로 로그만 남김
            log.error("--- 감사 로그 fallback payload 변환 실패. traceId={}, reason={} ---",
                    event == null ? null : event.getTraceId(), e.getMessage(), e);
        } catch (Exception e) {
            // fallback DB 저장 실패도 원래 API 요청 실패로 전파하지 않음
            log.error("--- 감사 로그 fallback 저장 실패. traceId={}, reason={} ---",
                    event == null ? null : event.getTraceId(), e.getMessage(), e);
        }
    }

    // audit_fallback_log.reason 컬럼 길이에 맞춰 실패 사유를 자르는 메서드
    private String truncateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "unknown fallback reason";
        }

        if (reason.length() <= REASON_MAX_LENGTH) {
            return reason;
        }

        return reason.substring(0, REASON_MAX_LENGTH);
    }
}
