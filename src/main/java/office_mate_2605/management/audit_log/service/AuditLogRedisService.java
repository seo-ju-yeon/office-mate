package office_mate_2605.management.audit_log.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.audit_log.dto.AuditLogEventDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 감사 로그 이벤트를 Redis 임시 큐에 저장하는 Service. (작성자: 서주연)
 *
 * <p>AuditLogAspect와 AuditLoginLogService가 생성한 감사 로그 이벤트를 Redis List에 추가한다.
 * 저장 실패가 원래 API 요청 실패로 번지지 않도록 예외를 내부에서 처리한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AuditLogRedisService {

    // Redis key 이름 관리
    private static final String AUDIT_LOG_QUEUE_KEY = "audit:log:queue";

    // AOP가 redisTemplate에 직접 의존하지 않도록 저장 책임 분리
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 저장 실패 시 audit_fallback_log 테이블에 원본 로그를 보관하기 위해 사용
    private final AuditLogFallbackService auditLogFallbackService;

    public void append(AuditLogEventDTO event) {
        // AOP가 만든 감사 로그 이벤트를 Redis 임시 큐에 저장하기 시작
        log.info("--- 감사 로그 Redis 저장 시도. traceId={}, action={}, targetType={} ---",
                event.getTraceId(), event.getAction(), event.getTargetType());

        try {
            // Redis List의 오른쪽 끝에 이벤트를 추가해 Batch/Scheduler가 나중에 순서대로 이관하게 함
            redisTemplate.opsForList().rightPush(AUDIT_LOG_QUEUE_KEY, event);
        } catch (Exception e) {
            // 감사 로그 저장 실패가 원래 API 요청 실패로 번지지 않도록 예외를 내부에서 처리
            log.error("--- 감사 로그 Redis 저장 실패. traceId={}, reason={} ---",
                    event.getTraceId(), e.getMessage(), e);

            // Redis 저장에 실패한 감사 로그는 fallback 테이블에 저장해 유실을 줄임
            auditLogFallbackService.saveFallback(event, "Redis 저장 실패: " + e.getMessage());
        }
    }
}
