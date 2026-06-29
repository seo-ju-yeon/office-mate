package office_mate_2605.util;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT accessToken과 refreshToken 생성 및 검증을 담당하는 공통 Util. (작성자: 서주연)
 *
 * <p>로그인 성공, 토큰 재발급 과정에서 사용할 JWT 문자열을 생성하고,
 * API 요청으로 전달된 JWT의 서명, 형식, 만료 시간을 검증한 뒤 payload claim을 반환한다.</p>
 *
 * <p>토큰에는 사번, 이름, 부서, 직급, 시스템 권한, 임시 비밀번호 변경 필요 여부 같은
 * 인증/인가 판단에 필요한 값을 claim으로 담는다.</p>
 */
@Log4j2
@Component
public class JWTUtil {
    // JWT 문자열을 만들고(generateToken), 받은 JWT가 정상인지 검사(validateToken)하는 공통 도구 클래스

    // JWT 서명 생성과 검증에 사용할 비밀키
    // application.properties의 my.jwt.secret 값 주입
    @Value("${my.jwt.secret}")
    private String key;

    // 일 단위 유효기간으로 JWT를 생성하는 메서드
    // 내부적으로 Duration 기반 생성 메서드에 위임
    public String generateToken(Map<String, Object> valueMap, int days) {
        return generateToken(valueMap, Duration.ofDays(days));
    }

    // 지정한 Duration만큼 유효한 JWT 문자열을 생성하는 메서드
    // accessToken은 짧게, refreshToken은 길게 발급하기 위해 Duration 기반으로 처리
    public String generateToken(Map<String, Object> valueMap, Duration duration) {
        log.info("--- JWTUtil.generateToken() 진입 duration={}, claimKeys={} ---", duration, valueMap.keySet());

//        log.info("generateKey: {}", key);
        log.info("JWT 생성 완료 duration={}, claimKeys={}", duration, valueMap.keySet());

        // JWT header 영역
        Map<String, Object> headers = new HashMap<>();
        headers.put("typ", "JWT");  // 토큰 타입
        headers.put("alg", "HS256");  // 서명 알고리즘

        // JWT payload 영역
        Map<String, Object> payloads = new HashMap<>();
        payloads.putAll(valueMap);  // employeeNo, role, tempPasswordRequired 같은 인증에 필요한 정보를 담음

        String jwtStr = Jwts.builder()
                .setHeader(headers)
                .setClaims(payloads)
                // 토큰 발급 시각 설정
                .setIssuedAt(Date.from(ZonedDateTime.now().toInstant()))
                // 토큰 만료 시각 설정
                .setExpiration(Date.from(ZonedDateTime.now().plus(duration).toInstant()))
                // 비밀키로 서명해 위조 여부를 검증할 수 있게 처리
                .signWith(SignatureAlgorithm.HS256, key.getBytes())  // header 알고리즘과 동일한 알고리즘 선언
                .compact();

        return jwtStr;
    }

    // 전달받은 JWT를 검증하고 payload claim을 반환하는 메서드
    // 서명, 형식, 만료 시간이 유효하지 않으면 JwtException 발생
    public Map<String, Object> validateToken(String token) throws JwtException {
        log.info("--- JWTUtil.validateToken() 진입 ---");

        // 문자열 비밀키를 JWT 검증용 SecretKey 객체로 변환
        byte[] keyBytes = key.getBytes();
        SecretKey secretKey = Keys.hmacShaKeyFor(keyBytes);  // JWT 검증용 키 객체로 변환

        // JWT 문자열을 파싱하면서 서명과 만료 시간을 함께 검증
        Map<String, Object> claim = Jwts.parser()  // parser : JWT 문자열을 읽고 검증하는 객체
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)  // 파싱 및 검증, 실패시 에러 (서명과 만료 검사가 함께 수행)
                .getPayload();

        return claim;
    }
}
