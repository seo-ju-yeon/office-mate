# PostgreSQL SQL 실행 가이드

## 실행 순서

IntelliJ Ultimate의 Database 탭에서 PostgreSQL 데이터소스를 만든 뒤 아래 순서로 실행한다.

1. `00_create_databases.sql`
    - `main_db`, `audit_dh` 데이터베이스를 만든다.
    - 이 파일은 `postgres` 기본 DB에 연결해서 실행한다.

2. `01_main_db_schema.sql`
    - 직원, 권한, 게시판, 캘린더, 채팅, 프로젝트, AI 지식 테이블을 만든다.
    - 이 파일은 `main_db`에 연결해서 실행한다.

3. `02_audit_db_schema.sql`
    - AOP/Redis/Batch로 쌓을 감사 로그 테이블을 만든다.
    - 이 파일은 `audit_dh`에 연결해서 실행한다.

4. `03_pgvector_optional.sql`
    - PGVector가 설치되어 있을 때만 실행한다.
    - RAG 검색용 벡터 컬럼과 인덱스를 추가한다.

5. `04_seed_portfolio_data.sql`
    - 포트폴리오 시연용 직원, 게시판, 기본 권한, 공지글을 넣는다.
    - 이 파일은 `main_db`에 연결해서 실행한다.

## 테이블 관리 기준

- 단순 CRUD: JPA 사용
    - `employee`, `board`, `post`, `post_comment`, `meeting_room`, `refresh_token`

- JOIN/집계/검색: MyBatis 사용
    - 게시글 목록 + 작성자 + 첨부 수
    - 부서 x 직급 권한 조회
    - 대시보드 일정/태스크 조회
    - 프로젝트 진척도 집계
    - AI RAG 검색 결과 조합

## 왜 DB를 둘로 나누는가

- `main_db`: 실제 업무 데이터
- `audit_dh`: 감사 로그 전용 데이터

감사 로그 DB에 장애가 생겨도 직원 등록, 게시글 작성, 채팅 같은 메인 기능이 멈추면 안 되므로 분리한다.

## main_db 주요 테이블

- 기준 정보: `departments`, `position_rank`, `roles`, `permissions`, `role_permissions`
- 직원/권한: `employee`, `employee_roles`
- 개인정보: `privacy_policies`, `privacy_consents`
- 게시판: `board`, `post`, `post_comment`, `post_attachment`
- 일정/캘린더: `google_calendar_link`, `calendar_event`
- 채팅: `chat_room`, `chat_room_member`, `chat_message`
- 프로젝트: `project`, `project_member`, `project_task`
- AI/업무/알림: `meeting_room`, `meeting_reservation`, `work_log`, `notification`, `ai_knowledge`

## PostgreSQL 초보 주의점

- `bigserial`: 자동 증가 숫자 PK이다.
- `timestamptz`: 타임존이 포함된 시간 타입이다. 서버/사용자 시간대 차이를 줄인다.
- `enum`: 정해진 값만 넣을 수 있는 타입이다. 상태값, 부서, 직급처럼 고정된 값에 적합하다.
- `REFERENCES`: 외래키이다. 예를 들어 게시글 작성자는 반드시 `employee`에 존재해야 한다.
- `CHECK`: 데이터 규칙이다. 예를 들어 일정 종료 시간이 시작 시간보다 늦어야 한다.
- `deleted_at`: 실제 DELETE 대신 삭제 시각만 넣는 soft delete 용도이다.
- `COMMENT ON COLUMN`: 컬럼 설명이다. IntelliJ Database 창에서 테이블/컬럼 정보를 볼 때 설명으로 확인할 수 있다.
