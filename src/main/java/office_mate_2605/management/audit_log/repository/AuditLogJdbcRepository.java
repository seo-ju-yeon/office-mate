package office_mate_2605.management.audit_log.repository;

import office_mate_2605.management.audit_log.dto.AuditLogEventDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * 감사 로그 이벤트를 audit_log 테이블에 저장하는 JDBC Repository. (작성자: 서주연)
 *
 * <p>Redis에서 꺼낸 AuditLogEventDTO를 감사 로그 전용 DB에 INSERT한다.
 * PostgreSQL enum, inet, timestamptz 타입에 맞게 값을 바인딩하며,
 * 단건 저장과 배치 저장을 모두 제공한다.</p>
 */
@Repository
public class AuditLogJdbcRepository {
    // AuditLogEventDTO를 office_mate_audit_log.audit_log 테이블에 저장하는 JDBC Repository
    // -> Redis에서 넘어온 로그를 audit_log 테이블에 저장 (INSERT 담당)

    // PostgreSQL enum/list/itimestamptz 타입에 맞춰 감사 로그를 insert
    private static final String INSERT_SQL = """
            INSERT INTO audit_log (
                trace_id,
                actor_no,
                actor_role,
                action,
                target_type,
                target_id,
                http_method,
                request_uri,
                client_ip,
                user_agent,
                result,
                reason,
                occurred_at
            ) VALUES (
                ?, ?, ?, CAST(? AS audit_action), ?, ?, ?, ?, CAST(? AS inet), ?,
                CAST(? AS audit_result), ?, to_timestamp(? / 1000.0)
            )
            """;

    // auditJdbcTemplate을 주입받아 office_mate_audit_log DB에 SQL을 실행
    private final JdbcTemplate jdbcTemplate;

    // audit 전용 JdbcTemplate을 명시적으로 주입받아 메인 DB와 저장 위치를 분리
    public AuditLogJdbcRepository(@Qualifier("auditJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 단건 저장이 필요한 경우 사용
    public void save(AuditLogEventDTO event) {
        jdbcTemplate.update(INSERT_SQL, ps -> bind(ps, event));
    }

    // Batch에서 Redis 로그를 여러 건씩 꺼내 저장할 때 사용
    public void saveAll(List<AuditLogEventDTO> events) {
        jdbcTemplate.batchUpdate(INSERT_SQL, events, events.size(), this::bind);
    }

    // SQL의 ? 순서와 audit_log 컬럼 순서가 맞아야 하므로 bind 메서드에서 한 곳에 모아 관리
    private void bind(PreparedStatement ps, AuditLogEventDTO event) throws SQLException {
        // AuditLogEvent 필드를 audit_log 테이블 컬럼 순서에 맞춰 바인딩
        ps.setString(1, event.getTraceId());
        ps.setString(2, event.getActorNo());
        ps.setString(3, event.getActorRole());
        ps.setString(4, event.getAction().name());
        ps.setString(5, event.getTargetType());
        ps.setString(6, event.getTargetId());
        ps.setString(7, event.getHttpMethod());
        ps.setString(8, event.getRequestUri());
        ps.setString(9, event.getClientIp());
        ps.setString(10, event.getUserAgent());
        ps.setString(11, event.getResult().name());
        ps.setString(12, event.getReason());
        ps.setLong(13, event.getOccurredAtEpochMillis());
    }
}
