package office_mate_2605.management.audit_log.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.audit_log.dto.AuditFallbackLogRecordDTO;
import office_mate_2605.management.audit_log.dto.AuditLogEventDTO;
import office_mate_2605.management.audit_log.repository.AuditFallbackLogJdbcRepository;
import office_mate_2605.management.audit_log.repository.AuditLogJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * fallback 테이블의 미처리 감사 로그를 audit_log 테이블로 이관하는 Service. (작성자: 서주연)
 *
 * <p>audit_fallback_log.processed_at이 NULL인 행을 읽어 payload JSON을 AuditLogEventDTO로 변환하고,
 * audit_log 테이블 저장에 성공한 fallback 행은 processed_at을 채워 재처리 대상에서 제외한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AuditLogFallbackFlushService {
    // fallback 테이블에 쌓인 감사 로그를 audit_log 테이블로 재이관하는 Service

    // fallback payload JSON을 AuditLogEventDTO로 변환하기 위해 사용
    private final ObjectMapper objectMapper;

    // fallback 미처리 로그 조회와 processed_at 갱신을 위해 사용
    private final AuditFallbackLogJdbcRepository auditFallbackLogJdbcRepository;

    // 변환된 감사 로그 이벤트를 audit_log 테이블에 저장하기 위해 사용
    private final AuditLogJdbcRepository auditLogJdbcRepository;

    // fallback 미처리 로그를 audit_log 테이블로 이관하는 메서드
    // audit_log 저장과 processed_at 갱신을 하나의 audit DB 트랜잭션으로 처리
    @Transactional(transactionManager = "auditTxManager")
    public int flush(int batchSize) {
        log.info("--- 감사 로그 fallback 이관 시작. batchSize={} ---", batchSize);

        // processed_at이 없는 fallback 로그를 오래된 순서대로 조회
        List<AuditFallbackLogRecordDTO> fallbackLogs = auditFallbackLogJdbcRepository.findUnprocessed(batchSize);
        if (fallbackLogs.isEmpty()) {
            log.info("--- 이관할 fallback 감사 로그가 없습니다. ---");
            return 0;
        }

        // fallback payload JSON을 audit_log 저장용 DTO로 변환
        List<AuditLogEventDTO> events = fallbackLogs.stream()
                .map(this::toAuditLogEvent)
                .toList();

        // audit_log 저장이 성공해야 fallback 처리 완료 표시를 진행
        auditLogJdbcRepository.saveAll(events);

        // 성공적으로 이관된 fallback row만 processed_at 갱신
        List<Long> processedIds = fallbackLogs.stream()
                .map(AuditFallbackLogRecordDTO::id)
                .toList();
        auditFallbackLogJdbcRepository.markProcessed(processedIds);

        log.info("--- 감사 로그 fallback 이관 완료. count={} ---", processedIds.size());
        return processedIds.size();
    }

    // fallback payload JSON을 AuditLogEventDTO로 변환하는 메서드
    private AuditLogEventDTO toAuditLogEvent(AuditFallbackLogRecordDTO fallbackLog) {
        try {
            return objectMapper.readValue(fallbackLog.payload(), AuditLogEventDTO.class);
        } catch (Exception e) {
            // payload 변환에 실패하면 트랜잭션을 롤백해 processed_at이 채워지지 않게 함
            throw new IllegalStateException("fallback_payload_parse_failed. id=" + fallbackLog.id(), e);
        }
    }
}
