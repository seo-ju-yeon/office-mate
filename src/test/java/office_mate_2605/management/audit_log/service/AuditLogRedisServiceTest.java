package office_mate_2605.management.audit_log.service;

import office_mate_2605.management.audit_log.domain.AuditAction;
import office_mate_2605.management.audit_log.domain.AuditResult;
import office_mate_2605.management.audit_log.dto.AuditLogEventDTO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogRedisServiceTest {

    private static final String AUDIT_LOG_QUEUE_KEY = "audit:log:queue";

    @Test
    void appendStoresEventInRedisWhenRedisIsAvailable() {
        // given: Redis List 저장이 정상 동작하는 상황을 준비
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ListOperations<String, Object> listOperations = mock(ListOperations.class);
        AuditLogFallbackService fallbackService = mock(AuditLogFallbackService.class);
        AuditLogRedisService redisService = new AuditLogRedisService(redisTemplate, fallbackService);
        AuditLogEventDTO event = createEvent();

        when(redisTemplate.opsForList()).thenReturn(listOperations);

        // when: 감사 로그 이벤트를 Redis 큐에 저장
        redisService.append(event);

        // then: Redis List 오른쪽 끝에 이벤트가 추가되어야 함
        verify(listOperations).rightPush(AUDIT_LOG_QUEUE_KEY, event);

        // then: Redis 저장이 성공했으므로 fallback 저장은 호출되지 않아야 함
        verify(fallbackService, never()).saveFallback(eq(event), contains("Redis 저장 실패"));
    }

    @Test
    void appendStoresEventInFallbackWhenRedisSaveFails() {
        // given: Redis List 저장 중 예외가 발생하는 상황을 준비
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ListOperations<String, Object> listOperations = mock(ListOperations.class);
        AuditLogFallbackService fallbackService = mock(AuditLogFallbackService.class);
        AuditLogRedisService redisService = new AuditLogRedisService(redisTemplate, fallbackService);
        AuditLogEventDTO event = createEvent();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.rightPush(AUDIT_LOG_QUEUE_KEY, event))
                .thenThrow(new RuntimeException("redis down"));

        // when: Redis 저장에 실패하는 감사 로그 이벤트를 append
        redisService.append(event);

        // then: Redis 저장 실패 시 fallback 테이블 저장 서비스가 호출되어야 함
        verify(fallbackService).saveFallback(event, "Redis 저장 실패: redis down");
    }

    private AuditLogEventDTO createEvent() {
        return AuditLogEventDTO.builder()
                .traceId("trace-test-001")
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
                .reason("테스트 감사 로그")
                .occurredAtEpochMillis(System.currentTimeMillis())
                .build();
    }
}
