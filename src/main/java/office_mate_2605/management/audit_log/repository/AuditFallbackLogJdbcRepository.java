package office_mate_2605.management.audit_log.repository;

import office_mate_2605.management.audit_log.dto.AuditFallbackLogRecordDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Redis 저장 실패 시 감사 로그 원본을 audit_fallback_log 테이블에 저장하는 JDBC Repository. (작성자: 서주연)
 *
 * <p>Redis 장애나 직렬화 오류로 감사 로그 이벤트를 Redis 큐에 저장하지 못했을 때,
 * JSON payload와 실패 사유를 감사 로그 전용 DB에 임시 보관한다.</p>
 */
@Repository
public class AuditFallbackLogJdbcRepository {
    // Redis에 저장하지 못한 감사 로그 원본을 audit_fallback_log 테이블에 INSERT하는 Repository

    private static final String INSERT_SQL = """
            INSERT INTO audit_fallback_log (
                payload,
                reason
            ) VALUES (
                CAST(? AS jsonb), ?
            )
            """;

    private static final String FIND_UNPROCESSED_SQL = """
            SELECT
                id,
                payload::text AS payload,
                reason
            FROM audit_fallback_log
            WHERE processed_at IS NULL
            ORDER BY id ASC
            LIMIT ?
            """;

    // audit_fallback_log는 감사 로그 전용 DB에 있으므로 auditJdbcTemplate 사용
    private final JdbcTemplate jdbcTemplate;

    // audit 전용 JdbcTemplate을 명시적으로 주입받아 메인 DB와 저장 위치를 분리
    public AuditFallbackLogJdbcRepository(@Qualifier("auditJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // fallback 로그 원본 JSON과 저장 사유를 audit_fallback_log에 저장하는 메서드
    public void save(String payloadJson, String reason) {
        jdbcTemplate.update(INSERT_SQL, payloadJson, reason);
    }

    // 아직 audit_log로 이관되지 않은 fallback 로그를 오래된 순서대로 조회하는 메서드
    public List<AuditFallbackLogRecordDTO> findUnprocessed(int limit) {
        return jdbcTemplate.query(FIND_UNPROCESSED_SQL, (rs, rowNum) -> new AuditFallbackLogRecordDTO(
                rs.getLong("id"),
                rs.getString("payload"),
                rs.getString("reason")
        ), limit);
    }

    // audit_log로 이관 완료된 fallback 로그의 processed_at을 갱신하는 메서드
    public int markProcessed(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        // JdbcTemplate은 IN 절 리스트 바인딩을 직접 지원하지 않으므로 id 개수만큼 placeholder 생성
        String placeholders = ids.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));

        String sql = "UPDATE audit_fallback_log SET processed_at = now() WHERE id IN (" + placeholders + ")";
        return jdbcTemplate.update(sql, ids.toArray());
    }
}
