-- audit log schema
-- 실행 대상 DB: office_mate_audit_log
-- 목적: AOP/Redis/Scheduler 기반 감사 로그를 메인 업무 DB와 분리된 감사 로그 전용 DB에 보관한다.
-- IntelliJ Ultimate에서는 office_mate_audit_log 데이터소스를 선택한 뒤 이 파일 전체를 실행한다.

CREATE TYPE audit_action AS ENUM (
    'CREATE', -- 데이터 생성
    'READ', -- 민감 데이터 조회
    'UPDATE', -- 데이터 수정
    'DELETE', -- 데이터 삭제 또는 논리 삭제
    'LOGIN', -- 로그인 성공
    'LOGIN_FAIL', -- 로그인 실패
    'LOGOUT', -- 로그아웃
    'PERMISSION_CHANGE', -- 사용자 권한 또는 관리 권한 변경
    'STATUS_CHANGE', -- 휴직/복직/퇴사 등 계정 또는 재직 상태 변경
    'EXPORT' -- CSV 다운로드 등 데이터 반출
    );

CREATE TYPE audit_result AS ENUM (
    'SUCCESS', -- 요청 성공
    'FAIL' -- 요청 실패
    );

CREATE TABLE audit_log
(
    id          bigserial PRIMARY KEY,              -- 감사 로그 PK. 자동 증가 값이다.
    trace_id    varchar(80)  NOT NULL,              -- API 요청 단위 추적 ID.
    actor_no    varchar(30),                        -- 행위를 수행한 직원 사번. 비로그인 요청 또는 로그인 실패는 NULL 가능하다.
    actor_role  varchar(20),                        -- 요청 당시 사용자 역할. USER, ADMIN, SUPER 등을 문자열로 저장한다.
    action      audit_action NOT NULL,              -- 감사 대상 행위. CREATE, UPDATE, DELETE, LOGIN, PERMISSION_CHANGE, STATUS_CHANGE, EXPORT 등을 구분한다.
    target_type varchar(60)  NOT NULL,              -- 행위 대상 유형. 예: AUTH, EMPLOYEE, ACCOUNT_SECURITY, AUDIT_LOG.
    target_id   varchar(80),                        -- 행위 대상 데이터 ID. 대상이 특정되지 않으면 NULL 가능하다.
    http_method varchar(10),                        -- HTTP 메서드. GET, POST, PUT, PATCH, DELETE 등.
    request_uri varchar(500),                       -- 요청 URI. 개인정보가 포함된 query string은 저장하지 않는 것을 권장한다.
    client_ip   inet,                               -- 요청 클라이언트 IP. PostgreSQL inet 타입을 사용한다.
    user_agent  varchar(500),                       -- 요청 브라우저 또는 클라이언트 User-Agent.
    result      audit_result NOT NULL,              -- 요청 처리 결과. SUCCESS 또는 FAIL.
    reason      varchar(500),                       -- 실패 사유, 차단 사유, 관리자 수정 사유 등 감사에 필요한 부가 설명.
    occurred_at timestamptz  NOT NULL,              -- 실제 API 요청이 발생한 시각.
    flushed_at  timestamptz  NOT NULL DEFAULT now() -- Redis 큐 또는 fallback 로그에서 audit_log 테이블로 이관된 시각.
);

COMMENT ON TABLE audit_log IS '감사 로그 영구 보관 테이블. Redis 큐와 fallback 테이블의 로그를 스케줄러 기반 이관 작업이 적재한다.';
COMMENT ON COLUMN audit_log.id IS '감사 로그 PK. 자동 증가 값이다.';
COMMENT ON COLUMN audit_log.trace_id IS 'API 요청 단위 추적 ID.';
COMMENT ON COLUMN audit_log.actor_no IS '행위를 수행한 직원 사번. 비로그인 요청 또는 로그인 실패는 NULL 가능하다.';
COMMENT ON COLUMN audit_log.actor_role IS '요청 당시 사용자 역할. USER, ADMIN, SUPER 등을 문자열로 저장한다.';
COMMENT ON COLUMN audit_log.action IS '감사 대상 행위. CREATE, READ, UPDATE, DELETE, LOGIN, LOGIN_FAIL, LOGOUT, PERMISSION_CHANGE, STATUS_CHANGE, EXPORT 등을 구분한다.';
COMMENT ON COLUMN audit_log.target_type IS '행위 대상 유형. 예: AUTH, EMPLOYEE, ACCOUNT_SECURITY, AUDIT_LOG.';
COMMENT ON COLUMN audit_log.target_id IS '행위 대상 데이터 ID. 대상이 특정되지 않으면 NULL 가능하다.';
COMMENT ON COLUMN audit_log.http_method IS 'HTTP 메서드. GET, POST, PUT, PATCH, DELETE 등.';
COMMENT ON COLUMN audit_log.request_uri IS '요청 URI. 개인정보가 포함된 query string은 저장하지 않는 것을 권장한다.';
COMMENT ON COLUMN audit_log.client_ip IS '요청 클라이언트 IP. PostgreSQL inet 타입을 사용한다.';
COMMENT ON COLUMN audit_log.user_agent IS '요청 브라우저 또는 클라이언트 User-Agent.';
COMMENT ON COLUMN audit_log.result IS '요청 처리 결과. SUCCESS 또는 FAIL.';
COMMENT ON COLUMN audit_log.reason IS '실패 사유, 차단 사유, 관리자 수정 사유 등 감사에 필요한 부가 설명.';
COMMENT ON COLUMN audit_log.occurred_at IS '실제 API 요청이 발생한 시각.';
COMMENT ON COLUMN audit_log.flushed_at IS 'Redis 큐 또는 fallback 로그에서 audit_log 테이블로 이관된 시각.';

-- 행위자별 감사 로그를 최신순으로 조회하기 위한 인덱스
CREATE INDEX idx_audit_log_actor_time ON audit_log (actor_no, occurred_at DESC);
-- 특정 대상 데이터의 변경/접근 이력을 최신순으로 조회하기 위한 인덱스
CREATE INDEX idx_audit_log_target_time ON audit_log (target_type, target_id, occurred_at DESC);
-- 단일 요청 흐름(trace_id)을 빠르게 추적하기 위한 인덱스
CREATE INDEX idx_audit_log_trace_id ON audit_log (trace_id);

CREATE TABLE audit_fallback_log
(
    id           bigserial PRIMARY KEY,               -- fallback 로그 PK. 자동 증가 값이다.
    payload      jsonb        NOT NULL,               -- Redis에 저장하지 못한 감사 로그 원본 JSON.
    reason       varchar(300) NOT NULL,               -- fallback 저장 사유. 예: Redis connection failure.
    occurred_at  timestamptz  NOT NULL DEFAULT now(), -- fallback 로그가 저장된 시각.
    processed_at timestamptz                          -- 스케줄러 이관 작업이 audit_log로 이관 완료한 시각. 미처리 상태면 NULL이다.
);

COMMENT ON TABLE audit_fallback_log IS 'Redis 저장 실패 시 감사 로그 원본을 임시 저장하는 fallback 테이블.';
COMMENT ON COLUMN audit_fallback_log.id IS 'fallback 로그 PK. 자동 증가 값이다.';
COMMENT ON COLUMN audit_fallback_log.payload IS 'Redis에 저장하지 못한 감사 로그 원본 JSON.';
COMMENT ON COLUMN audit_fallback_log.reason IS 'fallback 저장 사유. 예: Redis connection failure.';
COMMENT ON COLUMN audit_fallback_log.occurred_at IS 'fallback 로그가 저장된 시각.';
COMMENT ON COLUMN audit_fallback_log.processed_at IS '스케줄러 이관 작업이 audit_log로 이관 완료한 시각. 미처리 상태면 NULL이다.';

-- 아직 audit_log로 이관되지 않은 fallback 로그를 빠르게 조회하하기 위한 부분 인덱스
CREATE INDEX idx_audit_fallback_unprocessed
    ON audit_fallback_log (id)
    WHERE processed_at IS NULL;
