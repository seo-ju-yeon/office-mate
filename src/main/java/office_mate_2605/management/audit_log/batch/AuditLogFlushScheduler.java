package office_mate_2605.management.audit_log.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.audit_log.service.AuditLogFallbackFlushService;
import office_mate_2605.management.audit_log.service.AuditLogFlushService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis에 쌓인 감사 로그를 주기적으로 DB로 이관하는 Scheduler. (작성자: 서주연)
 *
 * <p>@Scheduled 설정에 따라 Redis 큐와 fallback 테이블의 감사 로그를 audit_log 테이블로 옮긴다.
 * 이관 실패 시 원본 큐 또는 fallback row를 유지해 다음 주기에 재시도할 수 있게 한다.</p>
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class AuditLogFlushScheduler {

    // 한 번의 주기 작업에서 DB로 이관할 최대 감사 로그 수
    private static final int BATCH_SIZE = 100;

    // Redis 감사 로그를 audit_log 테이블로 이관하는 실제 작업 서비스
    private final AuditLogFlushService auditLogFlushService;

    // fallback 테이블의 미처리 감사 로그를 audit_log 테이블로 이관하는 서비스
    private final AuditLogFallbackFlushService auditLogFallbackFlushService;

    // 5분마다 Redis 큐와 fallback 테이블의 감사 로그 이관 작업 실행
    @Scheduled(fixedDelayString = "${audit-log.flush.fixed-delay-ms:300000}")
    // 특정 시간을 지정 (밤 11시)
//    @Scheduled(cron = "0 0 23 * * *", zone = "Asia/Seoul")
    public void flushAuditLogs() {
        log.info("--- 감사 로그 주기 이관 작업 시작 ---");

        try {
            // Redis queue에 쌓인 감사 로그를 audit_log 테이블로 이관
            int redisFlushedCount = auditLogFlushService.flush(BATCH_SIZE);

            // Redis 장애 때 fallback 테이블에 저장된 미처리 로그를 audit_log 테이블로 이관
            int fallbackFlushedCount = auditLogFallbackFlushService.flush(BATCH_SIZE);

            log.info("--- 감사 로그 주기 이관 작업 종료. redisFlushedCount={}, fallbackFlushedCount={} ---",
                    redisFlushedCount, fallbackFlushedCount);
        } catch (Exception e) {
            // 이관 실패 시 Redis queue 또는 fallback row를 지우지 않고 다음 주기에 재시도
            log.error("--- 감사 로그 주기 이관 작업 실패. reason={} ---", e.getMessage(), e);
        }
    }
}
