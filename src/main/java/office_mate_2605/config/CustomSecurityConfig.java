package office_mate_2605.config;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.audit_log.service.AuditLoginLogService;
import office_mate_2605.management.auth.service.AccountSecurityStatusService;
import office_mate_2605.security.APIUserDetailsService;
import office_mate_2605.security.cookie.RefreshTokenCookieProvider;
import office_mate_2605.security.filter.APILoginFilter;
import office_mate_2605.security.filter.RefreshTokenFilter;
import office_mate_2605.security.filter.TokenCheckFilter;
import office_mate_2605.security.handler.APILoginFailureHandler;
import office_mate_2605.security.handler.APILoginSuccessHandler;
import office_mate_2605.security.service.RefreshTokenService;
import office_mate_2605.util.JWTUtil;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

/**
 * Spring Security 기반 JWT 인증과 인가 설정을 담당하는 Config. (작성자: 서주연)
 *
 * <p>로그인 요청을 처리하는 APILoginFilter, 일반 API 요청의 accessToken을 검증하는
 * TokenCheckFilter, refreshToken 재발급을 처리하는 RefreshTokenFilter를 Security FilterChain에 등록한다.</p>
 *
 * <p>서버 세션을 사용하지 않는 STATELESS 정책, URL별 접근 권한, @PreAuthorize 기반 메서드 보안,
 * 인증/인가 실패 JSON 응답, 정적 리소스 보안 제외, CORS 허용 정책, BCrypt PasswordEncoder Bean을 함께 설정한다.</p>
 */
@Log4j2
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class CustomSecurityConfig {

    // Spring Security는 요청이 Controller에 도착하기 전에 여러 Filter를 통과시킴
    // 로그인, 일반 API 요청, 토큰 재발급 요청을 각각 커스텀 필터로 처리
    // 서버 세션을 쓰지 않고 JWT만 사용하도록 STATELESS로 설정

    private final APIUserDetailsService apiUserDetailsService;  // DB에서 직원 정보를 조회
    private final RefreshTokenService refreshTokenService;  // refreshToken을 DB에 저장/검증/폐기
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;  // refreshToken HttpOnly Cookie 생성/삭제 담당
    private final AccountSecurityStatusService accountSecurityStatusService;  // 로그인 실패 횟수/계정 잠금 관리
    private final AuditLoginLogService auditLoginLogService;  // 로그인 성공/실패 감사 로그 기록 담당
    private final JWTUtil jwtUtil;  // JWT 생성/검증 도구

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        log.info("--- SecurityConfig FilterChain ---");

        // CORS: 브라우저가 다른 주소의 API를 호출할 때 필요한 허용 규칙
        httpSecurity.cors(cors ->
                cors.configurationSource(corsConfigurationSource()));

        // REST API + JWT 방식에서는 서버 세션/폼 로그인/HTTP Basic 인증을 사용하지 않음
        httpSecurity.csrf(csrf ->
                csrf.disable());  // REST API는 CSRF 공격에서 비교적 자유로워 끔
        httpSecurity.formLogin(form ->
                form.disable());  // 기본 로그인 페이지를 쓰지 않음
        httpSecurity.httpBasic(basic ->
                basic.disable());  // ID/PW를 헤더에 직접 실어 보내는 방식을 쓰지 않음
        // 세션을 쓰지 않으므로 모든 요청은 토큰으로 들고 옴
        httpSecurity.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // URL별 접근 규칙
        // 로그인/토큰 재발급/Swagger/임시 화면은 토큰 없이 접근 가능하고, /api/** 나머지는 JWT 인증이 필요함
        httpSecurity.authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // 인트로/로그인 화면. 토큰이 없어도 진입할 수 있어야 한다.
                                "/",
                                "/intro/**",
                                "/login",
                                // 로그인 전 비밀번호 찾기/재설정 화면.
                                "/password-find",
                                "/password-reset",
                                // 화면 HTML 진입 경로.
                                // 실제 사용자 권한 검사는 화면 안에서 호출하는 /api/** 요청과 @PreAuthorize에서 처리한다.
                                "/dashboard",
                                "/password-change",
                                "/mypage",
                                "/employees/register",
                                "/organization-chart",
                                "/account-security/manage",
                                "/status-requests/manage",
                                // Swagger/OpenAPI 문서 확인용 경로.
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                // 로그인/토큰 재발급 API.
                                // accessToken을 발급받기 전 호출해야 하므로 permitAll이 필요하다.
                                "/api/auth/login",
                                "/api/auth/refresh",
                                // 로그인 전 비밀번호 찾기 API.
                                // 사번+이메일 또는 인증 정보로 본인 확인을 하므로 JWT 없이 호출한다.
                                "/api/auth/password-reset/request",
                                "/api/auth/password-reset/confirm",
                                // 휴직 계정의 로그인 전 복직 신청 API.
                                // JWT는 없지만 사번+비밀번호를 Service에서 재검증한다.
                                "/api/auth/return-from-leave/request",

//                             "/api/projects/**",  // 프로젝트 관리
                                "/chat/*",
                                // 구글 OAuth가 서버로 직접 호출하는 캘린더 연동 콜백은 JWT 헤더를 실을 수 없어 예외 허용함
                                "/api/calendar/callback",
                                "/api/chat/room"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()  // api로 시작하는 모든 주소는 반드시 인증(토큰검사)을 통과해야함
                        .anyRequest().permitAll()
        );

        // 인증 실패나 권한 부족이 발생했을 때 HTML 에러 페이지 대신 JSON으로 응답
        httpSecurity.exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    // authenticationEntryPoint : 인증 자체가 안 된 경우 실행
                    // 예: accessToken 없이 /api/** 요청
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().println(new Gson().toJson(Map.of(
                            "error", "UNAUTHORIZED",
                            "message", authException.getMessage()
                    )));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // accessDeniedHandler : 인증은 되었지만 권한이 부족한 경우 실행
                    // 예: USER가 ADMIN/SUPER 전용 API 호출
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().println(new Gson().toJson(Map.of(
                            "error", "FORBIDDEN",
                            "message", accessDeniedException.getMessage()
                    )));
                })
        );

        // 1. AuthenticationManagerBuilder 가져오기 (AuthenticationManager 객체 생성을 위해서)
        AuthenticationManagerBuilder authenticationManagerBuilder =
                httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);

        // 2. 사용자 인증 정보 구성 (UserDetailsService + PasswordEncoder)
        authenticationManagerBuilder  // 빌더 객체를 들고오고 사용자 인증 정보를 무엇으로 할 지 기재
                .userDetailsService(apiUserDetailsService)  // 사용자 인증 시 로그인 정보를 불러올 서비스
                .passwordEncoder(passwordEncoder());  // 암호화된 비밀번호 들고옴

        // 3. AuthenticationManager 객체 생성 (실제 로그인 검증을 담당)
        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        // 4. Security 설정에 authenticationManager 명시적 등록
        httpSecurity.authenticationManager(authenticationManager);

        // 5. APILoginFilter 생성 및 AuthenticationManager 설정
        // /api/auth/login 요청을 처리할 커스텀 로그인 필터를 만듦
        APILoginFilter apiLoginFilter = new APILoginFilter("/api/auth/login");
        apiLoginFilter.setAuthenticationManager(authenticationManager);

        // 1) 로그인 성공 시 accessToken은 JSON body로, refreshToken은 HttpOnly Cookie로 내려줌
        // APILoginSuccessHandler 내부에서 refreshToken은 DB에 해시로 저장됨
        apiLoginFilter.setAuthenticationSuccessHandler(
                new APILoginSuccessHandler(jwtUtil, refreshTokenService, accountSecurityStatusService, refreshTokenCookieProvider, auditLoginLogService)
        );

        // 2) 로그인 실패 시에도 JSON 형태로 실패 사유를 내려줌
        // 기본 Security 실패 응답은 HTML일 수 있으므로 REST API에 맞게 JSON으로 통일
        // --> 로그인 실패 횟수 증가와 계정 잠금 응답 로직이 커져 별도 Handler로 분리하였음
        // SecurityConfig는 필터 등록과 보안 설정 역할에 집중하도록 유지
        apiLoginFilter.setAuthenticationFailureHandler(
                new APILoginFailureHandler(accountSecurityStatusService, auditLoginLogService)
        );

        // 7. 일반 API 요청에서 Authorization: Bearer <token>을 검사하는 필터
        // 로그인 이후 /api/** 요청이 들어오면 accessToken 검증을 담당
        TokenCheckFilter tokenCheckFilter = new TokenCheckFilter(apiUserDetailsService, jwtUtil);

        // 8. 기본 UsernamePasswordAuthenticationFilter보다 먼저 동작하도록 배치
        // apiLoginFilter : 로그인 요청 처리
        // tokenCheckFilter : 로그인 이후 API 요청의 accessToken 검사
        httpSecurity.addFilterBefore(apiLoginFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(tokenCheckFilter, UsernamePasswordAuthenticationFilter.class);

        // refresh는 access token이 만료된 뒤에도 호출되어야 하므로 별도 필터로 처리
        // RefreshTokenFilter는 /api/auth/refresh 요청에서 refreshToken을 검증하고 새 accessToken을 발급
        httpSecurity.addFilterBefore(
                new RefreshTokenFilter("/api/auth/refresh", jwtUtil, refreshTokenService, refreshTokenCookieProvider),
                TokenCheckFilter.class
        );

        // 위에서 설정한 Security 규칙들을 실제 FilterChain 객체로 만들어 Spring에 반환
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // CSS, JS, 이미지 같은 정적 리소스는 JWT 검사를 하지 않도록 제외
        log.info("--- SecurityConfig 정적 리소스 보안 제외 설정 ---");
        return web -> web.ignoring().requestMatchers(PathRequest.toStaticResources().atCommonLocations());
    }

    /* CORS */
    // 현재는 Spring Boot가 HTML과 API를 함께 제공하므로 localhost:8080이 기본이지만,
    // 별도 프론트 개발 서버 확장 가능성이 있어 localhost:3000도 허용
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // CORS : 프론트엔드 개발 서버와 백엔드 서버 주소가 다를 때도 API 요청을 허용하기 위한 설정
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(List.of(
                "http://localhost:8080",
                "http://localhost:3000",
                "http://172.17.0.*:8080"
        ));
        // setAllowedMethods : 클라이언트가 어떤 HTTP 방식(GET, POST 등) 으로 접근할 수 있는지 정의
        corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));
        // setAllowedHeaders : 클라이언트가 요청 시 어떤 헤더(Authorization, Content-Type 등)를 실어서 보낼 수 있는지 정의
        corsConfiguration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        // allowCredentials=true : 쿠키/인증 정보 포함 요청을 허용
        // 현재 JWT는 Authorization 헤더를 사용하지만, 추후 쿠키 기반 확장 가능성을 고려해 true 유지
        corsConfiguration.setAllowCredentials(true);

        // 모든 URL 경로에 위 CORS 정책을 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
}
