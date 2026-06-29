# SQL 실행 가이드

Office Mate는 메인 업무 DB와 감사 로그 DB를 분리해 사용한다.

- `office_mate_2605`: 직원, 인증, 권한, 게시판, 일정, 채팅, 프로젝트, 알림 등 업무 데이터
- `office_mate_audit_log`: AOP/Redis/Scheduler 기반 감사 로그와 Redis 장애 fallback 로그

## 파일 구성

| 파일 | 실행 대상 DB | 설명 |
| --- | --- | --- |
| `01_init.sql` | `postgres` | `office_mate_2605`, `office_mate_audit_log` 데이터베이스 생성 |
| `02_main_db.sql` | `office_mate_2605` | 메인 업무 DB 스키마 생성 |
| `03_audit_db.sql` | `office_mate_audit_log` | 감사 로그 전용 DB 스키마 생성 |
| `04_main_dummy.sql` | `office_mate_2605` | 로그인/권한/계정 보안/상태 신청/업무 화면 확인용 더미 데이터 |
| `05_audit_log_dummy.sql` | `office_mate_audit_log` | 감사 로그 조회 화면 확인용 더미 데이터 |

## 실행 순서

1. PostgreSQL 기본 DB인 `postgres`에 연결한 뒤 `01_init.sql`을 실행한다.
2. `office_mate_2605` 데이터소스에 연결한 뒤 `02_main_db.sql`을 실행한다.
3. `office_mate_audit_log` 데이터소스에 연결한 뒤 `03_audit_db.sql`을 실행한다.
4. 메인 화면 확인용 샘플 데이터가 필요하면 `office_mate_2605`에서 `04_main_dummy.sql`을 실행한다.
5. 감사 로그 화면 확인용 샘플 데이터가 필요하면 `office_mate_audit_log`에서 `05_audit_log_dummy.sql`을 실행한다.

## 더미 계정

`04_main_dummy.sql`에는 로컬 실행과 포트폴리오 시연을 위한 테스트 계정이 포함되어 있다.

- 테스트 계정: `SUPER001`, `ADMIN001`, `BE001`, `FE001`, `MS001`
- 공통 임시 비밀번호: `1111`
- `employee.password`에는 `1111`의 BCrypt 해시가 저장되어 있다.
- 최초 로그인 시 `temp_password_required = true` 상태이므로 비밀번호 변경 화면으로 이동한다.

이 값들은 시연용 더미 데이터이며 실제 운영 계정이나 실제 비밀번호가 아니다.

## 주요 테이블

### 메인 업무 DB

- 기준 정보: `departments`, `position_rank`, `roles`
- 직원/권한/계정 보안: `employee`, `employee_roles`, `account_security_status`, `employee_status_request`, `refresh_token`
- 게시판: `board`, `post`, `post_attachment`, `post_comment`
- 일정: `google_calendar_link`, `calendar_event`
- 채팅/AI: `chat_room`, `chat_room_ai_config`, `chat_room_member`, `chat_message`
- 프로젝트/알림: `project`, `project_member`, `project_task`, `notification`

### 감사 로그 DB

- `audit_log`: Redis 큐 또는 fallback 로그에서 이관된 감사 로그
- `audit_fallback_log`: Redis 저장 실패 시 원본 이벤트를 임시 보관하는 fallback 로그

## 주석 관리 기준

- 파일 상단 주석은 실행 대상 DB, 실행 순서, 포함 내용, 주의사항을 안내한다.
- 컬럼 옆 인라인 주석은 SQL을 읽을 때 바로 필요한 짧은 의미만 남긴다.
- `COMMENT ON TABLE`과 `COMMENT ON COLUMN`은 IntelliJ Database 창에서 확인할 수 있는 DB 메타 설명으로 사용한다.
- 더미 데이터 주석은 어떤 화면이나 기능 검증에 필요한 데이터인지 중심으로 작성한다.

## PostgreSQL 초보 주의점

- `bigserial`: 자동 증가 숫자 PK이다.
- `timestamptz`: 타임존이 포함된 시간 타입이다.
- `enum`: 부서, 직급, 상태처럼 정해진 값만 허용하는 타입이다.
- `REFERENCES`: 외래키이다.
- `CHECK`: 데이터 입력 규칙이다.
- `deleted_at`: 실제 DELETE 대신 삭제 시각을 기록하는 soft delete 용도이다.
- `revoked_at`: Refresh Token을 물리 삭제하지 않고 폐기 처리한 시각을 기록하는 컬럼이다.
