package office_mate_2605.security.handler;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.audit_log.service.AuditLoginLogService;
import office_mate_2605.management.auth.service.AccountSecurityStatusService;
import office_mate_2605.security.cookie.RefreshTokenCookieProvider;
import office_mate_2605.security.dto.EmployeePrincipal;
import office_mate_2605.security.service.RefreshTokenService;
import office_mate_2605.util.JWTUtil;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 로그인 성공 후 JWT 발급과 refreshToken 저장을 담당하는 Handler. (작성자: 서주연)
 *
 * <p>인증 성공 시 로그인 실패 횟수를 초기화하고, 휴직/퇴사 계정은 토큰 발급을 차단한다.
 * 정상 계정에는 accessToken을 JSON body로, refreshToken을 HttpOnly Cookie로 발급하며,
 * refreshToken 해시는 DB에 저장하고 로그인 성공 감사 로그를 남긴다.</p>
 */
@Log4j2
@RequiredArgsConstructor
public class APILoginSuccessHandler implements AuthenticationSuccessHandler {
    // APILoginSuccessHandler : 로그인 인증이 성공 후 JWT를 발급
    // APILoginFilter -> 아이디/비밀번호를 AuthenticationManager에 넘기고,
    // APIUserDetailsService + PasswordEncoder 검증이 끝나면 이 핸들러가 호출
    // 여기서 클라이언트에게 access token과 refresh token을 발급

    // JWTUtil은 access token과 refresh token을 실제 문자열로 만들어주는 도구
    private final JWTUtil jwtUtil;

    // RefreshTokenService는 refresh token을 DB에 저장하거나 폐기할 때 사용
    private final RefreshTokenService refreshTokenService;

    // 로그인 성공 시 연속 실패 횟수를 초기화할 때 사용
    private final AccountSecurityStatusService accountSecurityStatusService;

    // refreshToken을 응답 body가 아니라 HttpOnly Cookie로 내려줄 때 사용
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    // 로그인 성공 이력을 감사 로그 Redis 큐에 저장할 때 사용
    private final AuditLoginLogService auditLoginLogService;

    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(30);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        // 로그인 성공 시 자동으로 호출되는 메서드
        log.info("--- Login Success Handler ---");

        // 1. 토큰 생성
        log.info(authentication);  // 인증이 성공하면 authentication 안에 현재 로그인한 사용자 정보가 들어 있음

        // UserDetails를 EmployeePrincipal로 만들었기 때문에 형변환해서 사번/이름/권한을 꺼냄
        EmployeePrincipal principal = (EmployeePrincipal) authentication.getPrincipal();
        log.info("API login success. employeeNo={}", principal.getEmployeeNo());

        // 인증에 성공한 계정은 연속 로그인 실패 횟수를 초기화
        // 잠긴 계정은 EmployeePrincipal의 accountNonLocked=false로 성공 핸들러까지 오지 못함
        accountSecurityStatusService.resetLoginFailures(principal.getEmployeeNo());

        // 비밀번호 검증은 성공했지만 휴직/퇴사 상태인 계정에는 JWT를 발급하지 않음
        // 휴직 계정 -> 로그인 화면에서 복직 신청 UI를 띄울 수 있도록 ACCOUNT_ON_LEAVE를 내려줌
        if ("ON_LEAVE".equals(principal.getStatus())) {
            writeInactiveLoginResponse(response, 423, Map.of(
                    "error", "ACCOUNT_ON_LEAVE",
                    "message", "휴직 상태의 계정입니다. 복직 신청을 진행해주세요.",
                    "employeeNo", principal.getEmployeeNo()
            ));
            return;
        }
        // 퇴사 계정 -> 복직 대상이 아니므로 계정 상태 안내만 내려주고 업무 영역 접근은 차단
        if ("RESIGNED".equals(principal.getStatus())) {
            writeInactiveLoginResponse(response, 423, Map.of(
                    "error", "ACCOUNT_RESIGNED",
                    "message", "퇴사 처리된 계정은 로그인할 수 없습니다."
            ));
            return;
        }

        // 실제 업무 접근용 토큰 발급이 가능한 로그인만 성공 감사 로그로 남김
        auditLoginLogService.recordSuccess(request, principal);

        // claims = JWT payload에 들어갈 데이터
        // 너무 많은 개인정보를 넣지 않고, API 인증에 필요한 최소 정보만 넣음
        Map<String, Object> claims = Map.of(
                "employeeNo", principal.getEmployeeNo(),
                "role", principal.getRole(),
                "tempPasswordRequired", principal.isTempPasswordRequired()
        );

        // 1) access token: API 요청마다 Authorization 헤더에 담는 짧은 수명의 토큰
        String accessToken = jwtUtil.generateToken(claims, ACCESS_TOKEN_TTL);
        // 2) refresh token: access token 재발급에 사용하는 긴 수명의 토큰
        String refreshToken = jwtUtil.generateToken(claims, REFRESH_TOKEN_TTL);

        // refresh token은 로그아웃/폐기 처리를 위해 DB에도 저장
        // 원본 문자열이 아니라 RefreshTokenService 내부에서 해시값으로 저장된다.
        refreshTokenService.saveRefreshToken(
                principal.getEmployeeNo(),
                refreshToken,
                Instant.now().plus(REFRESH_TOKEN_TTL)
        );

        // refreshToken은 JavaScript가 읽을 수 있는 JSON body에 포함하지 않음
        // HttpOnly Cookie로 관리
        refreshTokenCookieProvider.addRefreshTokenCookie(response, refreshToken);

        // 2. 응답 처리
        // 응답에 포함할 데이터를 담은 Map 생성
        // accessToken은 REST API Authorization 헤더에 붙여야 하므로 body로 내려줌
        Map<String, Object> responseMap = Map.of(
                "employeeNo", principal.getEmployeeNo(),
                "name", principal.getName(),
                "role", principal.getRole(),
                "tempPasswordRequired", principal.isTempPasswordRequired(),
                "accessToken", accessToken
        );

        Gson gson = new Gson();  // Java 객체(Map)를 JSON 문자열로 변환하기 위한 라이브러리
        String jsonStr = gson.toJson(responseMap);  // jsonStr은 클라이언트에게 보낼 JSON 문자열

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);  // 반환되는 타입을 JSON으로 지정

        response.getWriter().println(jsonStr);  // JSON 응답을 클라이언트에게 전송
    }

    // 비활성 계정 로그인 차단 응답을 JSON으로 작성하는 메서드
    // Controller를 거치지 않는 Security Handler 내부에서 상태 코드와 응답 본문을 직접 설정
    private void writeInactiveLoginResponse(HttpServletResponse response,
                                            int status,
                                            Map<String, Object> responseMap) throws IOException {
        // 응답 Map을 JSON 문자열로 변환하기 위한 Gson 생성
        Gson gson = new Gson();

        // HTTP 상태 코드와 JSON 응답 인코딩 설정
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

        // responseMap을 JSON으로 변환해 응답 body에 작성
        response.getWriter().println(gson.toJson(responseMap));
    }
}
