-- 파일 목적: Office Mate에서 사용할 PostgreSQL 데이터베이스 2개를 생성한다.
-- 실행 대상 DB: postgres
-- 실행 순서: 1
-- 포함 내용: 메인 업무 DB(office_mate_2605), 감사 로그 DB(office_mate_audit_log) 생성
-- 주의사항: 이미 같은 이름의 DB가 있으면 CREATE DATABASE 실행이 실패할 수 있다.

CREATE DATABASE office_mate_2605;
CREATE DATABASE office_mate_audit_log;
