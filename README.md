# Office Mate

Office Mate는 사내 협업에 필요한 인증/인가, 직원 관리, 프로젝트/업무 관리, 일정, 게시판, 실시간 채팅, 감사 로그, AI 업무 보조 기능을 통합한 그룹웨어 시스템입니다.

이 프로젝트에서 저는 JWT 기반 인증/인가, 직원 관리, 휴직/복직/퇴사 승인, 계정 보안, 감사 로그 수집/조회 기능을 담당했습니다. 단순 CRUD 구현보다 실제 업무시스템에서 필요한 계정 상태 관리, 토큰 폐기, 권한 제어, 감사 이력 보존 흐름을 이해하고 구현하는 데 집중했습니다.

## 1. 프로젝트 개요

- 프로젝트 명칭: Office Mate
- 기간: 2026.04 ~ 2026.05
- 인원: 4명
- 핵심 컨셉: 직원 관리, 프로젝트/업무, 일정, 게시판, 실시간 채팅, 감사 로그, AI 업무 보조를 하나의 시스템에서 제공하는 통합 그룹웨어
- 개발 배경 및 목적: 기업 내 분산된 업무 도구와 소통 채널을 하나의 플랫폼으로 통합하고, 인증/권한/감사 로그를 통해 사내 업무시스템에 필요한 보안성과 추적 가능성을 확보하는 것을 목표로 했습니다.
- 담당 역할: 인증/인가, 직원 관리, 계정 보안, 휴직/복직/퇴사 승인, 감사 로그

## 2. 주요 기능

| 영역 | 기능 |
| --- | --- |
| 인증 | 로그인, 로그아웃, Access Token 검증, Refresh Token 재발급 |
| 계정 보안 | 비밀번호 변경, 임시 비밀번호 발급, 로그인 실패 계정 잠금, 관리자 잠금 해제 |
| 직원 관리 | 직원 등록/수정/삭제, 조직도, 부서/상태 필터링, 권한 관리 |
| 상태 신청 | 휴직/복직/퇴사 신청 및 승인/반려 |
| 감사 로그 | 로그인 및 주요 관리자 행위 기록, Redis 큐 적재, audit DB 이관, 조회/필터링 |
| 프로젝트/업무 | 프로젝트 생성/조회/상태 관리, 업무 할당/진척도 관리 |
| 대시보드/알림 | 진행 중 업무, 오늘 마감 업무, 지연 업무, 업무 알림 |
| 캘린더 | 개인/부서/프로젝트/업무 일정 관리, Google Calendar 내보내기 |
| 채팅/AI | WebSocket 실시간 채팅, Redis 초안 저장, OpenAI 기반 업무 보조 |
| 게시판 | 공지사항/자유게시판, 검색/페이징, 댓글, 첨부파일 |

### 담당 기능 요약

| 구분 | 구현 내용 |
| --- | --- |
| 인증/인가 | Spring Security Filter 기반 JSON 로그인, JWT Access Token 검증, Refresh Token 재발급 |
| 토큰 관리 | Refresh Token HttpOnly Cookie 저장, DB 해시 저장, 로그아웃/비밀번호 변경/휴직/퇴사 시 폐기 |
| 직원 관리 | 직원 등록/수정/삭제, 사번/이메일 중복 검증, 조직도 조회, 권한/재직 상태 관리 |
| 상태 신청 | 휴직/복직/퇴사 신청, SUPER 권한 기반 승인/반려, 비활성 계정 API 접근 차단 |
| 계정 보안 | 로그인 실패 횟수 누적, 5회 실패 시 계정 잠금, 관리자 잠금 해제 |
| 감사 로그 | AOP 기반 주요 API 성공/실패 수집, Redis 임시 저장, Scheduler 기반 audit DB 이관 |
| 관리자 화면 | 계정 보안 관리, 감사 로그 조건 검색/페이징/상세 조회 화면 연동 |

## 3. 기술 스택

- Language: Java 21
- Backend: Spring Boot, Spring MVC, Spring Security, Spring AOP, Spring Scheduling, Spring WebSocket/STOMP, Spring AI
- Frontend/View: Thymeleaf, Thymeleaf Layout Dialect, HTML, CSS, JavaScript, Axios
- Database: PostgreSQL, Redis
- Data Access: Spring Data JPA, MyBatis, JdbcTemplate, QueryDSL
- External API: OpenAI API, Google Calendar API, Java Mail
- Tools: Gradle, Lombok, ModelMapper, Gson, Springdoc OpenAPI, JUnit 5

## 4. 시스템 아키텍처 및 설계 흐름

Office Mate는 Thymeleaf 기반 화면에서 JavaScript/Axios로 API를 호출하고, Spring Boot 서버가 인증/인가, 업무 기능, 외부 API 연동을 처리하는 구조입니다. 데이터는 업무용 PostgreSQL DB와 감사 로그 전용 PostgreSQL DB로 분리했고, Redis는 감사 로그 임시 큐와 짧은 수명 데이터를 처리하는 용도로 사용했습니다.

### 4-1. 인증/인가 흐름

```text
Browser / Thymeleaf
 -> APILoginFilter
 -> AuthenticationManager
 -> APIUserDetailsService
 -> PasswordEncoder
 -> APILoginSuccessHandler / APILoginFailureHandler
 -> JWT Access Token 발급
 -> Refresh Token HttpOnly Cookie 저장 및 DB hash 저장
```

### 4-2. 계정 상태 및 토큰 보안 흐름

```text
API 요청
 -> TokenCheckFilter
 -> Access Token 검증
 -> DB 최신 직원/계정 상태 재조회
 -> ACTIVE/잠금 여부 판단
 -> 정상 요청만 Controller 진입
```

JWT Access Token은 서버가 직접 삭제하기 어렵기 때문에, 휴직/퇴사/계정 잠금처럼 상태가 바뀌는 업무시스템 요구사항은 매 요청 시 DB 최신 상태를 재검증하는 방식으로 보완했습니다.

### 4-3. 감사 로그 처리 흐름

```text
API 요청
 -> AuditLogAspect
 -> AuditLogPolicyRegistry
 -> Redis audit:log:queue
 -> AuditLogFlushScheduler
 -> audit_log 이관

Redis 장애
 -> audit_fallback_log 저장
 -> Scheduler 재이관
```

감사 로그는 비즈니스 로직과 분리된 공통 관심사로 보고 AOP로 수집했습니다. 사용자 요청 흐름에서 직접 audit DB에 저장하지 않고 Redis에 먼저 적재한 뒤 Scheduler가 이관하도록 구성했습니다.

## 5. 담당 기능

### 5-1. Spring Security Filter 기반 인증

로그인 요청은 일반 Controller가 아니라 `APILoginFilter`에서 처리했습니다.

```text
APILoginFilter
 -> AuthenticationManager
 -> APIUserDetailsService
 -> PasswordEncoder
 -> APILoginSuccessHandler / APILoginFailureHandler
```

Spring Security의 인증 흐름을 활용해 비밀번호 검증은 `PasswordEncoder`에 맡기고, 로그인 성공/실패 처리를 Security 레벨에서 일관되게 처리했습니다. 인증 성공 후에는 `SecurityContextHolder`와 연동해 이후 API 권한 체크 흐름으로 자연스럽게 이어지도록 구성했습니다.

### 5-2. JWT와 Refresh Token 관리

Access Token은 API 호출용으로 짧게 사용하고, Refresh Token은 Access Token 재발급용으로 분리했습니다.

초기에는 Access Token과 Refresh Token을 모두 JSON 응답으로 내려주고 localStorage에 저장했지만, XSS 발생 시 장기 Refresh Token까지 탈취될 수 있다는 문제가 있었습니다. 이를 개선해 Refresh Token은 HttpOnly Cookie로 저장하고, 서버 DB에는 원문 대신 SHA-256 해시만 저장하도록 변경했습니다.

```text
Access Token
 -> JSON 응답 body 반환
 -> localStorage 저장
 -> Authorization: Bearer 헤더로 API 호출

Refresh Token
 -> HttpOnly Cookie 저장
 -> DB에는 token_hash만 저장
 -> 로그아웃/비밀번호 변경/휴직/퇴사 시 revoke 처리
```

이 구조를 통해 브라우저 JavaScript에서 장기 Refresh Token에 직접 접근하지 못하게 했고, 서버에서는 DB의 `revoked_at` 상태를 기준으로 토큰 재사용을 차단할 수 있게 했습니다.

### 5-3. JWT claim만 신뢰하지 않는 계정 상태 검증

JWT는 발급 이후 값이 자동으로 바뀌지 않습니다. 따라서 직원이 퇴사 처리되거나, 권한이 변경되거나, 계정이 잠긴 뒤에도 기존 Access Token으로 API를 호출할 가능성이 있습니다.

이를 보완하기 위해 `TokenCheckFilter`에서 매 API 요청마다 JWT의 `employeeNo`를 기준으로 DB의 최신 직원 상태를 다시 조회했습니다.

```text
API 요청
 -> Access Token 검증
 -> employeeNo 추출
 -> DB에서 최신 직원/계정 상태 조회
 -> 휴직/퇴사/삭제/잠금 상태면 API 접근 차단
```

이 방식은 완전한 Stateless 구조의 장점은 일부 줄어들지만, 사내 업무시스템에서는 계정 상태 일관성과 보안이 더 중요하다고 판단했습니다.

### 5-4. 임시 비밀번호 변경 강제

신규 직원의 초기 비밀번호는 공통값으로 발급되기 때문에, 최초 로그인 후 개인 비밀번호로 변경하기 전까지 업무 기능 접근을 제한했습니다.

```text
직원 생성
 -> password = BCrypt(초기 비밀번호)
 -> temp_password_required = true

로그인 성공
 -> tempPasswordRequired 반환
 -> 비밀번호 변경 화면으로 이동

임시 비밀번호 상태에서 허용하는 API
 -> /api/auth/me
 -> /api/auth/logout
 -> /api/auth/change-password
```

이를 통해 초기 비밀번호가 고정되는 정책의 보안 리스크를 줄이고, 사용자가 반드시 개인 비밀번호로 전환하도록 했습니다.

### 5-5. 로그인 실패 계정 잠금 및 관리자 해제

반복적인 로그인 실패를 방치하면 비밀번호 대입 공격에 취약해질 수 있으므로, 로그인 실패 횟수를 추적하고 5회 이상 실패 시 계정을 자동 잠금 처리했습니다.

적용 정책은 다음과 같습니다.

- 로그인 실패 5회 이상 시 즉시 잠금
- 잠긴 계정은 올바른 비밀번호를 입력해도 로그인 불가
- 정상 로그인 성공 시 실패 횟수 초기화
- 잠긴 계정은 정상 로그인만으로 자동 해제되지 않음
- 관리자만 계정 잠금 해제 가능
- 관리자 해제 시 실패 횟수 0으로 초기화
- 관리자 해제 시각과 해제 관리자 사번 기록
- 기존 Access Token을 가진 사용자도 계정이 잠기면 API 접근 차단

계정 보안 상태는 `employee` 테이블과 분리해 `account_security_status`에서 관리했습니다. 직원 기본 정보와 로그인 실패/잠금 정책 상태의 책임을 분리하기 위한 설계입니다.

```text
로그인 실패
 -> APILoginFailureHandler
 -> AccountSecurityStatusService.recordLoginFailure()
 -> 실패 횟수 증가
 -> 5회 이상이면 account_locked = true

관리자 해제
 -> PATCH /api/management/account-security/{employeeNo}/unlock
 -> 실패 횟수 0 초기화
 -> unlocked_at, unlocked_by 기록
```

### 5-6. 직원 관리와 상태 신청

직원 생성/수정에는 단순 저장 이상의 업무 규칙이 있어 `EmployeeService`에 모았습니다.

- 사번 중복 불가
- 이메일 중복 불가
- 초기 비밀번호 BCrypt 저장
- 신규 직원은 임시 비밀번호 변경 필요 상태로 생성
- ADMIN은 USER 직원만 생성 가능
- SUPER는 ADMIN/SUPER 권한까지 관리 가능
- `employee_roles`에도 역할 저장
- 휴직/퇴사/삭제 처리 시 Refresh Token 폐기

또한 휴직/복직/퇴사 신청과 승인/반려 흐름을 구현하고, 비활성 계정은 다음 API 요청부터 `TokenCheckFilter`에서 차단되도록 처리했습니다.

### 5-7. AOP 기반 감사 로그

관리자 주요 행위와 로그인 성공/실패 이력을 자동 수집하기 위해 감사 로그 기능을 구현했습니다.

```text
API 요청
 -> AuditLogAspect
 -> AuditLogPolicyRegistry 정책 매칭
 -> AuditLogEventDTO 생성
 -> Redis audit:log:queue 저장
 -> AuditLogFlushScheduler
 -> AuditLogFlushService
 -> AuditLogJdbcRepository
 -> PostgreSQL office_mate_audit_log.audit_log
 -> 관리자 화면 조회
```

처음에는 `@Auditable` 어노테이션 방식도 검토했지만, 감사 대상 API가 여러 Controller에 흩어지는 문제가 있었습니다. 최종적으로 `AuditLogPolicyRegistry`에서 HTTP Method, URI 패턴, action, targetType, targetId 추출 규칙을 중앙관리하도록 구성했습니다.

감사 로그는 사용자 요청 흐름에 직접 DB insert를 수행하지 않고 Redis에 먼저 적재했습니다. 이후 Scheduler가 주기적으로 별도 audit DB로 이관합니다. Redis 장애 시에는 `audit_fallback_log`에 원본 이벤트를 저장하고, 복구 후 정식 `audit_log`로 재이관하는 fallback 흐름도 구현했습니다.

## 6. 데이터베이스 설계 요약

Office Mate는 메인 업무 DB와 감사 로그 DB를 분리했습니다. 메인 DB는 직원, 인증, 권한, 상태 변경 신청 등 업무 데이터를 관리하고, audit DB는 감사 로그와 fallback 로그를 별도로 보관합니다. 포트폴리오에서는 전체 테이블보다 제가 담당한 인증/인가, 계정 보안, 직원 상태 관리, 감사 로그와 직접 연결되는 테이블을 중심으로 설명합니다.

### 6-1. DB 분리와 모듈별 테이블 구성

| 영역 | 주요 테이블 | DB | 설명 |
| --- | --- | --- | --- |
| 조직 및 권한 관리 | `departments`, `position_rank`, `roles` | main DB | 부서, 직급 순서, 시스템 역할 기준 데이터 |
| 인사 및 계정 보안 | `employee`, `employee_status_request`, `account_security_status`, `refresh_token` | main DB | 직원 정보, 상태 신청, 계정 잠금, 토큰 보안 |
| 게시판 및 커뮤니티 | `board`, `post`, `post_attachment`, `post_comment` | main DB | 공지/자유게시판, 첨부파일, 댓글 |
| 협업 및 일정 관리 | `calendar_event`, `google_calendar_link` | main DB | 개인/팀/프로젝트 일정과 Google Calendar 연동 |
| 실시간 채팅 및 AI 설정 | `chat_room`, `chat_room_ai_config`, `chat_room_member`, `chat_message` | main DB | 채팅방, 참여자, 메시지, AI 설정 |
| 프로젝트 업무 및 알림 | `project`, `project_member`, `project_task`, `notification` | main DB | 프로젝트, 업무, 참여자, 알림 |
| 감사 로그 및 모니터링 | `audit_log`, `audit_fallback_log` | audit DB | 주요 API 감사 로그와 Redis 장애 fallback 로그 |

### 6-2. 담당 기능 핵심 테이블

| 구분 | 테이블 | 주요 컬럼 | 역할 |
| --- | --- | --- | --- |
| 기준 데이터 | `departments` | `code`, `name`, `sort_order` | 직원 소속 부서 마스터 데이터 |
| 기준 데이터 | `roles` | `code`, `name`, `description` | Spring Security 권한과 연결되는 `USER`, `ADMIN`, `SUPER` 역할 마스터 |
| 직원/계정 | `employee` | `employee_no`, `password`, `temp_password_required`, `name`, `email`, `department`, `position`, `role`, `status`, `joined_on`, `resigned_on`, `deleted_at` | 직원 기본 정보, 로그인 ID, 권한, 재직 상태, 임시 비밀번호 변경 필요 여부 관리 |
| 권한 | `employee_roles` | `employee_no`, `role_code`, `granted_at`, `granted_by` | 직원별 역할 부여 이력 관리 |
| 토큰 | `refresh_token` | `employee_no`, `token_hash`, `expires_at`, `revoked_at` | Refresh Token 원문 대신 해시 저장, 로그아웃/비밀번호 변경/휴직/퇴사 시 `revoked_at` 기록 |
| 계정 보안 | `account_security_status` | `employee_no`, `login_fail_count`, `account_locked`, `last_failed_at`, `locked_at`, `unlocked_at`, `unlocked_by` | 로그인 실패 횟수와 계정 잠금/해제 상태 관리 |
| 직원 상태 | `employee_status_request` | `employee_no`, `request_type`, `reason`, `status`, `decided_by`, `decided_at`, `decision_comment` | 휴직/복직/퇴사 신청과 승인/반려 이력 관리 |
| 감사 로그 | `audit_log` | `trace_id`, `actor_no`, `actor_role`, `action`, `target_type`, `request_uri`, `client_ip`, `result`, `occurred_at`, `flushed_at` | Redis 큐 또는 fallback에서 이관된 주요 API 감사 로그 영구 보관 |
| 감사 로그 | `audit_fallback_log` | `payload`, `reason`, `occurred_at`, `processed_at` | Redis 저장 실패 시 감사 로그 원본 JSON을 임시 보관하고 재이관 여부 관리 |

### 6-3. 테이블 관계

```text
departments / roles
    -> employee
        -> employee_roles
        -> refresh_token
        -> account_security_status
        -> employee_status_request

AuditLogAspect
    -> Redis audit:log:queue
        -> audit_log
    -> Redis 장애 시 audit_fallback_log
        -> audit_log 재이관
```

- `employee.department`는 `departments.code`를 참조해 직원의 소속 부서를 관리합니다.
- `employee.role`과 `employee_roles.role_code`는 `roles.code`를 참조해 API/화면 접근 권한의 기준이 됩니다.
- `refresh_token.employee_no`는 `employee.employee_no`를 참조합니다. 한 직원은 여러 Refresh Token 이력을 가질 수 있으며, `revoked_at`이 `NULL`인 토큰만 유효한 토큰으로 판단합니다.
- `account_security_status.employee_no`는 `employee.employee_no`를 PK/FK로 함께 사용합니다. 직원 기본 정보와 로그인 실패/계정 잠금 상태를 1:1로 분리하기 위한 구조입니다.
- `account_security_status.unlocked_by`는 잠금을 해제한 관리자 사번을 `employee.employee_no`로 기록합니다.
- `employee_status_request.employee_no`는 상태 변경 신청자를, `decided_by`는 승인/반려 처리자를 `employee.employee_no`로 기록합니다.
- `audit_log`는 별도 audit DB에 저장되며 메인 DB FK를 직접 두지 않고 `actor_no`, `target_type`, `target_id`를 기록합니다. 로그 보관과 업무 데이터 트랜잭션의 결합도를 낮추기 위한 설계입니다.
- `audit_fallback_log`는 Redis 저장 실패 시 원본 payload를 보관하고, 재이관 완료 시 `processed_at`으로 처리 여부를 남깁니다.

## 7. 프로젝트 구조

```text
src/main/java/office_mate_2605
├── board                  # 게시판, 댓글, 첨부파일
├── calender               # 캘린더 및 Google Calendar 연동
├── chat                   # WebSocket 채팅, AI 응답, Redis 초안
├── common
│   ├── controller         # 화면 진입 Controller
│   └── employee           # 직원, 조직도, 재직 상태 신청
├── config                 # Security, Redis, WebSocket, AI, DB 설정
├── dashboard              # 대시보드 통계, 알림
├── mail                   # 메일 발송
├── management
│   ├── audit_log          # 감사 로그 수집, fallback, 이관, 조회
│   └── auth               # 비밀번호, 계정 잠금, 인증 관련 API
├── project                # 프로젝트, 멤버, 업무(Task)
├── security               # JWT 필터, 로그인 핸들러, 토큰 서비스
└── util                   # JWT, BCrypt, 파일 업로드 유틸
```

## 8. 실행 방법

### 사전 준비

- Java 21
- PostgreSQL
- Redis
- OpenAI API Key
- Google Calendar API credentials

### 데이터베이스 준비

SQL 파일은 `src/main/resources/sql` 아래에 있습니다.

```text
01_init.sql
02_main_db.sql
03_audit_db.sql
04_main_dummy.sql
05_audit_log_dummy.sql
```

기본 실행 순서는 다음과 같습니다.

```text
1. 01_init.sql
2. 02_main_db.sql
3. 03_audit_db.sql
4. 04_main_dummy.sql
5. 05_audit_log_dummy.sql
```

### 환경 설정

민감정보는 Git에 포함하지 않고 환경변수 또는 `.env`로 관리합니다.

필요한 주요 설정은 다음과 같습니다.

```properties
spring.datasource.pgvector.url=jdbc:postgresql://localhost:5432/office_mate_2605
spring.datasource.pgvector.username=${DB_USERNAME}
spring.datasource.pgvector.password=${DB_PASSWORD}

spring.datasource.audit.url=jdbc:postgresql://localhost:5432/office_mate_audit_log
spring.datasource.audit.username=${AUDIT_DB_USERNAME}
spring.datasource.audit.password=${AUDIT_DB_PASSWORD}

spring.data.redis.host=localhost
spring.data.redis.port=6379

my.jwt.secret=${JWT_SECRET}
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.ai.openai.api-key=${OPENAI_API_KEY}

spring.servlet.multipart.location=${UPLOAD_PATH}
my.upload.path=${UPLOAD_PATH}
```

첨부파일 업로드 경로는 OS별 로컬 경로 차이를 고려해 `UPLOAD_PATH`로 분리했습니다. 애플리케이션에서는 `my.upload.path`를 기준으로 실제 파일을 저장하고, `/upload/**` URL 요청을 해당 로컬 디렉토리에 매핑합니다.

```properties
# macOS 예시
UPLOAD_PATH=/Users/your-name/upload

# Windows 예시
UPLOAD_PATH=C:/upload
```

Windows 환경에서는 역슬래시 이스케이프 문제를 줄이기 위해 `C:/upload`처럼 슬래시(`/`) 형식으로 작성하는 것을 권장합니다. 실제 업로드 파일은 Git에 포함하지 않으며, `upload/` 또는 `uploads/` 폴더는 `.gitignore`로 제외하는 것이 좋습니다.

Google Calendar 연동을 사용하려면 Google Cloud Console에서 OAuth Client를 생성하고, 로컬 환경에 credentials 파일을 별도로 배치해야 합니다. 실제 credentials 파일은 공개 저장소에 포함하지 않습니다.

### 애플리케이션 실행

```bash
./gradlew bootRun
```

접속 주소:

```text
http://localhost:8080
```

### 테스트 실행

```bash
./gradlew test
```

### API 문서

Springdoc OpenAPI Swagger UI를 통해 API 명세를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

## 9. 주요 화면

대표 화면은 README와 Notion 포트폴리오에 함께 첨부할 수 있도록 별도 캡처로 정리할 예정입니다.

추천 캡처 화면:

- 로그인 화면
- 대시보드
- 직원 등록
- 조직도
- 휴직/복직/퇴사 신청 관리
- 계정 보안 관리
- 감사 로그 조회
- 프로젝트/업무 관리
- 채팅 화면

## 10. 트러블슈팅

README에는 제출 검토자가 빠르게 확인할 수 있도록 대표 사례만 정리했습니다. 각 사례는 문제 상황, 원인 추적, 해결 방법, 결과를 중심으로 작성했습니다.

### 10-1. Refresh Token 저장 구조 개선

문제 상황: 초기 구현에서는 Access Token과 Refresh Token을 모두 JSON 응답으로 내려주고 브라우저 localStorage에 저장했습니다. 구현은 단순했지만, JavaScript에서 장기 유효한 Refresh Token에 직접 접근할 수 있다는 문제가 있었습니다.

원인 추적: 토큰 저장 위치를 검토하면서 Access Token은 짧은 수명으로 API 호출에 사용되지만, Refresh Token은 Access Token을 계속 재발급할 수 있는 장기 토큰이라는 점을 확인했습니다. localStorage는 브라우저 JavaScript에서 접근 가능하므로 XSS 상황에서 Refresh Token 탈취 위험이 더 크다고 판단했습니다.

원인: Access Token과 Refresh Token의 역할과 수명이 다른데도 동일한 저장 방식을 사용한 것이 핵심 원인이었습니다.

해결 방법: Access Token은 JSON body로 반환해 API 호출에 사용하고, Refresh Token은 HttpOnly Cookie에 저장하도록 분리했습니다. 서버 DB에는 Refresh Token 원문 대신 SHA-256 해시만 저장하고, 로그아웃/비밀번호 변경/휴직/퇴사 처리 시 `revoked_at`을 기록해 재발급을 차단했습니다.

결과: 브라우저 JavaScript에서 Refresh Token에 직접 접근할 수 없게 되었고, 서버에서는 DB의 hash 및 `revoked_at` 상태를 기준으로 Refresh Token 유효성을 통제할 수 있게 되었습니다.

배운 점: JWT 기반 인증에서도 토큰을 발급하는 것만큼 저장 위치와 폐기 정책이 중요하다는 것을 배웠습니다.

핵심 정리: 장기 토큰은 JavaScript 접근 영역에서 분리하고, 서버 측에서 재발급 권한을 통제할 수 있어야 합니다.

### 10-2. JWT stateless 한계 보완

문제 상황: 관리자가 직원을 휴직/퇴사 처리하거나 계정을 잠가도, 이미 발급된 Access Token이 남아 있으면 사용자가 기존 토큰으로 API를 계속 호출할 가능성이 있었습니다.

원인 추적: JWT Access Token은 서버 세션을 조회하지 않는 Stateless 구조이기 때문에, 발급 시점의 claim만 검증하면 이후 DB에서 변경된 직원 상태나 계정 잠금 상태가 즉시 반영되지 않는다는 점을 확인했습니다.

원인: Access Token 자체는 서버에서 직접 삭제할 수 없고, 토큰 내부 claim은 발급 이후 자동으로 변경되지 않는 것이 원인이었습니다.

해결 방법: `TokenCheckFilter`에서 Access Token 검증 후 `employeeNo`를 기준으로 DB의 최신 직원 상태와 계정 잠금 상태를 다시 조회했습니다. `ACTIVE`가 아니거나 잠긴 계정이면 `ACCOUNT_INACTIVE` 또는 `ACCOUNT_LOCKED` 응답을 반환하고, 프론트 공통 fetch handler에서 알림 표시 후 브라우저 토큰을 삭제하고 로그인 페이지로 이동하도록 처리했습니다.

결과: 기존 Access Token이 남아 있어도 다음 API 요청부터 최신 계정 상태를 기준으로 접근을 차단할 수 있게 되었습니다.

배운 점: Stateless 인증 구조는 확장성과 단순성의 장점이 있지만, 업무시스템처럼 계정 상태 변경을 즉시 반영해야 하는 환경에서는 DB 재검증 같은 보완 정책이 필요하다는 것을 배웠습니다.

핵심 정리: JWT claim만 신뢰하지 않고, 중요한 업무 API 진입 전 DB 최신 상태를 확인해 계정 생명주기 정책을 반영했습니다.

### 10-3. 감사 로그 Redis 장애 대비 fallback

문제 상황: 감사 로그는 API 요청 흐름에 부담을 주지 않기 위해 Redis 큐에 먼저 저장한 뒤 Scheduler가 audit DB로 이관하는 구조였습니다. 하지만 Redis 장애가 발생하면 감사 로그 이벤트가 저장되지 못하고 유실될 수 있었습니다.

원인 추적: 감사 로그는 보안 추적과 운영 이력 확인에 필요한 데이터이므로, Redis 장애 상황에서도 최소한 원본 이벤트를 남길 수 있어야 한다고 판단했습니다. Redis 저장 실패 시 예외만 남기면 이후 복구나 재처리가 불가능했습니다.

원인: Redis를 감사 로그의 단일 임시 저장소로 사용하면서, Redis 장애 시 대체 저장 경로가 없었던 것이 원인이었습니다.

해결 방법: Redis 저장 실패 시 `audit_fallback_log` 테이블에 `AuditLogEventDTO` 원본을 JSON으로 저장했습니다. 이후 Scheduler가 `processed_at IS NULL`인 fallback 로그를 조회해 정식 `audit_log` 테이블로 재이관하고, 이관 완료 후 `processed_at`을 갱신하도록 구성했습니다.

```text
Redis 장애
 -> audit_fallback_log 저장
 -> Redis 복구
 -> Scheduler 실행
 -> audit_log 이관
 -> processed_at 갱신
```

결과: Redis 장애 상황에서도 감사 로그 원본을 보관할 수 있게 되었고, 복구 후 정식 audit DB로 재이관할 수 있는 흐름을 확보했습니다.

배운 점: 비동기 처리 구조에서는 성능뿐 아니라 실패 시 데이터가 어디에 남는지까지 설계해야 한다는 점을 배웠습니다.

핵심 정리: Redis는 빠른 임시 저장소로 사용하되, 장애 시 RDB fallback을 둬 감사 로그 유실 가능성을 줄였습니다.

## 11. 성과 및 배운 점

### 11-1. JWT 기반 인증 구조와 토큰 보안 정책 이해

- Access Token과 Refresh Token의 역할을 분리하고, Refresh Token을 HttpOnly Cookie와 DB 해시 저장 방식으로 관리하며 JWT 기반 Stateless 인증 구조의 장단점을 실무적으로 이해했습니다.
- 로그아웃, 비밀번호 변경, 휴직/퇴사 처리 시 Refresh Token을 폐기하여 서버 측에서 토큰 재발급을 통제하는 보안 흐름을 학습했습니다.

### 11-2. Spring Security 기반 인증/인가 흐름 설계

- 커스텀 로그인 필터, 토큰 검증 필터, Refresh Token 재발급 필터를 구성하며 Spring Security Filter Chain의 동작 원리를 파악했습니다.
- `@PreAuthorize`와 공통 layout 권한 검사를 함께 적용하여 API 접근 제어와 화면 접근 제어를 분리해 설계하는 방법을 경험했습니다.

### 11-3. 계정 생명주기와 보안 정책 구현 경험

- 직원 등록부터 휴직, 복직, 퇴사, 계정 잠금, 비밀번호 재설정까지 이어지는 계정 생명주기 관리 흐름을 구현했습니다.
- JWT의 Stateless 특성으로 이미 발급된 Access Token을 즉시 삭제하기 어려운 문제를 DB 최신 상태 재검증 방식으로 보완했습니다.

### 11-4. AOP와 Redis 기반 감사 로그 파이프라인 설계

- 주요 API 요청을 AOP로 감지하고 Redis에 우선 적재한 뒤, Scheduler를 통해 감사 로그 전용 DB로 이관하는 비동기 로그 처리 구조를 구현했습니다.
- 감사 로그 대상 API를 중앙 정책 Registry로 관리하며 로그 수집 기준을 일관되게 유지하는 설계 방식을 학습했습니다.

### 11-5. 장애 대응과 데이터 유실 방지 관점 습득

- Redis 저장 실패 시 fallback 테이블에 감사 로그 원본을 저장하고 이후 재이관하는 구조를 구현하며, 장애 상황에서도 데이터 유실 가능성을 줄이는 방식을 경험했습니다.
- Controller, Service, Repository, DTO 계층을 분리하면서 인증/인가, 계정 보안, 감사 로그 기능을 유지보수 가능한 구조로 정리했습니다.

## 12. 개선 가능성 및 향후 계획

### 12-1. Refresh Token 보안 정책 고도화

- 현재는 DB 해시 저장과 revoke 정책을 통해 Refresh Token을 관리하고 있으나, 향후 기기별 로그인 세션 관리와 토큰 재사용 감지 로직을 추가해 보안성을 강화할 수 있습니다.
- Refresh Token 탈취 의심 상황이 발생하면 해당 계정의 모든 세션을 폐기하고 관리자에게 알림을 제공하는 구조로 확장할 계획입니다.

### 12-2. 감사 로그 검색 및 모니터링 기능 고도화

- 현재는 조건 검색과 CSV 추출 중심으로 감사 로그를 제공하고 있으나, 향후 관리자 대시보드에서 이상 행위 패턴을 시각화하는 기능을 추가할 수 있습니다.
- 로그인 실패 급증, 권한 변경, 대량 CSV 다운로드 같은 보안 이벤트를 실시간 알림으로 연결하는 방향으로 개선할 계획입니다.

### 12-3. 권한 관리 구조 세분화

- 현재는 USER, ADMIN, SUPER 중심의 역할 기반 접근 제어를 적용하고 있으나, 향후 부서별 권한과 기능별 세부 권한을 분리한 Permission 기반 구조로 확장할 수 있습니다.
- 관리자 권한 변경 이력과 승인 절차를 강화해 기업 환경에 적합한 권한 관리 체계로 고도화할 계획입니다.

## 13. 회고

프로젝트 기간 동안 인증, 권한, 계정 보안, 감사 로그처럼 시스템의 기반이 되는 영역을 담당하며 보안 중심의 백엔드 설계를 경험했습니다.

가장 많이 고민했던 부분은 JWT 기반 인증 구조에서 보안성과 사용자 편의성을 함께 확보하는 것이었습니다. 초기에는 Access Token과 Refresh Token을 모두 localStorage에 저장하는 방식이 단순해 보였지만, XSS 발생 시 장기 토큰이 탈취될 수 있다는 위험을 확인하고 Refresh Token을 HttpOnly Cookie와 DB 해시 저장 방식으로 분리했습니다. 이 과정을 통해 단순히 기능이 동작하는 것보다, 어떤 위치에 어떤 데이터를 저장해야 안전한지 판단하는 보안 설계의 중요성을 체감했습니다.

또한 휴직이나 퇴사처럼 계정 상태가 변경되었을 때 기존 Access Token을 어떻게 차단할지도 중요한 문제였습니다. JWT Stateless 구조에서는 서버가 이미 발급된 토큰을 직접 삭제하기 어렵지만, TokenCheckFilter에서 매 요청마다 DB의 최신 계정 상태를 재검증하도록 개선하여 실제 업무 요청 시점에 계정 상태 변경을 반영할 수 있었습니다.

감사 로그 기능을 구현하면서는 비즈니스 로직과 로그 수집 로직을 분리하는 설계의 필요성을 배웠습니다. AOP로 주요 API 요청을 감지하고 Redis와 Scheduler를 활용해 감사 로그 전용 DB로 이관하는 구조를 만들며, 성능과 추적 가능성을 함께 고려한 백엔드 설계를 경험했습니다.

이번 프로젝트를 통해 인증과 권한은 단순히 로그인 기능에 머무는 것이 아니라, 계정 상태, 화면 접근, 토큰 폐기, 감사 추적까지 연결되는 하나의 보안 흐름이라는 점을 배웠습니다. 짧은 일정이었지만 시스템의 기반을 책임지는 기능들을 구현하며 백엔드 개발자로서 보안 관점과 안정성에 대한 기준을 한 단계 높일 수 있었습니다.
