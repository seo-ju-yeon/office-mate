package office_mate_2605.security.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;

/**
 * refreshToken 저장, 검증, 폐기, 해시 처리를 담당하는 Service. (작성자: 서주연)
 *
 * <p>로그인 성공 또는 refreshToken rotation 시 기존 활성 토큰을 revoke하고
 * 새 refreshToken의 SHA-256 해시를 DB에 저장한다.
 * 재발급 요청에서는 클라이언트가 보낸 토큰을 해시해 DB의 활성 토큰과 비교한다.</p>
 */
@Log4j2
@Service
public class RefreshTokenService {
    // refresh token을 DB에 저장, 유효한 토큰인지 확인, 폐기

    // refresh_token 테이블은 직접 SQL로 저장/검증/폐기하므로 JdbcTemplate 사용
    // pgVectorJdbcTemplate을 지정해 메인 DB의 refresh_token 테이블에 접근
    private final JdbcTemplate jdbcTemplate;

    public RefreshTokenService(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate) {
        // 메인 DB용 JdbcTemplate을 명시적으로 주입
        // 여러 DataSource가 있으므로 @Qualifier로 pgVectorJdbcTemplate 지정
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(transactionManager = "pgVectorTxManager")
    public void saveRefreshToken(String employeeNo, String refreshToken, Instant expiresAt) {
        log.info("--- RefreshTokenService.saveRefreshToken() 진입 employeeNo={}, expiresAt={} ---", employeeNo, expiresAt);

        // 1. 새 refresh token을 저장하기 전에 기존 활성 토큰을 모두 폐기
        revokeActiveTokens(employeeNo);

        // 2. 실제 refresh token 문자열을 그대로 DB에 저장하지 않고 SHA-256 해시로 저장
        String sql = """
                INSERT INTO refresh_token (employee_no, token_hash, expires_at)
                VALUES (?, ?, ?)
                """;
        jdbcTemplate.update(sql, employeeNo, hash(refreshToken), OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
    }

    public boolean isValid(String employeeNo, String refreshToken) {
        log.info("--- RefreshTokenService.isValid() 진입 employeeNo={} ---", employeeNo);

        // refresh token 유효한지 확인 (클라이언트가 보낸 refresh token을 해시로 바꾼 뒤 DB에 같은 해시가 있는지 확인)

        // revoked_at IS NULL : 로그아웃/재발급으로 폐기되지 않은 토큰
        // expires_at > now() : 아직 만료되지 않은 토큰
        String sql = """
                SELECT COUNT(*)
                FROM refresh_token
                WHERE employee_no = ?
                  AND token_hash = ?
                  AND revoked_at IS NULL
                  AND expires_at > now()
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, employeeNo, hash(refreshToken));
        return count != null && count > 0;  // 조건에 맞는 행이 있으면 true, 없으면 false
    }

    @Transactional(transactionManager = "pgVectorTxManager")
    public void revokeActiveTokens(String employeeNo) {
        log.info("--- RefreshTokenService.revokeActiveTokens() 진입 employeeNo={} ---", employeeNo);

        // 현재 유효한 refresh token을 폐기 (물리적으로 DELETE하지 않고 revoked_at에 폐기 시간을 남김)
        // for 1)언제 로그아웃 했는지, 2)언제 새 로그인으로 기존 토큰이 폐기됐는지, 3)이상한 토큰 재사용 시도가 있는지 확인

        // revoked_at IS NULL : 로그아웃/재발급으로 폐기되지 않은 토큰
        String sql = """
                UPDATE refresh_token
                SET revoked_at = now()
                WHERE employee_no = ?
                  AND revoked_at IS NULL
                """;
        jdbcTemplate.update(sql, employeeNo);
    }

    public String hash(String token) {
        log.info("--- RefreshTokenService.hash() 진입 ---");

        // refresh token 원본을 SHA-256 해시 문자열로 변환
        try {
            // MessageDigest : 문자열을 고정 길이의 해시값으로 변경해주는 Java 표준 클래스
            MessageDigest digest = MessageDigest.getInstance("SHA-256");  // 긴 refresh token -> SHA-256 해시
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));  // 16진수 문자열
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
//            throw new IllegalStateException("refresh_token_hash_failed", e);
            throw new IllegalStateException("인증 토큰 처리 중 오류가 발생했습니다.", e);
        }
    }
}
