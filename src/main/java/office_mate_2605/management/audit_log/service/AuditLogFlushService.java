package office_mate_2605.management.audit_log.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.audit_log.dto.AuditLogEventDTO;
import office_mate_2605.management.audit_log.repository.AuditLogJdbcRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Redis 감사 로그 큐를 DB로 이관하는 Service. (작성자: 서주연)
 *
 * <p>AOP와 로그인 감사 로그가 Redis List에 저장한 이벤트를 일정 개수만큼 읽어
 * AuditLogEventDTO로 변환한 뒤 audit_log 테이블에 저장한다.
 * DB 저장이 성공한 범위만 Redis 큐에서 제거해 데이터 유실을 방지한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AuditLogFlushService {

    // AOP가 감사 로그를 쌓아두는 Redis List key
    private static final String AUDIT_LOG_QUEUE_KEY = "audit:log:queue";

    // Redis에서 감사 로그 원본을 읽기 위한 Template
    private final RedisTemplate<String, Object> redisTemplate;

    // Redis에서 꺼낸 Object를 AuditLogEvent로 변환하기 위한 Mapper
    private final ObjectMapper objectMapper;

    // 변환된 감사 로그 이벤트를 audit_log 테이블에 저장하는 Repository
    private final AuditLogJdbcRepository auditLogJdbcRepository;

    public int flush(int batchSize) {
        log.info("--- 감사 로그 DB 이관 시작. batchSize={} ---", batchSize);

        // Redis queue 앞쪽에서 최대 batchSize개까지 읽음
        List<Object> rawLogs = redisTemplate.opsForList()
                .range(AUDIT_LOG_QUEUE_KEY, 0, batchSize - 1);

        // Redis에 쌓인 로그가 없으면 작업을 종료
        if (rawLogs == null || rawLogs.isEmpty()) {
            log.info("--- DB로 이관할 감사 로그가 없습니다. ---");
            return 0;
        }

        // Redis 직렬화 결과를 DB 저장용 AuditLogEvent로 변환
        List<AuditLogEventDTO> events = rawLogs.stream()
                .map(rawLog -> objectMapper.convertValue(rawLog, AuditLogEventDTO.class))
                .toList();

        // DB 저장이 성공해야만 Redis queue에서 제거
        auditLogJdbcRepository.saveAll(events);

        // 저장 완료된 범위만 Redis queue에서 제거
        redisTemplate.opsForList().trim(AUDIT_LOG_QUEUE_KEY, events.size(), -1);

        log.info("--- 감사 로그 DB 이관 완료. count={} ---", events.size());
        return events.size();
    }
}
