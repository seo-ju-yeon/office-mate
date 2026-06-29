package office_mate_2605.security.filter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;

/**
 * JSON 로그인 요청을 Spring Security 인증 흐름으로 전달하는 Filter. (작성자: 서주연)
 *
 * <p>/api/auth/login 요청의 JSON body에서 사번과 비밀번호를 읽고,
 * 사번을 대문자로 정규화한 뒤 AuthenticationManager에 인증 요청을 위임한다.
 * 실패 횟수 기록과 로그인 감사 로그를 위해 정규화된 사번을 request attribute에 보관한다.</p>
 */
@Log4j2
public class APILoginFilter extends AbstractAuthenticationProcessingFilter {
    // APILoginFilter: 로그인 요청만 담당함
    // Controller가 아니라 Filter에서 처리하는 이유,
    // Spring Security의 인증 흐름(AuthenticationManager, UserDetailsService, PasswordEncoder)을 그대로 사용하기 위함

    public APILoginFilter(String defaultFilterProcessesUrl) {
        // defaultFilterProcessesUrl은 이 필터가 반응할 URL
        // CustomSecurityConfig에서 "/api/auth/login"으로 지정함
        super(defaultFilterProcessesUrl);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response)
            throws AuthenticationException, IOException {

        log.info("--- APILoginFilter ---");

        // 로그인은 아이디/비밀번호가 request body에 담기는 POST만 허용
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            log.info("Only POST Allowed");
            throw new AuthenticationServiceException("login_only_supports_post");
        }

        // 요청 JSON 예:
        // { "employeeNo": "SUPER001", "password": "1111" }
        Map<String, String> jsonData = parseRequestJSON(request);
//        log.info("--- 디버그 로그 jsonData: {} ---", jsonData);

        String employeeNo = jsonData.get("employeeNo");  // 사번(로그인 ID)
        String password = jsonData.get("password");

        // 사번이나 비밀번호가 비어 있으면 실제 DB 조회까지 가지 않고 바로 인증 실패로 처리
        if (employeeNo == null || employeeNo.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationServiceException("employeeNo_and_password_are_required");
        }

        // 사번을 대문자로 정규화
        // Locale.ROOT : 터키어 같은 특수 로케일에서 대문자 변환이 이상하게 되는 문제를 피하려고 넣은 안전장치
        String normalizedEmployeeNo = employeeNo.trim().toUpperCase(Locale.ROOT);
        log.info("로그인 요청 employeeNo={}", normalizedEmployeeNo);

        // 로그인 실패 핸들러에서도 사번을 알아야 실패 횟수를 기록할 수 있음
        // request body는 한 번 읽으면 다시 읽기 어려우므로 정규화한 사번을 request attribute에 보관
        request.setAttribute("employeeNo", normalizedEmployeeNo);

        // 로그인 실패 감사 로그에서도 사번을 남길 수 있도록 audit_log 전용 값으로 보관
        request.setAttribute("auditLoginEmployeeNo", normalizedEmployeeNo);

        // UsernamePasswordAuthenticationToken은 JWT가 아니라 Spring Security 인증 요청 객체
        // principal에는 로그인 ID인 사번, credentials에는 사용자가 입력한 비밀번호 원문을 담음
        // password는 PasswordEncoder 비교를 위해 trim하지 않음
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(normalizedEmployeeNo, password);

        // 실제 인증 판단은 AuthenticationManager에 위임
        // 내부에서 APIUserDetailsService로 직원 조회 후 PasswordEncoder로 비밀번호 검증
        // 성공/실패 결과는 Security 성공/실패 핸들러로 이어짐
        return getAuthenticationManager().authenticate(authenticationToken);
    }

    private Map<String, String> parseRequestJSON(HttpServletRequest request) {
        log.info("--- APILoginFilter.parseRequestJSON() 진입 ---");

        // HTTP request body의 JSON 문자열을 Map으로 처리
        // Gson은 JSON <-> Java 객체 변환을 도와주는 라이브러리
        try (Reader reader = new InputStreamReader(request.getInputStream())) {
            Gson gson = new Gson();

            // TypeToken은 "문자열 key와 문자열 value를 가진 Map"이라는 타입 정보를 Gson에게 알려줌
            // Map.class만 넘기면 Java가 제네릭 타입을 정확히 알 수 없어 컴파일 경고가 발생
            Type loginMapType = new TypeToken<Map<String, String>>() {
            }.getType();
            return gson.fromJson(reader, loginMapType);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AuthenticationServiceException("invalid_login_json");
        }
    }
}
