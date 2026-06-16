package office_mate_2605.security.filter;

import com.google.gson.Gson;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.security.cookie.RefreshTokenCookieProvider;
import office_mate_2605.security.exception.RefreshTokenException;
import office_mate_2605.security.service.RefreshTokenService;
import office_mate_2605.util.JWTUtil;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * refreshToken으로 새 accessToken을 발급하는 Filter. (작성자: 서주연)
 *
 * <p>/api/auth/refresh 요청에서 HttpOnly Cookie의 refreshToken을 읽어 JWT 서명/만료와
 * DB 저장 상태를 함께 검증한다. 검증에 성공하면 새 accessToken을 JSON으로 내려주고,
 * refreshToken 만료가 임박하면 rotation 후 Cookie를 갱신한다.</p>
 */
@Log4j2
@RequiredArgsConstructor
public class RefreshTokenFilter extends OncePerRequestFilter {
    // access token이 만료되면 사용자가 다시 로그인하지 않고, refresh token으로 새 access token을 받을 수 있음
    // refresh token은 DB의 refresh_token 테이블에 저장된 값과도 비교함
    // -> 로그아웃/비밀번호 변경 시 refresh token을 폐기할 수 있기 때문

    // refreshToken 재발급 요청을 처리할 경로
    // CustomSecurityConfig에서 /api/auth/refresh로 주입
    private final String refreshPath;

    // refreshToken 검증과 새 accessToken/refreshToken 생성을 위해 사용
    private final JWTUtil jwtUtil;

    // refreshToken이 DB에 저장되어 있고 폐기되지 않았는지 검증하기 위해 사용
    private final RefreshTokenService refreshTokenService;

    // HttpOnly Cookie에서 refreshToken을 조회하고 rotation 시 새 Cookie를 내려주기 위해 사용
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    // 새로 발급할 accessToken 유효 시간
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);

    // rotation 시 새로 발급할 refreshToken 유효 시간
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        log.info("--- RefreshTokenFilter.doFilterInternal() 진입: refresh token 재발급 요청인지 확인 ---");

        // HTTP 요청마다 실행되며, refreshPath 요청일 때만 토큰 재발급 흐름 수행

        // 클라이언트가 요청한 URI 경로
        String path = request.getRequestURI();
        log.info("--- [RefreshTokenFilter] path: {} ---", path);

        // 이 필터는 /api/auth/refresh(refreshPath) 요청에서만 동작
        if (!path.equals(refreshPath)) {
            log.info("--- RefreshTokenFilter.doFilterInternal() 종료: refreshPath가 아니므로 다음 필터로 전달 ---");
            filterChain.doFilter(request, response);
            return;
        }

        log.info("--- RefreshTokenFilter.doFilterInternal() 처리 시작: accessToken 재발급 흐름 실행 ---");

        // refreshToken은 더 이상 request body로 받지 않음
        // 브라우저가 /api/auth/refresh 요청에 자동으로 실어 보낸 HttpOnly Cookie에서만 읽음
        String refreshToken = refreshTokenCookieProvider.resolveRefreshToken(request);

        try {
            // refresh token은 정상 형식, 유효한 서명, 만료 전 상태여야 함
            Map<String, Object> refreshMap = checkRefreshToken(refreshToken);

            // refreshToken 안에 사번이 없으면 잘못된 refreshToken으로 간주
            String employeeNo = (String) refreshMap.get("employeeNo");
            if (employeeNo == null || employeeNo.isBlank()) {
                throw new RefreshTokenException(RefreshTokenException.ErrorCase.BAD_REFRESH);
            }

            // DB에 저장되어 있고 revoked_at이 없는 refresh token인지 확인
            // JWT 자체는 서명만 맞으면 서버가 검증할 수 있지만 refreshToken은 로그아웃/폐기 처리가 필요해서 DB에도 저장하고 확인
            if (!refreshTokenService.isValid(employeeNo, refreshToken)) {
                throw new RefreshTokenException(RefreshTokenException.ErrorCase.NOT_FOUND_REFRESH);
            }

            log.info("--- refreshToken 검증 성공: 새 accessToken 발급 ---");

            // 새 access token에 다시 넣을 claim을 구성
            // 이 정보들이 새 JWT payload에 들어감
            Map<String, Object> claims = Map.of(
                    "employeeNo", employeeNo,
                    "role", refreshMap.getOrDefault("role", "USER"),
                    "tempPasswordRequired", refreshMap.getOrDefault("tempPasswordRequired", false)
            );

            // access token은 항상 새로 발급
            String newAccessToken = jwtUtil.generateToken(claims, ACCESS_TOKEN_TTL);

            // refreshToken은 기본적으로 기존 값을 그대로 사용
            String newRefreshToken = refreshToken;

            // refresh token 만료가 2일 이하로 남으면 refresh token도 새로 발급
            // refresh token rotation
            if (shouldRotate(refreshMap)) {
                log.info("--- refreshToken 만료 임박: 새 refreshToken 발급 및 HttpOnly Cookie 갱신 ---");

                newRefreshToken = jwtUtil.generateToken(claims, REFRESH_TOKEN_TTL);
                refreshTokenService.saveRefreshToken(employeeNo, newRefreshToken, Instant.now().plus(REFRESH_TOKEN_TTL));
                // refreshToken rotation이 발생하면 새 refreshToken도 body가 아니라 Cookie로 다시 내려줌
                refreshTokenCookieProvider.addRefreshTokenCookie(response, newRefreshToken);
            }

            sendTokens(newAccessToken, response);
        } catch (RefreshTokenException e) {
            e.sendResponseError(response);
        }
    }

    private boolean shouldRotate(Map<String, Object> refreshMap) {
        log.info("--- RefreshTokenFilter.shouldRotate() 진입: refreshToken 만료 임박 여부 계산 ---");
        // 만료 기한이 얼마 남지 않았는지 판단하는 메서드

        // exp : 만료 시각 (초 단위 Unix timestamp)
        // refreshMap : refreshToken을 검증한 뒤 꺼낸 payload
        Long exp = ((Number) refreshMap.get("exp")).longValue();
        Date expDate = new Date(exp * 1000);  // Java Date는 밀리초 단위라서 1000을 곱함

        // refreshToken 만료까지 얼마나 남았는지 계산
        // 남은 시간 = refreshToken 만료 시간 - 현재 시간
        long diff = expDate.getTime() - System.currentTimeMillis();
        long refreshCondition = (24L * 60 * 60 * 1000) * 2;  // 2일을 밀리초로 계산한 값

        // true  -> refreshToken을 새로 발급해야 함
        // false -> 기존 refreshToken을 그대로 사용
        return diff <= refreshCondition;
    }

    private void sendTokens(String accessToken, HttpServletResponse response) throws IOException {
        log.info("--- RefreshTokenFilter.sendTokens() 진입: 재발급된 accessToken을 JSON 응답으로 전송 ---");
        // 재발급된 accessToken을 JSON으로 보내는 메서드

        // refreshToken은 HttpOnly Cookie로만 관리
        // refresh 응답 body에는 새 accessToken만 포함
        Map<String, String> tokenMap = Map.of("accessToken", accessToken);

        // Java 객체를 JSON 문자열로 변환하기 위한 라이브러리
        Gson gson = new Gson();
        String jsonStr = gson.toJson(tokenMap);

        // 반환 타입 JSON으로 지정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // JSON 응답을 클라이언트에게 전송
        response.getWriter().println(jsonStr);

        log.info("--- New accessToken: {} ---", accessToken);

    }

    private Map<String, Object> checkRefreshToken(String refreshToken)
            throws RefreshTokenException {
        log.info("--- RefreshTokenFilter.checkRefreshToken() 진입: refreshToken 검증, 만료는 차단 ---");

        // refreshToken이 없으면 검증 자체를 할 수 없으므로 바로 예외 처리
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new RefreshTokenException(RefreshTokenException.ErrorCase.NO_REFRESH);
        }

        try {
            // 토큰을 실제로 검증하고, 정상적인 경우 claim 데이터를 반환
            Map<String, Object> claim = jwtUtil.validateToken(refreshToken);
            return claim;
        } catch (MalformedJwtException malformedJwtException) {
            // 403, 토큰 형식 자체가 잘못됨
            log.error("--- MalformedJwtException (refreshToken 형식 잘못) ---", malformedJwtException);
            throw new RefreshTokenException(RefreshTokenException.ErrorCase.OLD_REFRESH);
        } catch (SignatureException signatureException) {
            // 403, 서명이 위조되었거나 서버 비밀키와 맞지 않음
            log.error("--- SignatureException (refreshToken 서명 위조) ---", signatureException);
            throw new RefreshTokenException(RefreshTokenException.ErrorCase.BAD_REFRESH);
        } catch (ExpiredJwtException expiredJwtException) {
            // 403, refreshToken이 만료됨
            log.error("--- ExpiredJwtException (refreshToken 만료) ---", expiredJwtException);
            throw new RefreshTokenException(RefreshTokenException.ErrorCase.BAD_REFRESH);
        }
    }
}
