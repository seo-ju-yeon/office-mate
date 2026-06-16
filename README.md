# Office Mate

Office Mate는 실시간 메신저, 프로젝트/업무 관리, 인사 관리, 일정 관리, 게시판, 감사 로그, AI 업무 보조 기능을 통합한 사내 그룹웨어 시스템입니다.

기업 내부에서 분산되기 쉬운 소통 채널, 프로젝트 현황, 일정, 직원 정보, 보안 이력을 하나의 플랫폼에서 관리하도록 설계했습니다. JWT 기반 인증, RBAC 권한 제어, 감사 로그 기록, WebSocket 실시간 채팅, OpenAI 기반 업무 보조 기능을 통해 협업 효율과 보안성을 함께 높이는 것을 목표로 합니다.

## 주요 기능

| 구분 | 기능 |
| --- | --- |
| 공통 인증 | JSON 기반 로그인, Access/Refresh Token 발급, JWT 검증, 토큰 재발급, 로그아웃 |
| 계정 보안 | 비밀번호 변경, 임시 비밀번호 발급, Redis 기반 임시 비밀번호 검증, 계정 잠금/해제 |
| 직원 관리 | 직원 등록/수정/삭제, 사번/이메일 중복 확인, 조직도 조회, 부서/상태 필터링 |
| 상태 신청 관리 | 휴직/복직/퇴사 신청, SUPER 승인/반려, ACTIVE SUPER 보호 정책 |
| 권한 관리 | USER/ADMIN/SUPER 역할 기반 API 접근 제어, 화면 접근 제어, 공지 작성 권한 체크 |
| 감사 로그 | 로그인/주요 시스템 행위 기록, Redis 임시 적재, 스케줄러 이관, 조건 검색, CSV 내보내기 |
| 프로젝트 관리 | 프로젝트 생성/조회/상태 변경/논리 삭제, 프로젝트 멤버 초대/조회/삭제 |
| 업무(Task) 관리 | 담당 업무 조회, 팀원별 업무 조회, 업무 등록/수정/삭제, 진척도 및 상태 관리 |
| 대시보드 | 진행 중 업무, 오늘 마감 업무, 지연 업무 등 요약 통계 조회 |
| 알림 서비스 | 업무 할당 및 변경 알림 조회, 읽음 처리 |
| 캘린더 | 개인/부서/프로젝트/업무 일정 조회, 일정 등록/수정/삭제, Google Calendar 내보내기 |
| 채팅 | 1:1/그룹 채팅방, STOMP 실시간 메시지 송수신, 메시지 저장, 읽음 처리 |
| 채팅 부가 기능 | Redis 기반 메시지 초안 저장/복구, 안 읽은 메시지 수 계산 |
| AI 서비스 | `@AI` 일정/프로젝트 질의응답, 선택 메시지 기반 업무 요약 |
| 게시판 | 공지사항/자유게시판, 검색/페이징, 상단 고정, 댓글, 첨부파일 업로드/다운로드 |

## 기술 스택

### Backend

- Java 21
- Spring Boot 3.5.14
- Spring MVC, Spring WebFlux
- Spring Security
- Spring Data JPA
- MyBatis
- QueryDSL
- Spring AOP
- Spring Scheduling
- Spring WebSocket, STOMP
- Spring AI

### Database & Infrastructure

- PostgreSQL
- PgVector 확장 기반 AI/RAG 확장 구조
- Redis
- JdbcTemplate
- Google Calendar API
- OpenAI API
- Java Mail

### Frontend

- Thymeleaf
- Thymeleaf Layout Dialect
- HTML, CSS, JavaScript
- Axios
- STOMP.js

### Tools & Libraries

- Gradle
- Lombok
- ModelMapper
- Gson
- Springdoc OpenAPI Swagger UI
- JUnit 5

## 시스템 아키텍처

```text
사용자 브라우저
   |
   | Thymeleaf 화면 / JavaScript API 요청
   v
Spring MVC Controller / REST Controller
   |
   | 인증/인가: Spring Security Filter Chain, @PreAuthorize
   v
Service Layer
   |
   | JPA / MyBatis / JdbcTemplate
   v
PostgreSQL / Redis / 외부 API(OpenAI, Google Calendar)
```

### 주요 설계 특징

- 화면은 Thymeleaf로 제공하고, 데이터 처리는 `/api/**` REST API와 JavaScript 호출로 분리했습니다.
- 인증은 서버 세션에 의존하지 않는 JWT 기반 구조로 구현했습니다.
- Access Token은 API 요청 인증에 사용하고, Refresh Token은 HttpOnly Cookie와 DB 해시 저장 방식으로 관리합니다.
- USER, ADMIN, SUPER 역할에 따라 API와 화면 접근 권한을 제어합니다.
- 감사 로그는 AOP로 수집하고 Redis에 우선 적재한 뒤 스케줄러로 감사 로그 전용 DB에 이관합니다.
- 실시간 채팅은 WebSocket/STOMP 기반으로 구성하고, CONNECT 단계에서 JWT를 검증합니다.
- AI 기능은 일정, 프로젝트, 업무 데이터를 컨텍스트로 사용해 채팅방 내 질의응답과 요약을 제공합니다.

## 프로젝트 구조

```text
src/main/java/office_mate_2605
├── board                  # 게시판, 댓글, 첨부파일
├── calender               # 캘린더 및 Google Calendar 연동
├── chat                   # WebSocket 채팅, AI 응답, 메시지 초안
├── common
│   ├── controller         # 화면 진입 Controller
│   └── employee           # 직원, 조직도, 재직 상태 신청
├── config                 # Security, Redis, WebSocket, AI, DB 설정
├── dashboard              # 대시보드 통계, 알림
├── mail                   # 메일 발송
├── management
│   ├── audit_log          # 감사 로그 수집, 이관, 조회
│   └── auth               # 비밀번호, 계정 잠금, 인증 관련 API
├── project                # 프로젝트, 멤버, 업무(Task)
├── security               # JWT 필터, 로그인 핸들러, 토큰 서비스
└── util                   # JWT, BCrypt, 파일 업로드 유틸
```

## 팀 구성 및 구현 범위

| 담당자 | 주요 담당 영역 | 구현 내용 |
| --- | --- | --- |
| 서주연 | 인증/인가, 직원 관리, 계정 보안, 감사 로그 | Spring Security JWT 인증, Refresh Token HttpOnly Cookie/DB 해시 저장, 직원 등록/수정/삭제, 조직도, 휴직/복직/퇴사 신청 및 승인, 계정 잠금/해제, 임시 비밀번호, RBAC 권한 제어, AOP/Redis/Scheduler 기반 감사 로그 |
| 강수현 | 프로젝트, 업무(Task), 캘린더, 대시보드, 알림 | 프로젝트 생성/조회/상태 관리, 프로젝트 멤버 초대/삭제, 업무 할당/진척도 관리, 대시보드 통계, 업무 알림, 내부 캘린더, Google Calendar OAuth 및 일정 내보내기 |
| 박재경 | 실시간 채팅, Redis 초안, AI 서비스 | WebSocket/STOMP 채팅, ChannelInterceptor 기반 JWT 인증, 1:1/그룹 채팅방, 메시지 저장/읽음 처리, Redis 초안 저장, OpenAI 기반 `@AI` 질의응답 및 채팅 요약 |
| 서민성 | 게시판, 댓글, 첨부파일 | 공지사항/자유게시판 목록, 검색/페이징, 게시글 작성/수정/삭제, 공지 상단 고정, 댓글 작성/삭제, 파일 첨부 및 다운로드 |

## 핵심 기능 상세

### 인증 및 계정 보안

- `APILoginFilter`를 통한 JSON 기반 로그인
- `TokenCheckFilter`를 통한 Access Token 검증
- `RefreshTokenFilter`를 통한 Access Token 재발급
- Refresh Token HttpOnly Cookie 저장
- Refresh Token DB 해시 저장 및 revoke 처리
- 로그아웃, 비밀번호 변경, 휴직/퇴사 시 기존 Refresh Token 폐기
- 로그인 실패 횟수 누적 및 계정 잠금
- 임시 비밀번호 발급, Redis 임시 저장, 비밀번호 재설정

### 직원 및 권한 관리

- 직원 등록, 수정, 논리 삭제
- 사번 및 이메일 중복 확인
- 부서, 직급, 재직 상태, 시스템 권한 관리
- 조직도 조회 및 부서/상태 필터링
- 휴직/복직/퇴사 신청
- SUPER 권한 기반 신청 승인/반려
- 최소 1명의 ACTIVE SUPER 계정 유지 정책
- `@PreAuthorize` 기반 API 권한 제어
- 공통 layout 기반 권한별 메뉴 노출 및 관리자 화면 직접 URL 접근 차단

### 감사 로그

- 로그인 성공/실패 및 주요 시스템 행위 기록
- 감사 로그 대상 API 중앙 정책 Registry 관리
- AOP 기반 Controller 요청 감지
- Redis Queue 기반 감사 로그 임시 저장
- 스케줄러 기반 감사 로그 DB 이관
- Redis 저장 실패 시 fallback 테이블 저장 및 재이관
- 감사 로그 조건 검색, 상세 조회, CSV 내보내기

### 프로젝트 및 업무

- 프로젝트 대시보드 조회
- 프로젝트 생성, 상태 변경, 논리 삭제
- 프로젝트 멤버 검색, 초대, 조회, 삭제
- 담당 업무 조회
- 팀원별 업무 조회
- 업무 등록, 수정, 삭제
- 업무 진척도 및 상태 관리
- 프로젝트/업무 할당 알림 생성

### 캘린더 및 외부 연동

- 개인/부서/프로젝트/업무 일정 조회
- 일정 등록, 수정, 삭제
- 일정 색상 및 범위 관리
- Google OAuth 인증
- 시스템 일정을 Google Calendar로 내보내기

### 실시간 채팅 및 AI

- 참여 채팅방 목록 조회
- 1:1 채팅방 및 그룹 채팅방 생성
- WebSocket/STOMP 기반 실시간 메시지 송수신
- 메시지 DB 저장
- 읽음 위치 업데이트
- 안 읽은 메시지 수 계산
- Redis 기반 메시지 초안 1시간 저장
- 채팅방 재접속 시 초안 복구
- `@AI` 키워드 기반 일정/프로젝트 질의응답
- 선택 메시지 기반 업무 요약

### 게시판

- 전체 게시글 조회
- 공지사항/자유게시판 목록 조회
- 제목/작성자 기준 검색 및 페이징
- 게시글 작성, 상세 조회, 수정, 삭제
- 공지사항 상단 고정
- 댓글 작성 및 삭제
- 첨부파일 업로드 및 다운로드

## 실행 방법

### 사전 준비

- Java 21
- PostgreSQL
- Redis
- OpenAI API Key
- Google Calendar API credentials

### 환경 설정

`src/main/resources/application.properties`에서 로컬 환경에 맞게 다음 값을 설정합니다.

```properties
spring.datasource.pgvector.url=jdbc:postgresql://localhost:5432/office_mate_2605
spring.datasource.pgvector.username=postgres
spring.datasource.pgvector.password=postgres

spring.datasource.audit.url=jdbc:postgresql://localhost:5432/office_mate_audit_log
spring.datasource.audit.username=postgres
spring.datasource.audit.password=postgres

spring.data.redis.host=localhost
spring.data.redis.port=6379

spring.ai.openai.api-key=${OPENAI_API_KEY}
```

Google Calendar 연동을 사용하려면 Google Cloud Console에서 OAuth Client를 생성하고, credentials 파일을 리소스 경로에 배치해야 합니다.

### 실행

```bash
./gradlew bootRun
```

애플리케이션 실행 후 브라우저에서 접속합니다.

```text
http://localhost:8080
```

### 테스트

```bash
./gradlew test
```

## API 문서

Springdoc OpenAPI Swagger UI를 통해 API 명세를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

## 프로젝트 산출물

- 요구사항 정의서
- 유스케이스 명세서
- ERD 및 데이터베이스 정의서
- API 명세서
- 테스트 시트
- 기능 설명서

## 회고

Office Mate는 짧은 개발 기간 안에서 인증/인가, 실시간 통신, 외부 API 연동, AI 기능, 감사 로그, 인사/프로젝트/게시판 도메인을 함께 구현한 통합 그룹웨어 프로젝트입니다.

단순한 CRUD 구현을 넘어 토큰 저장 위치, 계정 상태 변경, WebSocket 인증, 감사 로그 비동기 처리처럼 실제 서비스 운영에서 발생할 수 있는 보안과 안정성 문제를 직접 다루었습니다. 이를 통해 기능 구현뿐 아니라 시스템 설계, 협업, 트러블슈팅, 문서화의 중요성을 함께 경험했습니다.
