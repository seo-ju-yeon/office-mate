-- 파일 목적: 메인 업무 DB 스키마를 생성한다.
-- 실행 대상 DB: office_mate_2605
-- 실행 순서: 2
-- 포함 내용: 직원/권한/계정 보안/게시판/일정/채팅/프로젝트/알림/Refresh Token 테이블
-- 주의사항: 01_init.sql 실행 후 office_mate_2605 데이터소스를 선택한 뒤 실행한다.

CREATE TYPE department_code AS ENUM (
    'FRONTEND', -- 프론트엔드 팀
    'BACKEND', -- 백엔드 팀
    'MANAGEMENT_SUPPORT', -- 경영지원 팀
    'MARKETING', -- 마케팅 팀
    'DB_MANAGEMENT' -- DB관리 팀
    );

CREATE TYPE position_code AS ENUM (
    'STAFF', -- 사원
    'ASSISTANT_MANAGER', -- 대리
    'MANAGER', -- 과장
    'GENERAL_MANAGER', -- 부장
    'DEPUTY_GENERAL_MANAGER', -- 차장
    'CEO' -- 사장
    );

CREATE TYPE system_role AS ENUM (
    'USER', -- 일반 사용자
    'ADMIN', -- 관리자. 직원 등록, 게시글 관리 등 관리자 기능 일부 가능
    'SUPER' -- 최고 관리자. 시스템 전체 관리 권한
    );

CREATE TYPE employment_status AS ENUM (
    'ACTIVE', -- 재직
    'ON_LEAVE', -- 휴직
    'RESIGNED' -- 퇴사
    );

CREATE TYPE employment_request_type AS ENUM (
    'LEAVE', -- 휴직 신청
    'RESIGN', -- 퇴사 신청
    'RETURN_FROM_LEAVE' -- 복직 신청
    );

CREATE TYPE request_status AS ENUM (
    'PENDING', -- 승인 대기
    'APPROVED', -- 승인 완료
    'REJECTED' -- 반려
    );

CREATE TYPE board_type AS ENUM (
    'NOTICE', -- 공지사항 게시판
    'GENERAL' -- 일반 게시판
    );

CREATE TYPE post_status AS ENUM (
    'PUBLISHED', -- 정상 게시
    'DELETED' -- 삭제 처리
    );

CREATE TYPE project_status AS ENUM (
    'READY', -- 준비
    'IN_PROGRESS', -- 진행 중
    'DELAYED', -- 지연
    'ON_HOLD', -- 보류
    'DONE', -- 완료
    'CANCELED' -- 취소
    );

CREATE TYPE task_status AS ENUM (
    'TODO', -- 할 일
    'IN_PROGRESS', -- 진행 중
    'DONE' -- 완료
    );

CREATE TYPE calendar_scope AS ENUM (
    'PERSONAL', -- 개인 일정
    'PROJECT' -- 프로젝트 일정
    );

CREATE TYPE notification_ref_type AS ENUM (
    'PROJECT', -- 프로젝트
    'PROJECT_TASK' -- 프로젝트 업무
    );

-- 캘린더 반복 설정 Enum
CREATE TYPE recurrence_type AS ENUM (
    'DAILY', -- 매일
    'WEEKLY', -- 매주
    'MONTHLY' -- 매달
    );

-- 캘린더 색상 Enum (구글 캘린더 색상 인덱스 기준 명칭)
CREATE TYPE calendar_color AS ENUM (
    'LAVENDER', 'SAGE', 'GRAPE', 'FLAMINGO', 'BANANA',
    'TANGERINE', 'PEACOCK', 'GRAPHITE', 'BLUEBERRY', 'BASIL', 'TOMATO'
    );

CREATE TABLE departments
(
    code       department_code PRIMARY KEY,   -- 부서 코드. department_code enum 값을 그대로 사용한다.
    name       varchar(50) NOT NULL UNIQUE,   -- 부서 표시명. 예: 프론트엔드팀, 백엔드팀.
    sort_order smallint    NOT NULL DEFAULT 0 -- 화면 표시 순서. 숫자가 작을수록 먼저 표시한다.
);

COMMENT ON TABLE departments IS '부서 마스터. enum 기반 코드와 화면 표시명을 함께 관리한다.';
COMMENT ON COLUMN departments.code IS '부서 코드. employee.department와 FK로 연결된다.';
COMMENT ON COLUMN departments.name IS '부서 표시명.';
COMMENT ON COLUMN departments.sort_order IS '부서 표시 순서.';

INSERT INTO departments (code, name, sort_order)
VALUES ('FRONTEND', '프론트엔드팀', 1),
       ('BACKEND', '백엔드팀', 2),
       ('MANAGEMENT_SUPPORT', '경영지원팀', 3),
       ('MARKETING', '마케팅팀', 4),
       ('DB_MANAGEMENT', 'DB관리팀', 5);

CREATE TABLE position_rank
(
    position   position_code PRIMARY KEY, -- 직급 코드. position_code enum 값을 사용한다.
    rank_order smallint NOT NULL UNIQUE   -- 직급 순서. 숫자가 클수록 높은 직급으로 판단한다.
);

COMMENT ON TABLE position_rank IS '직급 enum의 업무상 순서를 보관한다.';
COMMENT ON COLUMN position_rank.position IS '직급 코드. position_code enum 값을 사용한다.';
COMMENT ON COLUMN position_rank.rank_order IS '직급 순서. 숫자가 클수록 높은 직급으로 판단한다.';

INSERT INTO position_rank (position, rank_order)
VALUES ('STAFF', 1),  -- 사원
       ('ASSISTANT_MANAGER', 2),  -- 대리
       ('MANAGER', 3),  -- 과장
       ('GENERAL_MANAGER', 4),  -- 부장
       ('DEPUTY_GENERAL_MANAGER', 5),  -- 차장
       ('CEO', 6);  -- 사장

CREATE TABLE roles
(
    code        system_role PRIMARY KEY,     -- 시스템 역할 코드. USER, ADMIN, SUPER 중 하나이다.
    name        varchar(50) NOT NULL UNIQUE, -- 역할 표시명. 예: 일반 사용자, 관리자, 최고 관리자.
    description varchar(255)                 -- 역할 설명.
);

COMMENT ON TABLE roles IS '시스템 전역 역할 마스터. Spring Security 권한과 연결된다.';
COMMENT ON COLUMN roles.code IS '시스템 역할 코드.';
COMMENT ON COLUMN roles.name IS '역할 표시명.';
COMMENT ON COLUMN roles.description IS '역할 설명.';

INSERT INTO roles (code, name, description)
VALUES ('USER', '일반 사용자', '일반 직원 기본 권한'),
       ('ADMIN', '관리자', '직원 등록 및 게시판 관리 권한'),
       ('SUPER', '최고 관리자', '시스템 전체 관리 권한');

CREATE TABLE employee
(
    employee_no            varchar(30) PRIMARY KEY,                                           -- 회사에서 지급한 사번. 로그인 ID이며 변경 불가이다.
    password               varchar(255)      NOT NULL,                                        -- BCrypt 등으로 암호화한 비밀번호 해시. 임시 비밀번호 1111도 평문 저장 금지이다.
    temp_password_required boolean           NOT NULL DEFAULT true,                           -- 임시 비밀번호 변경 필요 여부. true면 최초 로그인 후 비밀번호 변경을 강제한다.
    name                   varchar(50)       NOT NULL,                                        -- 직원 이름.
    email                  varchar(120)      NOT NULL UNIQUE,                                 -- 직원 이메일. 중복 불가이며 알림/계정 복구/Google 연동에 사용한다.
    department             department_code   NOT NULL REFERENCES departments (code),          -- 소속 부서. departments.code를 참조한다.
    position               position_code     NOT NULL,                                        -- 직급. STAFF, MANAGER, CEO 등 position_code enum 값을 사용한다.
    role                   system_role       NOT NULL DEFAULT 'USER' REFERENCES roles (code), -- 시스템 전역 역할. USER, ADMIN, SUPER 중 하나이다.
    status                 employment_status NOT NULL DEFAULT 'ACTIVE',                       -- 재직 상태. ACTIVE 재직, ON_LEAVE 휴직, RESIGNED 퇴사.
    joined_on              date              NOT NULL,                                        -- 입사일.
    resigned_on            date,                                                              -- 퇴사일. 재직자는 NULL이다.
    deleted_at             timestamptz                                                        -- 논리 삭제 시각. 실제 DELETE 대신 값이 있으면 삭제 데이터로 취급한다.
);

COMMENT ON TABLE employee IS '직원 계정 및 인사 기본 정보. employee_no는 사번 기반 로그인 ID이다.';
COMMENT ON COLUMN employee.employee_no IS '회사에서 지급한 사번. 로그인 ID로 사용하며 변경 불가이다.';
COMMENT ON COLUMN employee.password IS 'BCrypt 등으로 암호화한 비밀번호 해시. 1111 같은 임시 비밀번호도 평문 저장 금지이다.';
COMMENT ON COLUMN employee.temp_password_required IS '임시 비밀번호 변경 필요 여부. true면 비밀번호 변경 전까지 주요 기능 접근을 제한한다.';
COMMENT ON COLUMN employee.name IS '직원 이름.';
COMMENT ON COLUMN employee.email IS '직원 이메일. 로그인 알림, 비밀번호 변경, Google 연동 등에 사용한다.';
COMMENT ON COLUMN employee.department IS '소속 부서. department_code enum 값을 사용한다.';
COMMENT ON COLUMN employee.position IS '직급. position_code enum 값을 사용한다.';
COMMENT ON COLUMN employee.role IS '시스템 전역 역할. USER, ADMIN, SUPER 중 하나이다.';
COMMENT ON COLUMN employee.status IS '재직 상태. ACTIVE 재직, ON_LEAVE 휴직, RESIGNED 퇴사.';
COMMENT ON COLUMN employee.joined_on IS '입사일.';
COMMENT ON COLUMN employee.resigned_on IS '퇴사일. 재직자는 NULL이다.';
COMMENT ON COLUMN employee.deleted_at IS '논리 삭제 시각. 실제 DELETE 대신 값이 들어가면 삭제된 데이터로 취급한다.';

CREATE INDEX idx_employee_department ON employee (department);
CREATE INDEX idx_employee_status ON employee (status);

CREATE TABLE employee_roles
(
    employee_no varchar(30) NOT NULL REFERENCES employee (employee_no),
    role_code   system_role NOT NULL REFERENCES roles (code),
    granted_at  timestamptz NOT NULL DEFAULT now(),
    granted_by  varchar(30) REFERENCES employee (employee_no),
    PRIMARY KEY (employee_no, role_code)
);

COMMENT ON TABLE employee_roles IS '직원별 시스템 역할 매핑 테이블. employee.role과 함께 Spring Security 권한 조회에 사용한다.';
COMMENT ON COLUMN employee_roles.employee_no IS '역할을 부여받은 직원 사번.';
COMMENT ON COLUMN employee_roles.role_code IS '부여된 시스템 역할. USER, ADMIN, SUPER 중 하나이다.';
COMMENT ON COLUMN employee_roles.granted_at IS '역할이 부여된 시각.';
COMMENT ON COLUMN employee_roles.granted_by IS '역할을 부여한 관리자 사번. 시스템 초기 데이터는 자기 자신 또는 NULL 가능하다.';

CREATE INDEX idx_employee_roles_role_code ON employee_roles (role_code);


CREATE TABLE account_security_status
(
    employee_no      varchar(30) PRIMARY KEY REFERENCES employee (employee_no), -- 보안 상태를 관리할 직원 사번.
    login_fail_count integer     NOT NULL DEFAULT 0,                            -- 연속 로그인 실패 횟수.
    account_locked   boolean     NOT NULL DEFAULT false,                        -- 계정 잠금 여부. true이면 로그인할 수 없다.
    last_failed_at   timestamptz,                                               -- 마지막 로그인 실패 시각.
    locked_at        timestamptz,                                               -- 로그인 실패 5회 이상으로 잠긴 시각.
    unlocked_at      timestamptz,                                               -- 관리자가 잠금을 해제한 시각.
    unlocked_by      varchar(30) REFERENCES employee (employee_no),             -- 잠금을 해제한 관리자 사번.
    created_at       timestamptz NOT NULL DEFAULT now(),                        -- 보안 상태 생성 시각.
    updated_at       timestamptz NOT NULL DEFAULT now()                         -- 보안 상태 마지막 변경 시각.
);

COMMENT ON TABLE account_security_status IS '직원 계정의 로그인 실패 횟수와 잠금 상태를 관리하는 보안 상태 테이블.';
COMMENT ON COLUMN account_security_status.employee_no IS '보안 상태를 관리할 직원 사번.';
COMMENT ON COLUMN account_security_status.login_fail_count IS '연속 로그인 실패 횟수. 정상 로그인 또는 관리자 잠금 해제 시 0으로 초기화한다.';
COMMENT ON COLUMN account_security_status.account_locked IS '계정 잠금 여부. true이면 관리자가 해제하기 전까지 로그인할 수 없다.';
COMMENT ON COLUMN account_security_status.last_failed_at IS '마지막 로그인 실패 시각.';
COMMENT ON COLUMN account_security_status.locked_at IS '로그인 실패 횟수가 기준을 초과하여 계정이 잠긴 시각.';
COMMENT ON COLUMN account_security_status.unlocked_at IS '관리자가 계정 잠금을 해제한 시각.';
COMMENT ON COLUMN account_security_status.unlocked_by IS '계정 잠금을 해제한 관리자 사번.';
COMMENT ON COLUMN account_security_status.created_at IS '보안 상태 생성 시각.';
COMMENT ON COLUMN account_security_status.updated_at IS '보안 상태 마지막 변경 시각.';

CREATE INDEX idx_account_security_status_locked ON account_security_status (account_locked)
    WHERE account_locked = true;

CREATE TABLE employee_status_request
(
    id               bigserial PRIMARY KEY,                                              -- 재직 상태 변경 신청 PK. 자동 증가 값이다.
    employee_no      varchar(30)             NOT NULL REFERENCES employee (employee_no), -- 신청한 직원 사번.
    request_type     employment_request_type NOT NULL,                                   -- 신청 유형. LEAVE 휴직, RESIGN 퇴사, RETURN_FROM_LEAVE 복직.
    reason           text,                                                               -- 신청 사유.
    requested_at     timestamptz             NOT NULL DEFAULT now(),                     -- 신청 시각.
    status           request_status          NOT NULL DEFAULT 'PENDING',                 -- 신청 처리 상태. PENDING, APPROVED, REJECTED 중 하나이다.
    decided_by       varchar(30) REFERENCES employee (employee_no),                      -- 승인/반려 처리자 사번.
    decided_at       timestamptz,                                                        -- 승인/반려 처리 시각.
    decision_comment text                                                                -- 승인/반려 처리 코멘트.
);

COMMENT ON TABLE employee_status_request IS '직원의 휴직/퇴사 신청 및 승인 이력.';
COMMENT ON COLUMN employee_status_request.id IS '재직 상태 변경 신청 PK. 자동 증가 값이다.';
COMMENT ON COLUMN employee_status_request.employee_no IS '휴직/퇴사를 신청한 직원 사번.';
COMMENT ON COLUMN employee_status_request.request_type IS '신청 유형. LEAVE 휴직, RESIGN 퇴사, RETURN_FROM_LEAVE 복직.';
COMMENT ON COLUMN employee_status_request.reason IS '직원이 작성한 신청 사유.';
COMMENT ON COLUMN employee_status_request.requested_at IS '신청 시각.';
COMMENT ON COLUMN employee_status_request.status IS '신청 처리 상태. PENDING, APPROVED, REJECTED 중 하나이다.';
COMMENT ON COLUMN employee_status_request.decided_by IS '승인 또는 반려를 처리한 관리자 사번.';
COMMENT ON COLUMN employee_status_request.decided_at IS '승인 또는 반려 처리 시각.';
COMMENT ON COLUMN employee_status_request.decision_comment IS '승인 또는 반려 처리 코멘트.';

CREATE INDEX idx_employee_status_request_employee ON employee_status_request (employee_no, requested_at DESC);
CREATE INDEX idx_employee_status_request_status ON employee_status_request (status, requested_at DESC);

CREATE TABLE board
(
    id         bigserial PRIMARY KEY,             -- 게시판 PK. 자동 증가 값이다.
    type       board_type  NOT NULL,              -- 게시판 유형. NOTICE는 공지사항, GENERAL은 일반 게시판이다.
    name       varchar(80) NOT NULL,              -- 게시판 이름. 예: 공지사항, 자유게시판.
    is_active  boolean     NOT NULL DEFAULT true, -- 게시판 사용 여부. false면 화면에서 숨기거나 작성 제한한다.
    project_id bigint,                             -- 연결된 프로젝트 ID. NULL이면 전사 게시판, NOT NULL이면 프로젝트 전용 게시판이다.
    deleted_at timestamptz,                       -- 논리 삭제 시각. 프로젝트 마감(DONE/CANCELED) 시 채워진다. NULL이면 정상 상태이다.
    UNIQUE (type, name, project_id)               -- 같은 유형 + 같은 프로젝트 안에서 게시판 이름 중복을 허용하지 않는다.
);

COMMENT ON TABLE board IS '공지사항/일반/프로젝트 게시판 마스터.';
COMMENT ON COLUMN board.id IS '게시판 PK. 자동 증가 값이다.';
COMMENT ON COLUMN board.type IS '게시판 유형. NOTICE는 공지사항, GENERAL은 일반 게시판이다.';
COMMENT ON COLUMN board.name IS '게시판 이름. 예: 공지사항, 자유게시판, 프로젝트A 공지.';
COMMENT ON COLUMN board.is_active IS '게시판 사용 여부. false면 화면에서 숨기거나 작성 제한한다.';
COMMENT ON COLUMN board.project_id IS '연결된 프로젝트 ID. NULL이면 전사 게시판, NOT NULL이면 프로젝트 전용 게시판이다.';
COMMENT ON COLUMN board.deleted_at IS '논리 삭제 시각. 프로젝트 마감(DONE/CANCELED) 시 채워진다. NULL이면 정상 상태이다.';

CREATE INDEX idx_board_project ON board (project_id);

INSERT INTO board (type, name)
VALUES ('NOTICE', '공지사항'),
       ('GENERAL', '자유게시판')
ON CONFLICT (type, name, project_id) DO NOTHING;

CREATE TABLE post
(
    id         bigserial PRIMARY KEY,                                   -- 게시글 PK. 자동 증가 값이다.
    board_id   bigint       NOT NULL REFERENCES board (id),             -- 게시글이 속한 게시판 ID.
    author_no  varchar(30)  NOT NULL REFERENCES employee (employee_no), -- 게시글 작성자 사번.
    editor_no  varchar(30)  NULL REFERENCES employee (employee_no),     -- 게시글 수정자 사번. 수정 전이면 NULL이다.
    title      varchar(200) NOT NULL,                                   -- 게시글 제목.
    content    text         NOT NULL,                                   -- 게시글 본문.
    is_pinned  boolean      NOT NULL DEFAULT false,                     -- 공지 상단 고정 여부. 주로 NOTICE 게시판에서 사용한다.
    view_count integer      NOT NULL DEFAULT 0,                         -- 게시글 조회수.
    status     post_status  NOT NULL DEFAULT 'PUBLISHED',               -- 게시글 상태. 게시, 삭제 상태를 구분한다.
    deleted_by varchar(30) REFERENCES employee (employee_no),           -- 삭제 처리한 직원 사번. 관리자 삭제 추적에 사용한다.
    posted_at  timestamptz  NOT NULL DEFAULT now(),                     -- 게시글 최초 작성 시각.
    edited_at  timestamptz,                                             -- 게시글 마지막 수정 시각. 수정 전이면 NULL이다.
    deleted_at timestamptz                                              -- 게시글 논리 삭제 시각. 삭제 전이면 NULL이다.
);

COMMENT ON TABLE post IS '게시글. 단순 CRUD는 JPA, 목록/작성자/첨부 조인은 MyBatis 사용 권장.';
COMMENT ON COLUMN post.id IS '게시글 PK. 자동 증가 값이다.';
COMMENT ON COLUMN post.board_id IS '게시글이 속한 게시판 ID.';
COMMENT ON COLUMN post.author_no IS '게시글 작성자 사번.';
COMMENT ON COLUMN post.editor_no IS '게시글 수정자 사번.';
COMMENT ON COLUMN post.title IS '게시글 제목.';
COMMENT ON COLUMN post.content IS '게시글 본문.';
COMMENT ON COLUMN post.is_pinned IS '공지 상단 고정 여부.';
COMMENT ON COLUMN post.view_count IS '게시글 조회수.';
COMMENT ON COLUMN post.status IS '게시글 상태. 게시, 삭제 상태를 구분한다.';
COMMENT ON COLUMN post.deleted_by IS '삭제 처리한 직원 사번. NULL이면 본인 삭제 또는 미삭제 상태이다.';
COMMENT ON COLUMN post.posted_at IS '게시글 최초 작성 시각.';
COMMENT ON COLUMN post.edited_at IS '게시글 마지막 수정 시각. 수정 전이면 NULL이다.';
COMMENT ON COLUMN post.deleted_at IS '게시글 논리 삭제 시각. 삭제 전이면 NULL이다.';

CREATE INDEX idx_post_board_posted_at ON post (board_id, posted_at DESC);
CREATE INDEX idx_post_author ON post (author_no);
CREATE INDEX idx_post_pinned ON post (board_id, is_pinned, posted_at DESC);

CREATE TABLE post_attachment
(
    id            bigserial PRIMARY KEY,                      -- 첨부 파일 PK. 자동 증가 값이다.
    post_id       bigint       NOT NULL REFERENCES post (id), -- 첨부 파일이 연결된 게시글 ID.
    original_name varchar(255) NOT NULL,                      -- 사용자가 업로드한 원본 파일명.
    stored_path   varchar(500) NOT NULL,                      -- 서버 또는 클라우드 저장소에 저장된 실제 파일 경로.
    content_type  varchar(100) NOT NULL,                      -- 파일 MIME 타입. 예: image/png, application/pdf.
    file_size     bigint       NOT NULL,                      -- 파일 크기. byte 단위이다.
    uploaded_at   timestamptz  NOT NULL DEFAULT now()         -- 첨부 파일 업로드 시각.
);

COMMENT ON TABLE post_attachment IS '게시글 첨부 파일 메타데이터. 실제 파일은 파일 저장소에 두고 DB에는 경로만 저장한다.';
COMMENT ON COLUMN post_attachment.id IS '첨부 파일 PK. 자동 증가 값이다.';
COMMENT ON COLUMN post_attachment.post_id IS '첨부 파일이 연결된 게시글 ID.';
COMMENT ON COLUMN post_attachment.original_name IS '사용자가 업로드한 원본 파일명.';
COMMENT ON COLUMN post_attachment.stored_path IS '서버 또는 클라우드 저장소에 저장된 실제 파일 경로.';
COMMENT ON COLUMN post_attachment.content_type IS '파일 MIME 타입. 예: image/png, application/pdf.';
COMMENT ON COLUMN post_attachment.file_size IS '파일 크기. byte 단위이다.';
COMMENT ON COLUMN post_attachment.uploaded_at IS '첨부 파일 업로드 시각.';

CREATE TABLE post_comment
(
    id         bigserial PRIMARY KEY,                                   -- 댓글 PK. 자동 증가 값이다.
    post_id    bigint       NOT NULL REFERENCES post (id),              -- 댓글이 작성된 게시글 ID.
    author_no  varchar(30)  NOT NULL REFERENCES employee (employee_no), -- 댓글 작성자 사번.
    content    varchar(200) NOT NULL,                                   -- 댓글 내용. 길이를 제한해 과도한 입력을 방지한다.
    posted_at  timestamptz  NOT NULL DEFAULT now(),                     -- 댓글 작성 시각.
    deleted_at timestamptz                                              -- 댓글 논리 삭제 시각. 삭제 전이면 NULL이다.
);

COMMENT ON TABLE post_comment IS '게시글 댓글.';
COMMENT ON COLUMN post_comment.id IS '댓글 PK. 자동 증가 값이다.';
COMMENT ON COLUMN post_comment.post_id IS '댓글이 작성된 게시글 ID.';
COMMENT ON COLUMN post_comment.author_no IS '댓글 작성자 사번.';
COMMENT ON COLUMN post_comment.content IS '댓글 내용.';
COMMENT ON COLUMN post_comment.posted_at IS '댓글 작성 시각.';
COMMENT ON COLUMN post_comment.deleted_at IS '댓글 논리 삭제 시각. 삭제 전이면 NULL이다.';

CREATE INDEX idx_post_comment_post ON post_comment (post_id, posted_at);

CREATE TABLE google_calendar_link
(
    employee_no             varchar(30) PRIMARY KEY REFERENCES employee (employee_no), -- Google Calendar를 연동한 직원 사번.
    google_calendar_id      varchar(255) NOT NULL,                                     -- Google Calendar API에서 사용하는 캘린더 ID.
    access_token_encrypted  text         NOT NULL,                                     -- 암호화해서 저장한 Google API access token.
    refresh_token_encrypted text         NOT NULL,                                     -- 암호화해서 저장한 Google API refresh token.
    token_expires_at        timestamptz  NOT NULL                                      -- access token 만료 시각.
);

COMMENT ON TABLE google_calendar_link IS '직원별 Google Calendar 연동 정보.';
COMMENT ON COLUMN google_calendar_link.employee_no IS 'Google Calendar를 연동한 직원 사번.';
COMMENT ON COLUMN google_calendar_link.google_calendar_id IS 'Google Calendar API에서 사용하는 캘린더 ID.';
COMMENT ON COLUMN google_calendar_link.access_token_encrypted IS '암호화해서 저장한 Google API access token.';
COMMENT ON COLUMN google_calendar_link.refresh_token_encrypted IS '암호화해서 저장한 Google API refresh token.';
COMMENT ON COLUMN google_calendar_link.token_expires_at IS 'access token 만료 시각.';

CREATE TABLE calendar_event
(
    id               bigserial PRIMARY KEY,
    parent_id        bigint REFERENCES calendar_event (id),
    owner_no         varchar(30) REFERENCES employee (employee_no),
    department       department_code REFERENCES departments (code),
    project_id       bigint,
    created_by       varchar(30) REFERENCES employee (employee_no),
    title            varchar(200)   NOT NULL,
    description      text,
    starts_at        timestamptz    NOT NULL,
    ends_at          timestamptz    NOT NULL,
    is_all_day       boolean        NOT NULL DEFAULT false,
    scope            calendar_scope NOT NULL DEFAULT 'PERSONAL',
    google_event_id  varchar(255),
    color_type       calendar_color NOT NULL DEFAULT 'BLUEBERRY', -- 일정 색상 Enum
    recurrence_rule  recurrence_type,                             -- 반복 주기 Enum (null 허용: 반복 없음)
    recurrence_count integer CHECK (recurrence_count > 0),        -- 반복 횟수

    CHECK (ends_at > starts_at),
    CHECK (scope <> 'PERSONAL' OR owner_no IS NOT NULL)
);

COMMENT ON TABLE calendar_event IS '사내 일정 및 개인 업무 일정.';
COMMENT ON COLUMN calendar_event.id IS '일정 PK. 자동 증가 값이다.';
COMMENT ON COLUMN calendar_event.parent_id IS '반복 일정 그룹화를 위한 부모 일정 ID.';
COMMENT ON COLUMN calendar_event.owner_no IS '일정 소유자 사번.';
COMMENT ON COLUMN calendar_event.department IS '팀/부서 일정 대상 부서.';
COMMENT ON COLUMN calendar_event.project_id IS '프로젝트 일정에 연결된 프로젝트 ID.';
COMMENT ON COLUMN calendar_event.created_by IS '일정을 생성한 직원 사번.';
COMMENT ON COLUMN calendar_event.title IS '일정 제목.';
COMMENT ON COLUMN calendar_event.description IS '일정 상세 설명.';
COMMENT ON COLUMN calendar_event.starts_at IS '일정 시작 시각.';
COMMENT ON COLUMN calendar_event.ends_at IS '일정 종료 시각. starts_at보다 커야 한다.';
COMMENT ON COLUMN calendar_event.is_all_day IS '종일 일정 여부.';
COMMENT ON COLUMN calendar_event.scope IS '일정 범위. PERSONAL, PROJECT';
COMMENT ON COLUMN calendar_event.google_event_id IS 'Google Calendar API에서 발급받은 이벤트 고유 ID';
COMMENT ON COLUMN calendar_event.color_type IS '일정 표시 색상 (Enum)';
COMMENT ON COLUMN calendar_event.recurrence_rule IS '반복 주기 (DAILY, WEEKLY, MONTHLY)';
COMMENT ON COLUMN calendar_event.recurrence_count IS '반복 횟수';

CREATE INDEX idx_calendar_owner_starts_at ON calendar_event (owner_no, starts_at);
CREATE INDEX idx_calendar_department_starts_at ON calendar_event (department, starts_at);
CREATE INDEX idx_calendar_project_starts_at ON calendar_event (project_id, starts_at);

CREATE TABLE chat_room
(
    id       bigserial PRIMARY KEY,         -- 채팅방 PK. 자동 증가 값이다.
    name     varchar(100),                  -- 채팅방 이름. 1:1 채팅방은 NULL로 둘 수 있다.
    is_group boolean NOT NULL DEFAULT false -- 그룹 채팅 여부. false면 1:1 채팅으로 취급한다.
);

COMMENT ON TABLE chat_room IS '사내 메신저 채팅방.';
COMMENT ON COLUMN chat_room.id IS '채팅방 PK. 자동 증가 값이다.';
COMMENT ON COLUMN chat_room.name IS '채팅방 이름. 1:1 채팅방은 NULL로 둘 수 있다.';
COMMENT ON COLUMN chat_room.is_group IS '그룹 채팅 여부. false면 1:1 채팅으로 취급한다.';

-- 채팅방별 AI 설정 및 컨텍스트 관리
-- 채팅방마다 AI 활성화 여부나 대화 요약을 관리합니다.
CREATE TABLE chat_room_ai_config
(
    room_id            bigint PRIMARY KEY REFERENCES chat_room (id),
    is_ai_active       boolean     NOT NULL DEFAULT true,  -- AI 기능 활성화 여부.
    system_prompt      text,                               -- AI 페르소나 및 역할 설정.
    summary_enabled    boolean     NOT NULL DEFAULT false, -- 대화 요약 기능 사용 여부.
    last_summary       text,                               -- 최근까지의 대화 요약 내용 (컨텍스트 유지용).
    last_summarized_id bigint,                             -- 요약에 포함된 마지막 메시지 ID.
    updated_at         timestamptz NOT NULL DEFAULT now()  -- 요약 갱신 시각.
);

COMMENT ON TABLE chat_room_ai_config IS '채팅방별 AI 설정 및 대화 요약 정보 저장.';
COMMENT ON COLUMN chat_room_ai_config.room_id IS 'AI 설정을 적용할 채팅방 ID. chat_room.id와 1:1로 연결된다.';
COMMENT ON COLUMN chat_room_ai_config.is_ai_active IS '채팅방의 AI 기능 활성화 여부.';
COMMENT ON COLUMN chat_room_ai_config.system_prompt IS '채팅방 AI 응답에 적용할 시스템 프롬프트.';
COMMENT ON COLUMN chat_room_ai_config.summary_enabled IS '채팅방 대화 요약 기능 사용 여부.';
COMMENT ON COLUMN chat_room_ai_config.last_summary IS '최근까지의 채팅방 대화 요약 내용.';
COMMENT ON COLUMN chat_room_ai_config.last_summarized_id IS '요약에 포함된 마지막 채팅 메시지 ID.';
COMMENT ON COLUMN chat_room_ai_config.updated_at IS 'AI 설정 또는 요약 정보가 마지막으로 갱신된 시각.';

CREATE TABLE chat_room_member
(
    room_id              bigint      NOT NULL REFERENCES chat_room (id),         -- 참여 중인 채팅방 ID.
    employee_no          varchar(30) NOT NULL REFERENCES employee (employee_no), -- 채팅방 참여 직원 사번.
    joined_at            timestamptz NOT NULL DEFAULT now(),                     -- 채팅방 참여 시각.
    last_read_message_id BIGINT, -- 마지막으로 읽은 메시지 ID. 안 읽은 메시지 수 계산에 사용한다.
    PRIMARY KEY (room_id, employee_no)                                           -- 한 직원은 같은 채팅방에 중복 참여할 수 없다.
);

COMMENT ON TABLE chat_room_member IS '채팅방 참여자 목록.';
COMMENT ON COLUMN chat_room_member.room_id IS '참여 중인 채팅방 ID.';
COMMENT ON COLUMN chat_room_member.employee_no IS '채팅방 참여 직원 사번.';
COMMENT ON COLUMN chat_room_member.joined_at IS '채팅방 참여 시각.';
COMMENT ON COLUMN chat_room_member.last_read_message_id IS '마지막 읽은 ID. 안 읽은 메시지 수 계산에 사용한다.';

CREATE TABLE chat_message
(
    id                bigserial PRIMARY KEY,                                  -- 채팅 메시지 PK. 자동 증가 값이다.
    room_id           bigint      NOT NULL REFERENCES chat_room (id),         -- 메시지가 전송된 채팅방 ID.
    sender_no         varchar(30) NOT NULL REFERENCES employee (employee_no), -- 메시지를 보낸 직원 사번.
    parent_message_id bigint REFERENCES chat_message (id),                    -- 답장 또는 AI가 답변한 원본 메시지
    content           text        NOT NULL,                                   -- 메시지 내용.
    message_type      varchar(20) NOT NULL DEFAULT 'TEXT',                    -- 메시지 유형. TEXT로 사용한다.
    is_ai_called      boolean     NOT NULL DEFAULT false,                     -- @AI 호출 여부.
    is_ai_generated   boolean     NOT NULL DEFAULT false,                     -- AI가 작성한 메시지인지 여부
    ai_metadata       jsonb,                                                  -- AI 모델명, 사용된 소스(RAG), 토큰 등 저장
    sent_at           timestamptz NOT NULL DEFAULT now()                      -- 메시지 전송 시각.
);

COMMENT ON TABLE chat_message IS '채팅 메시지 저장 테이블.';
COMMENT ON COLUMN chat_message.id IS '채팅 메시지 PK. 자동 증가 값이다.';
COMMENT ON COLUMN chat_message.room_id IS '메시지가 전송된 채팅방 ID.';
COMMENT ON COLUMN chat_message.sender_no IS '메시지를 보낸 직원 사번.';
COMMENT ON COLUMN chat_message.parent_message_id IS '답장 또는 AI 답변의 기준이 되는 원본 메시지 ID.';
COMMENT ON COLUMN chat_message.content IS '메시지 내용.';
COMMENT ON COLUMN chat_message.message_type IS '메시지 유형. TEXT로 사용한다.';
COMMENT ON COLUMN chat_message.is_ai_called IS '@AI 호출로 작성된 사용자 질문 메시지 여부.';
COMMENT ON COLUMN chat_message.is_ai_generated IS 'AI가 생성한 답변 또는 요약 메시지 여부.';
COMMENT ON COLUMN chat_message.ai_metadata IS 'AI 메시지 유형, 모델명, 원본 메시지 ID 등 부가 정보를 저장하는 JSON 데이터.';
COMMENT ON COLUMN chat_message.sent_at IS '메시지 전송 시각.';

CREATE INDEX idx_chat_message_room_sent_at ON chat_message (room_id, sent_at DESC);
CREATE INDEX idx_chat_room_member_employee ON chat_room_member (employee_no);

CREATE TABLE project
(
    id               bigserial PRIMARY KEY,                                  -- 프로젝트 PK. 자동 증가 값이다.
    name             varchar(150)    NOT NULL,                               -- 프로젝트명.
    description      text,                                                   -- 프로젝트 설명.
    owner_department department_code NOT NULL REFERENCES departments (code), -- 프로젝트 주관 부서.
    manager_no       varchar(30) REFERENCES employee (employee_no),          -- 프로젝트 담당자 또는 관리자 사번.
    status           project_status  NOT NULL DEFAULT 'READY',               -- 프로젝트 상태. 준비, 진행, 지연, 보류, 완료, 취소를 구분한다.
    progress_rate    smallint        NOT NULL DEFAULT 0,                     -- 프로젝트 진행률. 0부터 100까지의 정수이다.
    starts_on        date,                                                   -- 프로젝트 시작일.
    ends_on          date,                                                   -- 프로젝트 종료 예정일 또는 종료일.
    CHECK (progress_rate BETWEEN 0 AND 100)                                  -- 진행률은 0 이상 100 이하만 허용한다.
);

COMMENT ON TABLE project IS '프로젝트 기본 정보와 대시보드 진척도 상태.';
COMMENT ON COLUMN project.id IS '프로젝트 PK. 자동 증가 값이다.';
COMMENT ON COLUMN project.name IS '프로젝트명.';
COMMENT ON COLUMN project.description IS '프로젝트 설명.';
COMMENT ON COLUMN project.owner_department IS '프로젝트 주관 부서.';
COMMENT ON COLUMN project.manager_no IS '프로젝트 담당자 또는 관리자 사번.';
COMMENT ON COLUMN project.status IS '프로젝트 상태. 준비, 진행, 지연, 보류, 완료, 취소를 구분한다.';
COMMENT ON COLUMN project.progress_rate IS '프로젝트 진행률. 0부터 100까지의 정수이다.';
COMMENT ON COLUMN project.starts_on IS '프로젝트 시작일.';
COMMENT ON COLUMN project.ends_on IS '프로젝트 종료 예정일 또는 종료일.';

CREATE INDEX idx_project_status ON project (status);

ALTER TABLE board
    ADD CONSTRAINT fk_board_project
        FOREIGN KEY (project_id) REFERENCES project (id);

ALTER TABLE calendar_event
    ADD CONSTRAINT fk_calendar_event_project
        FOREIGN KEY (project_id) REFERENCES project (id);

CREATE TABLE project_member
(
    project_id  bigint      NOT NULL REFERENCES project (id),           -- 참여 중인 프로젝트 ID.
    employee_no varchar(30) NOT NULL REFERENCES employee (employee_no), -- 프로젝트 참여 직원 사번.
    joined_at   timestamptz NOT NULL DEFAULT now(),                     -- 프로젝트 참여 시각.
    PRIMARY KEY (project_id, employee_no)                               -- 같은 프로젝트에 같은 직원을 중복 등록하지 않는다.
);

COMMENT ON TABLE project_member IS '프로젝트 참여 멤버 목록.';
COMMENT ON COLUMN project_member.project_id IS '참여 중인 프로젝트 ID.';
COMMENT ON COLUMN project_member.employee_no IS '프로젝트 참여 직원 사번.';
COMMENT ON COLUMN project_member.joined_at IS '프로젝트 참여 시각.';

CREATE TABLE project_task
(
    id            bigserial PRIMARY KEY,                         -- 프로젝트 업무 PK. 자동 증가 값이다.
    project_id    bigint       NOT NULL REFERENCES project (id), -- 업무가 속한 프로젝트 ID.
    assignee_no   varchar(30) REFERENCES employee (employee_no), -- 업무 담당자 사번. 미배정이면 NULL 가능하다.
    title         varchar(200) NOT NULL,                         -- 업무 제목.
    description   text,                                          -- 업무 상세 설명.
    status        task_status  NOT NULL DEFAULT 'TODO',          -- 업무 상태. 할 일, 진행 중, 완료를 구분한다.
    progress_rate smallint     NOT NULL DEFAULT 0,               -- 업무 진행률. 0부터 100까지의 정수이다.
    priority      varchar(20)  NOT NULL DEFAULT 'NORMAL',        -- 업무 우선순위. LOW, NORMAL, HIGH, CRITICAL 등으로 사용한다.
    assigned_by   varchar(30) REFERENCES employee (employee_no), -- 업무를 배정한 직원 사번.
    due_on        date,                                          -- 업무 마감일.
    is_critical   boolean      NOT NULL DEFAULT false,           -- Critical Path 여부. 대시보드 위험 플래그에 사용한다.
    completed_at  timestamptz,                                   -- 업무 완료 시각. 완료 전이면 NULL이다.
    CHECK (progress_rate BETWEEN 0 AND 100)                      -- 업무 진행률은 0 이상 100 이하만 허용한다.
);

COMMENT ON TABLE project_task IS '프로젝트 하위 업무.';
COMMENT ON COLUMN project_task.id IS '프로젝트 업무 PK. 자동 증가 값이다.';
COMMENT ON COLUMN project_task.project_id IS '업무가 속한 프로젝트 ID.';
COMMENT ON COLUMN project_task.assignee_no IS '업무 담당자 사번. 미배정이면 NULL 가능하다.';
COMMENT ON COLUMN project_task.title IS '업무 제목.';
COMMENT ON COLUMN project_task.description IS '업무 상세 설명.';
COMMENT ON COLUMN project_task.status IS '업무 상태. 할 일, 진행 중, 차단, 완료를 구분한다.';
COMMENT ON COLUMN project_task.progress_rate IS '업무 진행률. 0부터 100까지의 정수이다.';
COMMENT ON COLUMN project_task.priority IS '업무 우선순위.';
COMMENT ON COLUMN project_task.assigned_by IS '업무를 배정한 직원 사번.';
COMMENT ON COLUMN project_task.due_on IS '업무 마감일.';
COMMENT ON COLUMN project_task.is_critical IS 'Critical Path 여부.';
COMMENT ON COLUMN project_task.completed_at IS '업무 완료 시각. 완료 전이면 NULL이다.';

CREATE INDEX idx_project_task_project_status ON project_task (project_id, status);
CREATE INDEX idx_project_task_assignee ON project_task (assignee_no);
CREATE INDEX idx_project_task_critical ON project_task (is_critical, due_on) WHERE is_critical = true;

CREATE TABLE notification
(
    id          bigserial PRIMARY KEY,                                   -- 알림 PK. 자동 증가 값이다.
    employee_no varchar(30)  NOT NULL REFERENCES employee (employee_no), -- 알림 수신 직원 사번.
    title       varchar(200) NOT NULL,                                   -- 알림 제목.
    message     text,                                                    -- 알림 상세 메시지.
    ref_type    notification_ref_type,                                   -- 알림이 참조하는 대상 유형.
    ref_id      varchar(80),                                             -- 참조 대상 ID. 테이블별 PK 타입 차이를 고려해 문자열로 저장한다.
    is_read     boolean      NOT NULL DEFAULT false,                     -- 알림 읽음 여부.
    created_at  timestamptz  NOT NULL DEFAULT now()                      -- 알림 생성 시각.
);

COMMENT ON TABLE notification IS '직원별 알림 테이블. 업무 배정, 댓글, 일정, 시스템 알림 등에 사용한다.';
COMMENT ON COLUMN notification.id IS '알림 PK.';
COMMENT ON COLUMN notification.employee_no IS '알림 수신 직원 사번.';
COMMENT ON COLUMN notification.title IS '알림 제목.';
COMMENT ON COLUMN notification.message IS '알림 상세 메시지.';
COMMENT ON COLUMN notification.ref_type IS '알림 참조 대상 유형.';
COMMENT ON COLUMN notification.ref_id IS '참조 대상 ID.';
COMMENT ON COLUMN notification.is_read IS '알림 읽음 여부.';
COMMENT ON COLUMN notification.created_at IS '알림 생성 시각.';

CREATE INDEX idx_notification_unread ON notification (employee_no, is_read) WHERE is_read = false;

CREATE TABLE refresh_token
(
    id          bigserial PRIMARY KEY,                                   -- Refresh token PK. 자동 증가 값이다.
    employee_no varchar(30)  NOT NULL REFERENCES employee (employee_no), -- 토큰 소유자 사번.
    token_hash  varchar(255) NOT NULL UNIQUE,                            -- Refresh token 원문이 아닌 해시값. 탈취 피해를 줄이기 위해 원문 저장을 피한다.
    expires_at  timestamptz  NOT NULL,                                   -- Refresh token 만료 시각.
    revoked_at  timestamptz                                              -- 로그아웃 또는 강제 만료 처리 시각. 유효한 토큰이면 NULL이다.
);

COMMENT ON TABLE refresh_token IS 'JWT refresh token 저장소.';
COMMENT ON COLUMN refresh_token.id IS 'Refresh token PK. 자동 증가 값이다.';
COMMENT ON COLUMN refresh_token.employee_no IS '토큰 소유자 사번.';
COMMENT ON COLUMN refresh_token.token_hash IS 'Refresh token 원문이 아닌 해시값. 토큰 탈취 피해를 줄이기 위해 원문 저장을 피한다.';
COMMENT ON COLUMN refresh_token.expires_at IS 'Refresh token 만료 시각.';
COMMENT ON COLUMN refresh_token.revoked_at IS '로그아웃 또는 강제 만료 처리 시각. 유효한 토큰이면 NULL이다.';

CREATE OR REPLACE FUNCTION prevent_employee_no_update()
    RETURNS trigger AS
$$
BEGIN
    IF OLD.employee_no <> NEW.employee_no THEN
        RAISE EXCEPTION 'employee_no cannot be changed';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_employee_no_update
    BEFORE UPDATE
    ON employee
    FOR EACH ROW
EXECUTE FUNCTION prevent_employee_no_update();
