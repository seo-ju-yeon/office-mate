-- main dummy data
-- 실행 대상 DB: office_mate_2605
-- 실행 순서: 01_init.sql -> 02_main_db.sql -> 04_main_dummy.sql
--
-- 목적:
-- Spring Boot 실행 직후 로그인, 권한, 게시판, 일정, 채팅, 프로젝트 화면을
-- 바로 테스트할 수 있도록 main DB에 최소 샘플 데이터를 넣는다.
--
-- 비밀번호:
-- 아래 employee.password 값은 임시 비밀번호 1111을 BCrypt로 암호화한 값이다.
-- 로그인 테스트 계정: SUPER001 / ADMIN001 / BE001 / FE001 / MS001
-- 공통 임시 비밀번호: 1111

INSERT INTO employee (employee_no,
                      password,
                      temp_password_required,
                      name,
                      email,
                      department,
                      position,
                      role,
                      status,
                      joined_on)
VALUES ('SUPER001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '슈퍼관리자',
        'super@example.com',
        'BACKEND',
        'CEO',
        'SUPER',
        'ACTIVE',
        CURRENT_DATE),
       ('ADMIN001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '관리자',
        'admin@example.com',
        'MANAGEMENT_SUPPORT',
        'GENERAL_MANAGER',
        'ADMIN',
        'ACTIVE',
        CURRENT_DATE),
       ('BE001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '백엔드사원',
        'backend.staff@example.com',
        'BACKEND',
        'STAFF',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('FE001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '프론트엔드사원',
        'frontend.staff@example.com',
        'FRONTEND',
        'STAFF',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('MS001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '경영지원대리',
        'management.assistant@example.com',
        'MANAGEMENT_SUPPORT',
        'ASSISTANT_MANAGER',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('FE002',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '프론트엔드대리',
        'frontend.assistant@example.com',
        'FRONTEND',
        'ASSISTANT_MANAGER',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('FE003',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '프론트엔드과장',
        'frontend.manager@example.com',
        'FRONTEND',
        'MANAGER',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('FE004',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '프론트엔드휴직자',
        'frontend.leave@example.com',
        'FRONTEND',
        'STAFF',
        'USER',
        'ON_LEAVE',
        CURRENT_DATE),
       ('BE002',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '백엔드대리',
        'backend.assistant@example.com',
        'BACKEND',
        'ASSISTANT_MANAGER',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('BE003',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '백엔드과장',
        'backend.manager@example.com',
        'BACKEND',
        'MANAGER',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('BE004',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '백엔드차장',
        'backend.deputy@example.com',
        'BACKEND',
        'DEPUTY_GENERAL_MANAGER',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('MS002',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '경영지원사원',
        'management.staff@example.com',
        'MANAGEMENT_SUPPORT',
        'STAFF',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('MS003',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '경영지원과장',
        'management.manager@example.com',
        'MANAGEMENT_SUPPORT',
        'MANAGER',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('MK001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '마케팅사원',
        'marketing.staff@example.com',
        'MARKETING',
        'STAFF',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('MK002',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '마케팅대리',
        'marketing.assistant@example.com',
        'MARKETING',
        'ASSISTANT_MANAGER',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('DB001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        'DB관리사원',
        'db.staff@example.com',
        'DB_MANAGEMENT',
        'STAFF',
        'USER',
        'ACTIVE',
        CURRENT_DATE),
       ('DB002',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        'DB관리휴직자',
        'db.leave@example.com',
        'DB_MANAGEMENT',
        'ASSISTANT_MANAGER',
        'USER',
        'ON_LEAVE',
        CURRENT_DATE)
ON CONFLICT (employee_no) DO NOTHING;

-- 계정 상태 변경 테스트용 직원.
-- LEAVE001은 휴직 계정, RESIGN001은 퇴직 계정으로 로그인/토큰 차단 흐름 검증에 사용한다.
INSERT INTO employee (employee_no,
                      password,
                      temp_password_required,
                      name,
                      email,
                      department,
                      position,
                      role,
                      status,
                      joined_on,
                      resigned_on)
VALUES ('LEAVE001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '휴직처리직원',
        'leave.test@example.com',
        'MANAGEMENT_SUPPORT',
        'STAFF',
        'USER',
        'ON_LEAVE',
        CURRENT_DATE - 365,
        NULL),
       ('RESIGN001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '퇴직처리직원',
        'resign.test@example.com',
        'BACKEND',
        'STAFF',
        'USER',
        'RESIGNED',
        CURRENT_DATE - 730,
        CURRENT_DATE - 30)
ON CONFLICT (employee_no) DO NOTHING;

-- 계정 잠금 테스트용 직원.
-- LOCK001은 로그인 실패 횟수 초과로 이미 잠긴 계정 상태를 검증하는 데 사용한다.
INSERT INTO employee (employee_no,
                      password,
                      temp_password_required,
                      name,
                      email,
                      department,
                      position,
                      role,
                      status,
                      joined_on)
VALUES ('LOCK001',
        '$2y$10$TANo4Rgxh0Itd7hB1ZraAuxuE0QTfo6DCkQXQLKJFCTk5c.tgfA6G',
        true,
        '계정잠금직원',
        'lock.test@example.com',
        'DB_MANAGEMENT',
        'STAFF',
        'USER',
        'ACTIVE',
        CURRENT_DATE - 180)
ON CONFLICT (employee_no) DO NOTHING;

INSERT INTO account_security_status (employee_no,
                                     login_fail_count,
                                     account_locked,
                                     last_failed_at,
                                     locked_at,
                                     unlocked_at,
                                     unlocked_by,
                                     created_at,
                                     updated_at)
VALUES ('LOCK001',
        5,
        true,
        now() - interval '10 minutes',
        now() - interval '10 minutes',
        NULL,
        NULL,
        now() - interval '30 minutes',
        now() - interval '10 minutes')
ON CONFLICT (employee_no) DO UPDATE
    SET login_fail_count = EXCLUDED.login_fail_count,
        account_locked   = EXCLUDED.account_locked,
        last_failed_at   = EXCLUDED.last_failed_at,
        locked_at        = EXCLUDED.locked_at,
        unlocked_at      = EXCLUDED.unlocked_at,
        unlocked_by      = EXCLUDED.unlocked_by,
        updated_at       = EXCLUDED.updated_at;

-- 상태 신청 관리 테스트 데이터.
-- PENDING, APPROVED, REJECTED 상태를 함께 넣어 승인/반려 화면과 상태 반영 이력을 검증한다.
INSERT INTO employee_status_request (employee_no,
                                     request_type,
                                     reason,
                                     requested_at,
                                     status,
                                     decided_by,
                                     decided_at,
                                     decision_comment)
SELECT v.employee_no,
       v.request_type::employment_request_type,
       v.reason,
       v.requested_at,
       v.status::request_status,
       v.decided_by,
       v.decided_at,
       v.decision_comment
FROM (VALUES ('MS002',
              'LEAVE',
              '가족 돌봄을 위한 단기 휴직 신청입니다.',
              now() - interval '1 day',
              'PENDING',
              NULL::varchar,
              NULL::timestamptz,
              NULL::text),
             ('FE004',
              'RETURN_FROM_LEAVE',
              '휴직 기간 종료로 복직을 신청합니다.',
              now() - interval '2 days',
              'PENDING',
              NULL::varchar,
              NULL::timestamptz,
              NULL::text),
             ('LEAVE001',
              'LEAVE',
              '건강상 사유로 휴직을 신청합니다.',
              now() - interval '14 days',
              'APPROVED',
              'SUPER001',
              now() - interval '13 days',
              '휴직 신청을 승인합니다.'),
             ('RESIGN001',
              'RESIGN',
              '개인 사유로 퇴사를 신청합니다.',
              now() - interval '45 days',
              'APPROVED',
              'SUPER001',
              now() - interval '30 days',
              '퇴사 신청을 승인합니다.'),
             ('MK001',
              'LEAVE',
              '프로젝트 일정으로 인한 휴직 신청입니다.',
              now() - interval '7 days',
              'REJECTED',
              'SUPER001',
              now() - interval '6 days',
              '진행 중인 업무 인수인계 계획이 필요하여 반려합니다.'))
         AS v(employee_no, request_type, reason, requested_at, status, decided_by, decided_at, decision_comment)
WHERE EXISTS (SELECT 1
              FROM employee e
              WHERE e.employee_no = v.employee_no)
  AND NOT EXISTS (SELECT 1
                  FROM employee_status_request r
                  WHERE r.employee_no = v.employee_no
                    AND r.request_type = v.request_type::employment_request_type
                    AND r.reason = v.reason);

-- 직원별 시스템 역할 매핑. employee.role과 employee_roles를 같은 역할 상태로 맞춘다.
INSERT INTO employee_roles (employee_no, role_code, granted_by)
VALUES ('SUPER001', 'SUPER', 'SUPER001'),
       ('ADMIN001', 'ADMIN', 'SUPER001'),
       ('BE001', 'USER', 'ADMIN001'),
       ('FE001', 'USER', 'ADMIN001'),
       ('MS001', 'USER', 'ADMIN001'),
       ('FE002', 'USER', 'ADMIN001'),
       ('FE003', 'USER', 'ADMIN001'),
       ('FE004', 'USER', 'ADMIN001'),
       ('BE002', 'USER', 'ADMIN001'),
       ('BE003', 'USER', 'ADMIN001'),
       ('BE004', 'USER', 'ADMIN001'),
       ('MS002', 'USER', 'ADMIN001'),
       ('MS003', 'USER', 'ADMIN001'),
       ('MK001', 'USER', 'ADMIN001'),
       ('MK002', 'USER', 'ADMIN001'),
       ('DB001', 'USER', 'ADMIN001'),
       ('DB002', 'USER', 'ADMIN001'),
       ('LEAVE001', 'USER', 'ADMIN001'),
       ('RESIGN001', 'USER', 'ADMIN001'),
       ('LOCK001', 'USER', 'ADMIN001')
ON CONFLICT (employee_no, role_code) DO NOTHING;

INSERT INTO post (board_id, author_no, title, content, is_pinned)
SELECT b.id,
       'SUPER001',
       '시스템 오픈 안내',
       '사내 사무 보조 및 협업 플랫폼 포트폴리오 프로젝트입니다.',
       true
FROM board b
WHERE b.type = 'NOTICE'
  AND b.name = '공지사항'
  AND NOT EXISTS (SELECT 1
                  FROM post p
                  WHERE p.author_no = 'SUPER001'
                    AND p.title = '시스템 오픈 안내');

INSERT INTO post (board_id, author_no, title, content)
SELECT b.id,
       'BE001',
       '백엔드 API 작업 공유',
       'JWT, Spring Security, PostgreSQL 기반으로 인증 작업을 진행 중입니다.'
FROM board b
WHERE b.type = 'GENERAL'
  AND b.name = '자유게시판'
  AND NOT EXISTS (SELECT 1
                  FROM post p
                  WHERE p.author_no = 'BE001'
                    AND p.title = '백엔드 API 작업 공유');

INSERT INTO post_comment (post_id, author_no, content)
SELECT p.id,
       'FE001',
       '프론트 화면 연동 시 accessToken 전달 방식도 맞춰보겠습니다.'
FROM post p
WHERE p.title = '백엔드 API 작업 공유'
  AND NOT EXISTS (SELECT 1
                  FROM post_comment c
                  WHERE c.post_id = p.id
                    AND c.author_no = 'FE001'
                    AND c.content = '프론트 화면 연동 시 accessToken 전달 방식도 맞춰보겠습니다.');

INSERT INTO project (name, description, owner_department, manager_no, status, progress_rate, starts_on, ends_on)
SELECT '사내 사무 보조 및 협업 플랫폼',
       '포트폴리오용 팀 협업 플랫폼 구축 프로젝트',
       'BACKEND',
       'ADMIN001',
       'IN_PROGRESS',
       15,
       CURRENT_DATE,
       CURRENT_DATE + 60
WHERE NOT EXISTS (SELECT 1
                  FROM project
                  WHERE name = '사내 사무 보조 및 협업 플랫폼');

INSERT INTO project_member (project_id, employee_no)
SELECT p.id, e.employee_no
FROM project p
         JOIN employee e ON e.employee_no IN ('ADMIN001', 'BE001', 'FE001', 'MS001')
WHERE p.name = '사내 사무 보조 및 협업 플랫폼'
ON CONFLICT (project_id, employee_no) DO NOTHING;

INSERT INTO project_task (project_id, assignee_no, title, description, status, progress_rate, priority, assigned_by,
                          due_on, is_critical)
SELECT p.id,
       'BE001',
       'JWT 로그인 및 토큰 재발급 구현',
       'Spring Security Filter 기반 로그인, access token, refresh token 흐름을 구현한다.',
       'IN_PROGRESS',
       40,
       'HIGH',
       'ADMIN001',
       CURRENT_DATE + 7,
       true
FROM project p
WHERE p.name = '사내 사무 보조 및 협업 플랫폼'
  AND NOT EXISTS (SELECT 1
                  FROM project_task t
                  WHERE t.project_id = p.id
                    AND t.title = 'JWT 로그인 및 토큰 재발급 구현');

INSERT INTO project_task (project_id, assignee_no, title, description, status, progress_rate, priority, assigned_by,
                          due_on, is_critical)
SELECT p.id,
       'FE001',
       '대시보드 기본 레이아웃 구현',
       'Thymeleaf 레이아웃과 대시보드 진입 화면을 정리한다.',
       'TODO',
       0,
       'NORMAL',
       'ADMIN001',
       CURRENT_DATE + 10,
       false
FROM project p
WHERE p.name = '사내 사무 보조 및 협업 플랫폼'
  AND NOT EXISTS (SELECT 1
                  FROM project_task t
                  WHERE t.project_id = p.id
                    AND t.title = '대시보드 기본 레이아웃 구현');

INSERT INTO calendar_event (owner_no, created_by, title, description, starts_at, ends_at, scope)
SELECT 'BE001',
       'BE001',
       'JWT 인증 로직 점검',
       '로그인, refresh, 비밀번호 변경 API 테스트',
       now() + interval '1 day',
       now() + interval '1 day 1 hour',
       'PERSONAL'
WHERE NOT EXISTS (SELECT 1
                  FROM calendar_event
                  WHERE owner_no = 'BE001'
                    AND title = 'JWT 인증 로직 점검');

INSERT INTO calendar_event (project_id, created_by, title, description, starts_at, ends_at, scope)
SELECT p.id,
       'ADMIN001',
       '프로젝트 중간 점검',
       '대시보드 진척도와 JWT 인증 흐름 점검',
       now() + interval '5 days',
       now() + interval '5 days 2 hours',
       'PROJECT'
FROM project p
WHERE p.name = '사내 사무 보조 및 협업 플랫폼'
  AND NOT EXISTS (SELECT 1
                  FROM calendar_event ce
                  WHERE ce.project_id = p.id
                    AND ce.title = '프로젝트 중간 점검');

INSERT INTO chat_room (name, is_group)
SELECT 'OfficeMate 개발방', true
WHERE NOT EXISTS (SELECT 1
                  FROM chat_room
                  WHERE name = 'OfficeMate 개발방');

INSERT INTO chat_room_member (room_id, employee_no)
SELECT r.id, e.employee_no
FROM chat_room r
         JOIN employee e ON e.employee_no IN ('ADMIN001', 'BE001', 'FE001', 'MS001')
WHERE r.name = 'OfficeMate 개발방'
ON CONFLICT (room_id, employee_no) DO NOTHING;

INSERT INTO chat_message (room_id, sender_no, content)
SELECT r.id,
       'ADMIN001',
       '오늘은 JWT 로그인 흐름과 DB 더미 데이터를 맞춰보겠습니다.'
FROM chat_room r
WHERE r.name = 'OfficeMate 개발방'
  AND NOT EXISTS (SELECT 1
                  FROM chat_message m
                  WHERE m.room_id = r.id
                    AND m.sender_no = 'ADMIN001'
                    AND m.content = '오늘은 JWT 로그인 흐름과 DB 더미 데이터를 맞춰보겠습니다.');

INSERT INTO chat_message (room_id, sender_no, content, sent_at)
SELECT r.id,
       v.sender_no,
       v.content,
       now() - v.sent_before
FROM chat_room r
         JOIN (VALUES ('BE001', 'TokenCheckFilter에서 계정 상태를 다시 확인하는 흐름까지 같이 보겠습니다.', interval '2 hours'),
                      ('FE001', 'layout 공통 fetch handler에서 ACCOUNT_INACTIVE 응답 처리도 연결해두겠습니다.', interval '90 minutes'),
                      ('MS001', '시연 시나리오에는 휴직/퇴직 계정으로 접근 차단되는 장면을 넣으면 좋겠습니다.', interval '45 minutes'))
              AS v(sender_no, content, sent_before) ON true
WHERE r.name = 'OfficeMate 개발방'
  AND NOT EXISTS (SELECT 1
                  FROM chat_message m
                  WHERE m.room_id = r.id
                    AND m.sender_no = v.sender_no
                    AND m.content = v.content);

INSERT INTO chat_room (name, is_group)
SELECT 'OfficeMate 보안검토방', true
WHERE NOT EXISTS (SELECT 1
                  FROM chat_room
                  WHERE name = 'OfficeMate 보안검토방');

INSERT INTO chat_room_member (room_id, employee_no)
SELECT r.id, e.employee_no
FROM chat_room r
         JOIN employee e ON e.employee_no IN ('ADMIN001', 'BE001', 'DB001')
WHERE r.name = 'OfficeMate 보안검토방'
ON CONFLICT (room_id, employee_no) DO NOTHING;

INSERT INTO chat_message (room_id, sender_no, content, sent_at)
SELECT r.id,
       v.sender_no,
       v.content,
       now() - v.sent_before
FROM chat_room r
         JOIN (VALUES ('ADMIN001', 'Refresh Token은 HttpOnly Cookie로 분리했고 DB에는 해시값만 저장됩니다.', interval '3 hours'),
                      ('BE001', '로그아웃, 비밀번호 변경, 휴직/퇴직 처리 시 Refresh Token revoke도 같이 확인했습니다.', interval '2 hours 40 minutes'),
                      ('DB001', '감사 로그 쪽은 Redis 실패 시 fallback 테이블로 저장되는 케이스까지 더미로 설명하면 좋겠습니다.', interval '2 hours 10 minutes'))
              AS v(sender_no, content, sent_before) ON true
WHERE r.name = 'OfficeMate 보안검토방'
  AND NOT EXISTS (SELECT 1
                  FROM chat_message m
                  WHERE m.room_id = r.id
                    AND m.sender_no = v.sender_no
                    AND m.content = v.content);

INSERT INTO chat_room (name, is_group)
SELECT 'BE001-FE001 1:1', false
WHERE NOT EXISTS (SELECT 1
                  FROM chat_room
                  WHERE name = 'BE001-FE001 1:1');

INSERT INTO chat_room_member (room_id, employee_no)
SELECT r.id, e.employee_no
FROM chat_room r
         JOIN employee e ON e.employee_no IN ('BE001', 'FE001')
WHERE r.name = 'BE001-FE001 1:1'
ON CONFLICT (room_id, employee_no) DO NOTHING;

INSERT INTO chat_message (room_id, sender_no, content, sent_at)
SELECT r.id,
       v.sender_no,
       v.content,
       now() - v.sent_before
FROM chat_room r
         JOIN (VALUES ('BE001', '게시판 API 응답 형식 맞춰서 프론트 연결 부탁드립니다.', interval '75 minutes'),
                      ('FE001', '확인했습니다. 목록 페이징과 상세 조회 먼저 연결해볼게요.', interval '70 minutes'),
                      ('BE001', '첨부파일 다운로드 쪽은 인증 헤더가 필요한지도 같이 체크해주세요.', interval '65 minutes'))
              AS v(sender_no, content, sent_before) ON true
WHERE r.name = 'BE001-FE001 1:1'
  AND NOT EXISTS (SELECT 1
                  FROM chat_message m
                  WHERE m.room_id = r.id
                    AND m.sender_no = v.sender_no
                    AND m.content = v.content);

INSERT INTO chat_room_ai_config (room_id, is_ai_active, system_prompt, summary_enabled, last_summary, last_summarized_id)
SELECT r.id,
       true,
       '업무 협업 내용을 간결하게 정리하고 일정, 담당자, 결정사항을 중심으로 답변한다.',
       true,
       'JWT 인증, 계정 상태 차단, 화면 권한 제어, 감사 로그 흐름을 중심으로 논의가 진행되었다.',
       (SELECT max(m.id) FROM chat_message m WHERE m.room_id = r.id)
FROM chat_room r
WHERE r.name IN ('OfficeMate 개발방', 'OfficeMate 보안검토방')
ON CONFLICT (room_id) DO UPDATE
    SET is_ai_active       = EXCLUDED.is_ai_active,
        system_prompt      = EXCLUDED.system_prompt,
        summary_enabled    = EXCLUDED.summary_enabled,
        last_summary       = EXCLUDED.last_summary,
        last_summarized_id = EXCLUDED.last_summarized_id,
        updated_at         = now();

UPDATE chat_room_member crm
SET last_read_message_id = latest.latest_message_id
FROM (SELECT room_id, max(id) AS latest_message_id
      FROM chat_message
      GROUP BY room_id) latest
WHERE crm.room_id = latest.room_id
  AND crm.employee_no IN ('ADMIN001', 'BE001');

UPDATE chat_room_member crm
SET last_read_message_id = latest.latest_message_id - 1
FROM (SELECT room_id, max(id) AS latest_message_id
      FROM chat_message
      GROUP BY room_id) latest
WHERE crm.room_id = latest.room_id
  AND crm.employee_no IN ('FE001', 'MS001', 'DB001')
  AND latest.latest_message_id > 1;

INSERT INTO notification (employee_no, title, message, ref_type, ref_id)
SELECT 'BE001',
       '새 업무가 배정되었습니다',
       'JWT 로그인 및 토큰 재발급 구현 업무를 확인해주세요.',
       'PROJECT_TASK',
       t.id::varchar
FROM project_task t
WHERE t.title = 'JWT 로그인 및 토큰 재발급 구현'
  AND NOT EXISTS (SELECT 1
                  FROM notification n
                  WHERE n.employee_no = 'BE001'
                    AND n.title = '새 업무가 배정되었습니다'
                    AND n.ref_type = 'PROJECT_TASK'
                    AND n.ref_id = t.id::varchar);

-- 프로젝트 전용 공지 게시판 생성
INSERT INTO board (type, name, project_id)
SELECT 'NOTICE',
       p.name || ' 공지',
       p.id
FROM project p
WHERE p.name = '사내 사무 보조 및 협업 플랫폼'
  AND NOT EXISTS (SELECT 1
                  FROM board b
                  WHERE b.project_id = p.id
                    AND b.type = 'NOTICE');

-- 프로젝트 공지 게시글 더미
-- ADMIN001: 프로젝트 총괄 공지
INSERT INTO post (board_id, author_no, title, content)
SELECT b.id,
       'ADMIN001',
       '[필독] 프로젝트 진행 규칙 안내',
       '브랜치 전략, 코드 리뷰 절차, 일정 관리 방식을 공유합니다.'
FROM board b
         JOIN project p ON p.id = b.project_id
WHERE p.name = '사내 사무 보조 및 협업 플랫폼'
  AND b.type = 'NOTICE'
  AND b.project_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM post ps
                  WHERE ps.board_id = b.id
                    AND ps.title = '[필독] 프로젝트 진행 규칙 안내');

-- BE001: 백엔드 작업 공유 (기존 task와 맥락 일치)
INSERT INTO post (board_id, author_no, title, content)
SELECT b.id,
       'ADMIN001',
       'API 명세서 공유',
       '게시판 / 댓글 / 첨부파일 API 명세를 Notion에 정리했습니다. 링크 확인 부탁드립니다.'
FROM board b
         JOIN project p ON p.id = b.project_id
WHERE p.name = '사내 사무 보조 및 협업 플랫폼'
  AND b.type = 'NOTICE'
  AND b.project_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM post ps
                  WHERE ps.board_id = b.id
                    AND ps.title = 'API 명세서 공유');

-- FE001: 프론트엔드 작업 공유 (기존 task와 맥락 일치)
INSERT INTO post (board_id, author_no, title, content)
SELECT b.id,
       'ADMIN001',
       '대시보드 레이아웃 진행 현황',
       'Thymeleaf 레이아웃 기본 구조 잡았습니다. 공통 헤더/사이드바 연동 확인 부탁드립니다.'
FROM board b
         JOIN project p ON p.id = b.project_id
WHERE p.name = '사내 사무 보조 및 협업 플랫폼'
  AND b.type = 'NOTICE'
  AND b.project_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM post ps
                  WHERE ps.board_id = b.id
                    AND ps.title = '대시보드 레이아웃 진행 현황');
