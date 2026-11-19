# TS 001. 인증과 권한 검사

## 요청별 필터 구성

로그인, 토큰 재발급, 업무 API 인증을 각각 다른 필터에서 처리하도록 구성했습니다. `CustomSecurityConfig.filterChain`에서 기본 Form Login, HTTP Basic, CSRF를 끄고 `STATELESS`와 메서드 보안을 설정했습니다. `APILoginFilter`와 `TokenCheckFilter`는 `UsernamePasswordAuthenticationFilter` 앞에, `RefreshTokenFilter`는 `TokenCheckFilter` 앞에 등록했습니다. [Security 설정](../../src/main/java/office_mate_2605/config/CustomSecurityConfig.java)

| 요청 | 처리 방식 |
| --- | --- |
| `POST /api/auth/login` | `APILoginFilter.attemptAuthentication`이 JSON을 읽고 인증을 `AuthenticationManager`에 위임합니다. |
| `/api/auth/refresh` | `RefreshTokenFilter.doFilterInternal`이 경로가 일치하면 쿠키를 읽습니다. 화면에서는 POST로 요청합니다. 필터에서 HTTP 메서드를 따로 제한하지는 않습니다. |
| 보호된 `/api/**` | `TokenCheckFilter.doFilterInternal`이 Bearer JWT와 DB의 계정 상태를 확인합니다. 이후 `authenticated()`와 메서드의 `@PreAuthorize`를 적용합니다. |
| 공개 API와 화면 | 로그인과 재발급, 비밀번호 재설정 요청과 확인, 복직 신청, Google Calendar 콜백, `/api/chat/room`을 허용합니다. 토큰 필터는 화면 요청을 건너뛰고, 정적 리소스는 별도 설정으로 제외합니다. |

`permitAll`의 경로와 필터 내부의 제외 조건은 따로 관리합니다. 일부 필터는 `startsWith`로 경로를 비교하므로 경로를 추가할 때 두 곳을 확인해야 합니다. 화면에서는 역할별로 메뉴를 표시하고, 서버에서는 감사 로그 조회처럼 권한이 필요한 메서드에 `hasRole('SUPER')`를 적용했습니다.

## 로그인 성공과 실패

1. 필터에서 POST 요청인지, `employeeNo`와 `password`가 있는지 확인합니다. 사번의 앞뒤 공백을 제거하고 대문자로 바꿉니다. `APIUserDetailsService.loadUserByUsername`에서 삭제되지 않은 직원의 기본 역할, 추가 역할, 잠금 여부를 가져오고 `AuthenticationManager`가 BCrypt로 비밀번호를 확인합니다.
2. 성공 핸들러에서 잠기지 않은 계정의 실패 횟수를 초기화합니다. `ON_LEAVE`나 `RESIGNED`이면 HTTP 423과 함께 각각 `ACCOUNT_ON_LEAVE` 또는 `ACCOUNT_RESIGNED`를 반환합니다. 이때는 토큰 발급과 성공 감사 기록까지 진행하지 않습니다.
3. 발급 대상이면 `recordSuccess`로 기록하고 `employeeNo`, `role`, `tempPasswordRequired`를 담아 토큰에 서명합니다. Access Token과 직원 정보는 JSON으로 반환합니다. Refresh Token은 DB에 해시를 저장한 뒤 HttpOnly 쿠키로 전달합니다. 임시 비밀번호 상태인 직원도 로그인할 수 있습니다.
4. 실패 핸들러에서는 `recordFail`을 호출합니다. 이미 잠긴 계정이면 423을 반환합니다. 그 외에는 삭제되지 않은 직원의 실패 횟수를 누적해 5회부터 423을, 그 전에는 401 `LOGIN_FAILED`를 반환합니다. 등록되지 않은 사번에는 보안 상태 행을 만들지 않습니다.

## 보호 API의 검사 순서

1. `validateAccessToken`에서 Authorization 헤더와 Bearer 형식을 검사하고 `JWTUtil.validateToken`에서 서명과 만료를 확인합니다.
2. claim의 `employeeNo`가 비어 있으면 거부합니다. 사번이 있으면 DB를 다시 조회하며 `deleted_at`이 있는 직원은 제외합니다.
3. 재직 상태, 잠금, 임시 비밀번호 순으로 확인합니다. 비활성 계정과 잠긴 계정에는 로그아웃만 허용합니다. 임시 비밀번호 상태에는 `/api/auth/me`, `/api/auth/logout`, `/api/auth/change-password`를 허용합니다.
4. DB에서 읽은 권한으로 `UsernamePasswordAuthenticationToken`을 만들어 `SecurityContextHolder`에 설정합니다. 이후 Controller와 서비스는 이 요청의 사용자 정보(principal)를 사용합니다.

| 실패 조건 | HTTP 상태와 오류 코드 |
| --- | --- |
| 헤더가 없거나 너무 짧음 / 잘못된 인증 방식 / Access Token 만료 | 401 `UNACCEPT` / `BADTYPE` / `EXPIRED` |
| Access Token 형식이나 사번 오류 / 서명 오류 | 403 `MALFORM` / `BADSIGN` |
| 비활성 계정 / 계정 잠금 / 임시 비밀번호 제한 | 423 `ACCOUNT_INACTIVE` / `ACCOUNT_LOCKED` / `TEMP_PASSWORD_REQUIRED` |
| Spring Security의 미인증 요청 / 권한 부족 | 401 `UNAUTHORIZED` / 403 `FORBIDDEN` |
| 삭제되지 않은 직원 조회 실패 | `UsernameNotFoundException`이 발생합니다. 필터가 직접 응답을 작성하는 예외는 `AccessTokenException`이므로 삭제 직후의 HTTP 응답 형식은 통합 테스트가 필요합니다. |

## 토큰 재발급과 로그아웃

공통 레이아웃의 `window.fetch`로 보낸 요청이 401 `EXPIRED`를 받으면 Refresh Token 쿠키를 포함해 재발급을 요청합니다. 대시보드나 캘린더에서 Axios로 보낸 요청에는 이 처리가 적용되지 않습니다. `RefreshTokenFilter`는 서명, 만료, 사번과 `RefreshTokenService.isValid` 결과를 확인합니다. 기존 Refresh Token의 claim으로 새 Access Token을 만들어 JSON으로 응답합니다. Refresh Token의 남은 기간이 2일 이하이면 함께 교체합니다. [공통 화면 처리](../../src/main/resources/static/js/layout/layout.js)

쿠키가 없으면 400 `NO_REFRESH`, 형식 오류는 403 `OLD_REFRESH`, 서명이나 만료 또는 사번 오류는 401 `BAD_REFRESH`입니다. DB에 유효한 행이 없으면 401 `NOT_FOUND_REFRESH`를 반환합니다. 이 단계에서는 계정 잠금을 확인하지 않습니다.

`AuthService.logout`에서 해당 직원의 Refresh Token을 폐기하고 Controller에서 쿠키를 지웁니다. 화면의 `handleAccountInactiveResponse`는 `ACCOUNT_INACTIVE`를 받으면 로컬 인증 정보를 정리하고 로그인 화면으로 이동합니다. `ACCOUNT_LOCKED`에는 이 함수의 처리가 적용되지 않습니다.

## 비밀번호 변경과 재설정

`changePassword`에서 새 비밀번호가 8자 이상인지 확인합니다. 현재 비밀번호가 전달되면 DB의 해시와 대조합니다. 변경 후에는 임시 비밀번호 플래그를 해제하고 Refresh Token을 폐기합니다.

비밀번호를 잊은 경우에는 `PasswordResetService.issueTempPassword`에서 사번과 이메일을 확인합니다. 임시 비밀번호는 메일로 보내고 해시는 Redis에 10분간 저장합니다. `confirmResetPassword`에서 임시 비밀번호를 확인한 뒤 새 비밀번호를 저장하고, Refresh Token을 폐기하고 Redis 키를 지웁니다. [PasswordResetService](../../src/main/java/office_mate_2605/management/auth/service/PasswordResetService.java)
