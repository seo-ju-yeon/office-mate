package office_mate_2605.security.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * refreshToken HttpOnly Cookie의 생성, 조회, 삭제 정책을 관리하는 컴포넌트. (작성자: 서주연)
 *
 * <p>refreshToken을 localStorage에 노출하지 않고 HttpOnly Cookie로만 보관하기 위해
 * Cookie 이름, path, maxAge, secure, sameSite 설정을 한 곳에서 관리한다.
 * 로그인 성공, 토큰 재발급, 로그아웃 흐름에서 공통으로 사용한다.</p>
 */
@Component
public class RefreshTokenCookieProvider {
    // refreshToken Cookie를 생성/조회/삭제하는 공통 컴포넌트
    // 로그인 성공, 토큰 재발급, 로그아웃 흐름에서 같은 Cookie 정책을 사용하기 위해 한 곳에서 관리
    // refreshToken은 장기 토큰이므로 localStorage 대신 HttpOnly Cookie로 보관해 XSS 노출 위험을 낮춤

    // 브라우저에 저장될 Cookie 이름
    // DevTools Application > Cookies에서 이 이름으로 확인 가능
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    // refreshToken Cookie가 전송될 URL 범위
    // 인증 API에서만 필요하므로 /api/auth로 제한해 일반 화면 요청에는 전송하지 않음
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth";

    // Cookie의 브라우저 보관 기간
    // refreshToken JWT 자체의 만료 시간과 Cookie Max-Age를 같은 30일 기준으로 맞춰 관리
    private static final Duration REFRESH_TOKEN_COOKIE_MAX_AGE = Duration.ofDays(30);

    // Secure Cookie 여부
    // 로컬 HTTP 개발에서는 false, 배포 HTTPS 환경에서는 true로 설정해야 함
    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean secure;

    // 응답에 refreshToken 저장 Cookie를 추가하는 메서드
    // 로그인 성공 또는 refreshToken rotation 시 호출
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        // Set-Cookie 헤더를 내려보내면 브라우저가 refreshToken을 Cookie 저장소에 자동 저장
        // 프론트 JavaScript가 refreshToken 값을 직접 저장하지 않는 것이 핵심
        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie(refreshToken).toString());
    }

    // 브라우저의 refreshToken Cookie를 삭제하는 메서드
    // 로그아웃 시 서버 DB revoke와 함께 호출
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        // 같은 name/path를 가진 Cookie를 Max-Age=0으로 내려보내 브라우저 저장소에서 제거
        response.addHeader(HttpHeaders.SET_COOKIE, createDeleteCookie().toString());
    }

    // 요청 Cookie 목록에서 refreshToken 값을 꺼내는 메서드
    // HttpOnly Cookie는 JavaScript에서 읽을 수 없지만 서버에서는 request cookie로 조회 가능
    public String resolveRefreshToken(HttpServletRequest request) {
        // /api/auth/refresh 요청에서는 Path 규칙에 맞는 refreshToken Cookie가 자동 전송됨
        // RefreshTokenFilter는 request body가 아니라 여기서 꺼낸 Cookie 값으로 토큰 검증
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            // Cookie가 하나도 없는 요청이면 refreshToken도 없으므로 null 반환
            return null;
        }

        for (Cookie cookie : cookies) {
            // 여러 Cookie 중 refreshToken 이름을 가진 Cookie만 탐색
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    // 실제 refreshToken 저장용 Cookie를 생성하는 메서드
    // HttpOnly, Secure, SameSite, Path, Max-Age 정책을 한 곳에서 지정
    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true) // JavaScript에서 refreshToken을 읽지 못하게 함
                .secure(secure) // true이면 HTTPS 요청에서만 Cookie 전송
                .sameSite("Lax") // 외부 사이트 요청으로 Cookie가 과도하게 전송되는 것을 완화
                .path(REFRESH_TOKEN_COOKIE_PATH) // 인증 API 요청에만 Cookie 전송
                .maxAge(REFRESH_TOKEN_COOKIE_MAX_AGE) // 브라우저 보관 기간
                .build();
    }

    // refreshToken 삭제용 Cookie를 생성하는 메서드
    // 저장할 때와 같은 name/path로 Max-Age=0을 내려야 기존 Cookie가 제거됨
    private ResponseCookie createDeleteCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true) // 저장 Cookie와 같은 HttpOnly 정책 유지
                .secure(secure) // 저장 Cookie와 같은 Secure 정책 유지
                .sameSite("Lax") // 저장 Cookie와 같은 SameSite 정책 유지
                .path(REFRESH_TOKEN_COOKIE_PATH) // path가 다르면 다른 Cookie로 취급될 수 있음
                .maxAge(Duration.ZERO) // Max-Age=0으로 브라우저 Cookie 삭제
                .build();
    }
}
