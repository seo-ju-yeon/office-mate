package office_mate_2605.management.audit_log.repository;

import office_mate_2605.management.audit_log.domain.AuditAction;
import office_mate_2605.management.audit_log.domain.AuditResult;
import office_mate_2605.management.audit_log.dto.AuditLogResponseDTO;
import office_mate_2605.management.audit_log.dto.AuditLogSearchRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * audit_log 테이블의 조건 검색과 CSV용 조회를 담당하는 JDBC Repository. (작성자: 서주연)
 *
 * <p>감사 로그 전용 DB에서 페이지 목록, 전체 건수, CSV 내보내기용 목록을 조회한다.
 * 검색 조건에 따라 WHERE 절을 동적으로 구성하고, 조회 결과를 AuditLogResponseDTO로 변환한다.</p>
 */
@Repository
public class AuditLogQueryRepository {
    // audit_log 테이블에서 화면에 보여줄 로그 목록 조회 (SELECT 담당)

    // auditJdbcTemplate을 주입받아 office_mate_audit_log DB에서 감사 로그를 조회
    private final JdbcTemplate jdbcTemplate;

    // audit 전용 JdbcTemplate을 사용해 조회도 office_mate_audit_log DB로 보냄
    public AuditLogQueryRepository(@Qualifier("auditJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AuditLogResponseDTO> findAll(AuditLogSearchRequestDTO request) {
        // 화면 목록 조회에 사용할 page, size, offset 값을 계산
        int page = request.normalizedPage();
        int size = request.normalizedSize();
        int offset = page * size;

        // 검색 조건에 따라 WHERE 절과 바인딩 파라미터를 함께 만듦
        List<Object> params = new ArrayList<>();
        String whereClause = buildWhereClause(request, params);

        // audit_log 목록을 최신 발생 시각 순으로 조회
        String sql = """
                SELECT id,
                       trace_id,
                       actor_no,
                       actor_role,
                       action::text AS action,
                       target_type,
                       target_id,
                       http_method,
                       request_uri,
                       client_ip::text AS client_ip,
                       user_agent,
                       result::text AS result,
                       reason,
                       occurred_at,
                       flushed_at
                FROM audit_log
                """ + whereClause + " " + """
                ORDER BY occurred_at DESC
                LIMIT ? OFFSET ?
                """;

        params.add(size);
        params.add(offset);

        // 조회 결과 한 행을 화면 응답 DTO로 변환
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AuditLogResponseDTO(
                rs.getLong("id"),
                rs.getString("trace_id"),
                rs.getString("actor_no"),
                rs.getString("actor_role"),
                AuditAction.valueOf(rs.getString("action")),
                rs.getString("target_type"),
                rs.getString("target_id"),
                rs.getString("http_method"),
                rs.getString("request_uri"),
                rs.getString("client_ip"),
                rs.getString("user_agent"),
                AuditResult.valueOf(rs.getString("result")),
                rs.getString("reason"),
                rs.getObject("occurred_at", OffsetDateTime.class),
                rs.getObject("flushed_at", OffsetDateTime.class)
        ), params.toArray());
    }

    // CSV 다운로드용 감사 로그를 최신 발생 시각 순으로 최대 limit건 조회
    // 화면 필터와 동일한 검색 조건을 적용하기 위해 WHERE 절을 재사용
    public List<AuditLogResponseDTO> findForExport(AuditLogSearchRequestDTO request, int limit) {
        List<Object> params = new ArrayList<>();
        String whereClause = buildWhereClause(request, params);

        // CSV는 페이지 이동이 없으므로 OFFSET 없이 LIMIT만 사용
        String sql = """
                SELECT id,
                       trace_id,
                       actor_no,
                       actor_role,
                       action::text AS action,
                       target_type,
                       target_id,
                       http_method,
                       request_uri,
                       client_ip::text AS client_ip,
                       user_agent,
                       result::text AS result,
                       reason,
                       occurred_at,
                       flushed_at
                FROM audit_log
                """ + whereClause + " " + """
                ORDER BY occurred_at DESC
                LIMIT ?
                """;

        // 과도한 다운로드를 막기 위해 최대 조회 건수를 바인딩
        params.add(limit);

        // 조회 결과를 CSV 생성에 사용할 응답 DTO 목록으로 변환
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AuditLogResponseDTO(
                rs.getLong("id"),
                rs.getString("trace_id"),
                rs.getString("actor_no"),
                rs.getString("actor_role"),
                AuditAction.valueOf(rs.getString("action")),
                rs.getString("target_type"),
                rs.getString("target_id"),
                rs.getString("http_method"),
                rs.getString("request_uri"),
                rs.getString("client_ip"),
                rs.getString("user_agent"),
                AuditResult.valueOf(rs.getString("result")),
                rs.getString("reason"),
                rs.getObject("occurred_at", OffsetDateTime.class),
                rs.getObject("flushed_at", OffsetDateTime.class)
        ), params.toArray());
    }

    // 페이지 계산을 위해 현재 검색 조건에 해당하는 전체 건수를 조회
    public long count(AuditLogSearchRequestDTO request) {
        List<Object> params = new ArrayList<>();
        String whereClause = buildWhereClause(request, params);
        String sql = "SELECT COUNT(*) FROM audit_log " + whereClause;

        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count == null ? 0 : count;
    }

    // 조건을 동적으로 붙이기 위해 기본 WHERE 절을 1=1로 시작
    private String buildWhereClause(AuditLogSearchRequestDTO request, List<Object> params) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");

        // PostgreSQL enum 타입인 audit_action과 비교
        if (request.action() != null) {
            where.append(" AND action = CAST(? AS audit_action)");
            params.add(request.action().name());
        }

        // PostgreSQL enum 타입인 audit_result와 비교
        if (request.result() != null) {
            where.append(" AND result = CAST(? AS audit_result)");
            params.add(request.result().name());
        }

        // 특정 행위자 사번으로 필터링
        if (request.actorNo() != null && !request.actorNo().isBlank()) {
            where.append(" AND actor_no = ?");
            params.add(request.actorNo().trim());
        }

        // 시작일 00:00:00 이후 로그만 조회
        if (request.startDate() != null) {
            where.append(" AND occurred_at >= ?");
            params.add(toTimestamp(request.startDate()));
        }

        // 종료일 전체를 포함하기 위해 다음 날 00:00:00 미만으로 조회
        if (request.endDate() != null) {
            where.append(" AND occurred_at < ?");
            params.add(toTimestamp(request.endDate().plusDays(1)));
        }

        return where.toString();
    }

    // 날짜 필터는 해당 일자의 00:00 기준으로 변환
    private Timestamp toTimestamp(LocalDate date) {
        LocalDateTime dateTime = date.atStartOfDay();
        return Timestamp.valueOf(dateTime);
    }
}
