package office_mate_2605.security.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.security.APIUserDetailsService;
import office_mate_2605.security.dto.EmployeePrincipal;
import office_mate_2605.security.exception.AccessTokenException;
import office_mate_2605.util.JWTUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * API 요청의 accessToken을 검증하고 SecurityContext를 구성하는 Filter. (작성자: 서주연)
 *
 * <p>/api/** 요청에서 Authorization Bearer accessToken을 검증하고,
 * 토큰의 사번으로 DB의 최신 직원 정보를 다시 조회해 인증 객체를 생성한다.
 * 휴직/퇴사, 계정 잠금, 임시 비밀번호 상태는 허용된 API를 제외하고 차단한다.</p>
 */
@Log4j2
@RequiredArgsConstructor
public class TokenCheckFilter extends OncePerRequestFilter {
    // 로그인 이후 API 요청마다 accessToken 검사를 수행하는 필터
    // 클라이언트는 API 요청마다 'Authorization: Bearer <accessToken>' 헤더를 보냄

    // accessToken을 검증하고 정상이라면 SecurityContext에 인증 정보를 넣는 역할
    // 이후 Controller에서는 Authentication 객체로 현재 사용자를 알 수 있음

    // 토큰의 사번으로 DB의 최신 직원 정보를 다시 조회하기 위해 사용
    // JWT claim만 믿지 않고 권한/재직상태/잠금상태 변경을 즉시 반영하기 위함
    private final APIUserDetailsService apiUserDetailsService;

    // Authorization 헤더의 accessToken 서명, 형식, 만료 시간을 검증하기 위해 사용
    private final JWTUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 클라이언트가 요청한 URI 경로
        String path = request.getRequestURI();
        log.info("--- [TokenCheckFilter] path: {} ---", path);

        // /api/가 아닌 화면 요청은 JWT 검사 대상이 아님
        // 로그인과 재발급 API도 토큰 없이 호출되어야 하므로 검사에서 제외
        // 구글 연동 API도 토큰 없이 호출되어야 하므로 검사에서 제외
        if (!path.startsWith("/api/")
                || path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/refresh")
                || path.startsWith("/api/auth/password-reset/")
                || path.startsWith("/api/auth/return-from-leave/request")
                || path.startsWith("/api/calendar/callback")
                || path.equals("/api/chat/room")) {
            filterChain.doFilter(request, response);
            log.info("--- JWT 검사 대상 아님 ---");
            return;
        }

        log.info("--- Token Check Filter ---");
        log.info("JWTUtil: {}", jwtUtil);

        try {
            // 1. Authorization 헤더에서 access token을 꺼내고 검증
            Map<String, Object> payload = validateAccessToken(request);

            // 2. 토큰 payload에서 사번을 꺼냄
            String employeeNo = (String) payload.get("employeeNo");

            // 사번이 없으면 비정상 토큰으로 판단
            if (employeeNo == null || employeeNo.isBlank()) {
                throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.MALFORM);
            }

            // 3. 토큰에서 꺼낸 사번으로 DB에서 직원을 다시 조회
            // JWT는 발급 시점 정보라 최신 상태가 아닐 수 있으므로 퇴사/삭제/권한 변경을 DB 기준으로 반영
            UserDetails userDetails = apiUserDetailsService.loadUserByUsername(employeeNo);

            // 4. 휴직/퇴사 등 ACTIVE가 아닌 계정은 기존 accessToken으로 업무 API를 계속 쓰지 못하게 차단
            if (isInactiveAccountBlockedPath(path, userDetails)) {
                throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.ACCOUNT_INACTIVE);
            }

            // 5. 계정 잠금 상태라면 기존 accessToken으로도 업무 API를 계속 쓰지 못하게 차단
            if (isLockedAccountBlockedPath(path, userDetails)) {
                throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.ACCOUNT_LOCKED);
            }

            // 6. 임시 비밀번호 상태라면 비밀번호 변경 관련 API만 허용
            if (isTempPasswordBlockedPath(path, userDetails)) {
                throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.TEMP_PASSWORD_REQUIRED);
            }

            // 7. Spring Security가 이해할 수 있는 인증 완료 객체 생성
            // 로그인 시점의 인증 요청 객체가 아니라 이미 인증된 사용자 표시용
            UsernamePasswordAuthenticationToken authenticationToken =
                    // 두 번째 값이 null인 이유는 accessToken 검증이 끝나 비밀번호가 필요 없기 때문
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 8. 현재 요청의 인증 정보를 SecurityContext에 저장
            // 이 필터가 SecurityContext에 인증 정보를 넣어야 이후 Controller가 현재 로그인 사용자를 알 수 있음
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (AccessTokenException e) {
            e.sendResponseError(response);
        }
    }

    // 휴직/퇴사 등 ACTIVE가 아닌 계정은 기존 토큰으로 업무 API를 계속 호출하지 못하게 차단
    private boolean isInactiveAccountBlockedPath(String path, UserDetails userDetails) {
        log.info("--- TokenCheckFilter.isInactiveAccountBlockedPath() 진입 path={} ---", path);

        // 1. 현재 프로젝트의 로그인 주체가 아니라면 status를 알 수 없으므로 별도 차단하지 않음
        if (!(userDetails instanceof EmployeePrincipal principal)) {
            return false;
        }

        // 2. ACTIVE 계정은 정상 활동 가능
        if ("ACTIVE".equals(principal.getStatus())) {
            return false;
        }

        // 3. logout은 예외로 허용
        // 이미 휴직/퇴사 처리된 사용자가 로그아웃해도 refresh token 폐기와 브라우저 상태 정리는 필요
        return !path.startsWith("/api/auth/logout");
    }

    // 잠긴 계정은 기존 accessToken으로 업무 API를 계속 호출하지 못하게 차단
    private boolean isLockedAccountBlockedPath(String path, UserDetails userDetails) {
        log.info("--- TokenCheckFilter.isLockedAccountBlockedPath() 진입 path={} ---", path);

        // 1. 현재 프로젝트의 로그인 주체가 아니라면 잠금 상태를 알 수 없으므로 별도 차단하지 않음
        if (!(userDetails instanceof EmployeePrincipal principal)) {
            return false;
        }

        // 2. 잠기지 않은 계정은 정상 활동 가능
        if (!principal.isAccountLocked()) {
            return false;
        }

        // 3. logout은 예외로 허용
        // 잠긴 계정이라도 refresh token 폐기와 브라우저 상태 정리는 가능해야 함
        return !path.startsWith("/api/auth/logout");
    }

    // 임시 비밀번호 상태라면 비밀번호 변경 관련 API만 허용
    private boolean isTempPasswordBlockedPath(String path, UserDetails userDetails) {
        log.info("--- TokenCheckFilter.isTempPasswordBlockedPath() 진입 path={} ---", path);

        // 1. UserDetails 안의 실제 객체가 EmployeePrincipal이 아니면 임시 비밀번호 여부를 알 수 없으므로 통과
        if (!(userDetails instanceof EmployeePrincipal)) {
            return false;
        }

        // 2. tempPasswordRequired 값을 확인하기 위해 EmployeePrincipal로 형변환
        EmployeePrincipal principal = (EmployeePrincipal) userDetails;

        // 3. 임시 비밀번호 상태가 아니라면 API 접근을 막을 필요가 없으므로 검사 통과
        if (!principal.isTempPasswordRequired()) {
            return false;
        }

        // 4. 임시 비밀번호 상태여도 아래 API는 허용
        boolean allowedPath = path.startsWith("/api/auth/me")
                || path.startsWith("/api/auth/logout")
                || path.startsWith("/api/auth/change-password");

        // 5. 허용 경로라면 막지 않음
        if (allowedPath) {
            return false;
        }

        // 6. 임시 비밀번호 상태인데 허용 경로가 아니라면 접근을 막음
        return true;
    }

    private Map<String, Object> validateAccessToken(HttpServletRequest request) {
        // 요청 헤더에서 accessToken을 꺼내고 정상 토큰인지 검사한 뒤 payload를 반환하는 메서드

        log.info("--- TokenCheckFilter.validateAccessToken() 진입 ---");

        // 1. 토큰 추출 (Authorization 헤더가 없거나, Bearer 뒤에 토큰이 없으면 인증 실패 처리)
        String headerStr = request.getHeader("Authorization");  // 클라이언트가 토큰을 보내지 않았다면 null

        // Authorization 헤더가 없거나 길이가 8자 미만('Bearer ')인 경우 예외 발생
        if (headerStr == null || headerStr.length() < 8) {
            // Bearer 형식 -> 6글자 + 공백 1 + 토큰 최소 1글자, 즉 8글자 이상
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.UNACCEPT);
        }

        // substring(0, 6) => "Bearer"
        String tokenType = headerStr.substring(0, 6);  // Authorization 헤더의 첫 6글자
        // substring(7) => 실제 JWT 문자열
        String tokenStr = headerStr.substring(7);  // Bearer 생략해서 실제 토큰 문자열 추출

        // 2. 토큰 타입 검사
        // Bearer가 소문자로 들어올 수 있기 때문에 equalsIgnoreCase 대소문자 구분하지 않는 것을 사용
        if (!tokenType.equalsIgnoreCase("Bearer")) {
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.BADTYPE);
        }

        // 3. 토큰 검증
        try {
            // 토큰을 실제로 검증하고 서명, 만료 시간, 토큰 형식이 정상이면 claim 데이터 반환
            Map<String, Object> claim = jwtUtil.validateToken(tokenStr);
            return claim;
        } catch (MalformedJwtException e) {
            // 403, 토큰 형식 자체가 잘못됨
            log.error("Malformed access token", e);
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.MALFORM);
        } catch (SignatureException e) {
            // 403, 서명이 위조됨
            log.error("Invalid access token signature", e);
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.BADSIGN);
        } catch (ExpiredJwtException e) {
            // 403, 토큰이 만료됨
            log.error("Expired access token", e);
            throw new AccessTokenException(AccessTokenException.TOKEN_ERROR.EXPIRED);
        }
    }
}
