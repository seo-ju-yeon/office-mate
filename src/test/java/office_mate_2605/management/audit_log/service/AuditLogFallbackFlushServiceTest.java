package office_mate_2605.management.audit_log.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import office_mate_2605.management.audit_log.domain.AuditAction;
import office_mate_2605.management.audit_log.domain.AuditResult;
import office_mate_2605.management.audit_log.dto.AuditFallbackLogRecordDTO;
import office_mate_2605.management.audit_log.dto.AuditLogEventDTO;
import office_mate_2605.management.audit_log.repository.AuditFallbackLogJdbcRepository;
import office_mate_2605.management.audit_log.repository.AuditLogJdbcRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogFallbackFlushServiceTest {

    @Test
    void flushMovesFallbackLogsToAuditLogAndMarksProcessed() throws Exception {
        // given: fallback 테이블에 미처리 감사 로그가 1건 있는 상황을 준비
        ObjectMapper objectMapper = new ObjectMapper();
        AuditFallbackLogJdbcRepository fallbackRepository = mock(AuditFallbackLogJdbcRepository.class);
        AuditLogJdbcRepository auditLogJdbcRepository = mock(AuditLogJdbcRepository.class);
        AuditLogFallbackFlushService flushService = new AuditLogFallbackFlushService(
                objectMapper,
                fallbackRepository,
                auditLogJdbcRepository
        );

        AuditLogEventDTO event = createEvent();
        String payloadJson = objectMapper.writeValueAsString(event);
        AuditFallbackLogRecordDTO fallbackLog = new AuditFallbackLogRecordDTO(
                10L,
                payloadJson,
                "Redis 저장 실패: redis down"
        );

        when(fallbackRepository.findUnprocessed(100)).thenReturn(List.of(fallbackLog));

        // when: fallback 미처리 로그를 audit_log로 이관
        int flushedCount = flushService.flush(100);

        // then: 이관된 건수가 반환되어야 함
        assertEquals(1, flushedCount);

        // then: payload JSON이 AuditLogEventDTO로 변환되어 audit_log 저장 Repository로 전달되어야 함
        ArgumentCaptor<List<AuditLogEventDTO>> eventsCaptor = ArgumentCaptor.forClass(List.class);
        verify(auditLogJdbcRepository).saveAll(eventsCaptor.capture());
        assertEquals("trace-fallback-001", eventsCaptor.getValue().get(0).getTraceId());
        assertEquals(AuditAction.READ, eventsCaptor.getValue().get(0).getAction());

        // then: audit_log 저장 성공 후 fallback row의 processed_at 갱신이 요청되어야 함
        verify(fallbackRepository).markProcessed(List.of(10L));
    }

    @Test
    void flushDoesNothingWhenFallbackLogIsEmpty() {
        // given: fallback 테이블에 미처리 로그가 없는 상황을 준비
        ObjectMapper objectMapper = new ObjectMapper();
        AuditFallbackLogJdbcRepository fallbackRepository = mock(AuditFallbackLogJdbcRepository.class);
        AuditLogJdbcRepository auditLogJdbcRepository = mock(AuditLogJdbcRepository.class);
        AuditLogFallbackFlushService flushService = new AuditLogFallbackFlushService(
                objectMapper,
                fallbackRepository,
                auditLogJdbcRepository
        );

        when(fallbackRepository.findUnprocessed(100)).thenReturn(List.of());

        // when: fallback 이관 실행
        int flushedCount = flushService.flush(100);

        // then: 이관할 로그가 없으므로 0건 반환
        assertEquals(0, flushedCount);

        // then: audit_log 저장과 processed_at 갱신은 호출되지 않아야 함
        verify(auditLogJdbcRepository, never()).saveAll(anyList());
        verify(fallbackRepository, never()).markProcessed(anyList());
    }

    private AuditLogEventDTO createEvent() {
        return AuditLogEventDTO.builder()
                .traceId("trace-fallback-001")
                .actorNo("SUPER001")
                .actorRole("SUPER")
                .action(AuditAction.READ)
                .targetType("AUDIT_LOG")
                .targetId("1")
                .httpMethod("GET")
                .requestUri("/api/management/audit-logs")
                .clientIp("127.0.0.1")
                .userAgent("JUnit")
                .result(AuditResult.SUCCESS)
                .reason("fallback 테스트 감사 로그")
                .occurredAtEpochMillis(System.currentTimeMillis())
                .build();
    }
}
