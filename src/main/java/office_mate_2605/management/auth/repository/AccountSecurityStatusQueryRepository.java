package office_mate_2605.management.auth.repository;

import office_mate_2605.common.employee.domain.DepartmentCode;
import office_mate_2605.common.employee.domain.PositionCode;
import office_mate_2605.common.employee.domain.SystemRole;
import office_mate_2605.management.auth.dto.AccountSecurityStatusListResponseDTO;
import office_mate_2605.management.auth.dto.AccountSecurityStatusSearchRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 계정 보안 관리 목록의 조건 검색과 페이징 조회를 담당하는 JDBC Repository. (작성자: 서주연)
 *
 * <p>employee 테이블을 기준으로 account_security_status를 LEFT JOIN해
 * 직원 기본 정보와 로그인 실패/잠금 상태를 한 번에 조회한다.
 * 검색 조건에 따라 WHERE 절을 동적으로 구성하고, LIMIT/OFFSET으로 페이지 목록을 조회한다.</p>
 */
@Repository
public class AccountSecurityStatusQueryRepository {
    // 계정 보안 관리 화면의 목록 조회 SQL을 담당

    // pgVectorJdbcTemplate을 주입받아 메인 DB의 employee/account_security_status 테이블에 접근
    private final JdbcTemplate jdbcTemplate;

    public AccountSecurityStatusQueryRepository(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        // 여러 DataSource가 있으므로 @Qualifier로 메인 DB JdbcTemplate 지정
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AccountSecurityStatusListResponseDTO> findAll(AccountSecurityStatusSearchRequestDTO request) {
        // 화면 목록 조회에 사용할 page, size, offset 값을 계산함
        int page = request.normalizedPage();
        int size = request.normalizedSize();
        int offset = page * size;

        // 검색 조건에 따라 WHERE 절과 바인딩 파라미터를 함께 만듦
        List<Object> params = new ArrayList<>();
        String whereClause = buildWhereClause(request, params);

        // 직원 기본 정보와 계정 보안 상태를 함께 조회함
        String sql = """
                SELECT e.employee_no,
                       e.name,
                       e.department::text AS department,
                       e.position::text AS position,
                       e.role::text AS role,
                       COALESCE(ass.login_fail_count, 0) AS login_fail_count,
                       COALESCE(ass.account_locked, false) AS account_locked,
                       ass.last_failed_at,
                       ass.locked_at,
                       ass.unlocked_at,
                       ub.employee_no AS unlocked_by
                FROM employee e
                LEFT JOIN account_security_status ass
                       ON e.employee_no = ass.employee_no
                LEFT JOIN employee ub
                       ON ass.unlocked_by = ub.employee_no
                """ + whereClause + " " + """
                ORDER BY e.employee_no ASC
                LIMIT ? OFFSET ?
                """;

        // 페이징 조회에 사용할 LIMIT/OFFSET 값을 마지막 파라미터로 추가함
        params.add(size);
        params.add(offset);

        // 조회 결과 한 행을 계정 보안 관리 목록 응답 DTO로 변환함
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRow(rs), params.toArray());
    }

    public long count(AccountSecurityStatusSearchRequestDTO request) {
        // 페이지 계산을 위해 현재 검색 조건에 해당하는 전체 건수 조회함
        List<Object> params = new ArrayList<>();
        String whereClause = buildWhereClause(request, params);

        String sql = """
                SELECT COUNT(*)
                FROM employee e
                LEFT JOIN account_security_status ass
                       ON e.employee_no = ass.employee_no
                """ + whereClause;

        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count == null ? 0 : count;
    }

    private String buildWhereClause(AccountSecurityStatusSearchRequestDTO request, List<Object> params) {
        // 논리 삭제되지 않은 직원만 계정 보안 관리 대상으로 조회함
        StringBuilder where = new StringBuilder(" WHERE e.deleted_at IS NULL");

        // 잠긴 계정만 보기 옵션이 켜져 있으면 account_locked=true인 직원만 조회함
        if (request.onlyLocked()) {
            where.append(" AND COALESCE(ass.account_locked, false) = true");
        }

        // 검색어가 있으면 사번 또는 이름에 포함되는 직원만 조회함
        String keyword = request.normalizedKeyword();
        if (keyword != null) {
            where.append(" AND (UPPER(e.employee_no) LIKE ? OR UPPER(e.name) LIKE ?)");
            String likeKeyword = "%" + keyword + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        return where.toString();
    }

    private AccountSecurityStatusListResponseDTO mapRow(ResultSet rs) throws SQLException {
        // enum 코드 문자열을 화면 표시명으로 변환하기 위해 Enum으로 복원함
        DepartmentCode department = DepartmentCode.valueOf(rs.getString("department"));
        PositionCode position = PositionCode.valueOf(rs.getString("position"));
        SystemRole role = SystemRole.valueOf(rs.getString("role"));

        return new AccountSecurityStatusListResponseDTO(
                rs.getString("employee_no"),
                rs.getString("name"),
                department.name(),
                department.getDisplayName(),
                position.name(),
                position.getDisplayName(),
                role.name(),
                role.getDisplayName(),
                rs.getInt("login_fail_count"),
                rs.getBoolean("account_locked"),
                getLocalDateTime(rs, "last_failed_at"),
                getLocalDateTime(rs, "locked_at"),
                getLocalDateTime(rs, "unlocked_at"),
                rs.getString("unlocked_by")
        );
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        // DB timestamptz 값이 null이면 화면 응답도 null로 내려줌
        Timestamp timestamp = rs.getTimestamp(columnName);
        if (timestamp == null) {
            return null;
        }

        // DTO가 LocalDateTime을 사용하므로 Timestamp를 LocalDateTime으로 변환함
        return timestamp.toLocalDateTime();
    }
}
