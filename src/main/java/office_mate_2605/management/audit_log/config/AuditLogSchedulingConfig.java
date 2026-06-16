package office_mate_2605.management.audit_log.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 감사 로그 배치 스케줄링을 활성화하는 설정 클래스. (작성자: 서주연)
 *
 * <p>@EnableScheduling을 통해 AuditLogFlushScheduler의 주기 실행 작업이 동작하도록 한다.</p>
 */
@Configuration
@EnableScheduling
public class AuditLogSchedulingConfig {
    // audit_log 모듈의 주기 실행 작업을 활성화

    // @Scheduled 기반 로그 이관 작업이 실행되도록 스케줄링 기능을 활성화함
    // -> AuditLogFlushScheduler.flushAuditLogs()가 주기적으로 실행됨
}
