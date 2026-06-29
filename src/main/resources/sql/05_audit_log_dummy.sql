-- 파일 목적: 감사 로그 화면 확인용 더미 데이터를 생성한다.
-- 실행 대상 DB: office_mate_audit_log
-- 실행 순서: 5
-- 포함 내용: 로그인/계정/직원/권한/CSV 다운로드/fallback 샘플 감사 로그
-- 주의사항: 포트폴리오 시연과 로컬 화면 확인을 위한 더미 감사 로그만 포함한다.

INSERT INTO audit_log (
    trace_id,
    actor_no,
    actor_role,
    action,
    target_type,
    target_id,
    http_method,
    request_uri,
    client_ip,
    user_agent,
    result,
    reason,
    occurred_at
)
VALUES
    (
        'demo-login-super-001',
        'SUPER001',
        'SUPER',
        'LOGIN',
        'AUTH',
        'SUPER001',
        'POST',
        '/api/auth/login',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '2 days'
    ),
    (
        'demo-login-fail-001',
        'ADMIN001',
        NULL,
        'LOGIN',
        'AUTH',
        'ADMIN001',
        'POST',
        '/api/auth/login',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'FAIL',
        'BadCredentialsException: 비밀번호가 일치하지 않습니다.',
        now() - interval '2 days' + interval '10 minutes'
    ),
    (
        'demo-employee-create-001',
        'ADMIN001',
        'ADMIN',
        'CREATE',
        'EMPLOYEE',
        'BE001',
        'POST',
        '/api/management/employees',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '1 day' + interval '1 hour'
    ),
    (
        'demo-employee-update-001',
        'ADMIN001',
        'ADMIN',
        'UPDATE',
        'EMPLOYEE',
        'BE001',
        'PUT',
        '/api/management/employees/BE001',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '1 day' + interval '2 hours'
    ),
    (
        'demo-permission-change-001',
        'SUPER001',
        'SUPER',
        'PERMISSION_CHANGE',
        'EMPLOYEE',
        'ADMIN001',
        'PATCH',
        '/api/management/employees/ADMIN001/management',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '1 day' + interval '3 hours'
    ),
    (
        'demo-account-unlock-001',
        'SUPER001',
        'SUPER',
        'UPDATE',
        'ACCOUNT_SECURITY',
        'ADMIN001',
        'PATCH',
        '/api/management/account-security/ADMIN001/unlock',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '12 hours'
    ),
    (
        'demo-password-reset-request-001',
        NULL,
        NULL,
        'UPDATE',
        'AUTH',
        NULL,
        'POST',
        '/api/auth/password-reset/request',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '11 hours'
    ),
    (
        'demo-password-reset-confirm-001',
        NULL,
        NULL,
        'UPDATE',
        'AUTH',
        NULL,
        'POST',
        '/api/auth/password-reset/confirm',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '10 hours'
    ),
    (
        'demo-employee-resign-001',
        'ADMIN001',
        'ADMIN',
        'STATUS_CHANGE',
        'EMPLOYEE',
        'BE001',
        'PATCH',
        '/api/management/employees/BE001/resign',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '9 hours'
    ),
    (
        'demo-return-from-leave-request-001',
        NULL,
        NULL,
        'STATUS_CHANGE',
        'EMPLOYEE_STATUS_REQUEST',
        NULL,
        'POST',
        '/api/auth/return-from-leave/request',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '8 hours'
    ),
    (
        'demo-my-status-request-001',
        'BE001',
        'USER',
        'STATUS_CHANGE',
        'EMPLOYEE_STATUS_REQUEST',
        NULL,
        'POST',
        '/api/my/status-requests',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '7 hours'
    ),
    (
        'demo-status-request-approve-001',
        'SUPER001',
        'SUPER',
        'STATUS_CHANGE',
        'EMPLOYEE_STATUS_REQUEST',
        '1001',
        'PATCH',
        '/api/management/status-requests/1001/approve',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '6 hours' + interval '20 minutes'
    ),
    (
        'demo-status-request-reject-001',
        'SUPER001',
        'SUPER',
        'STATUS_CHANGE',
        'EMPLOYEE_STATUS_REQUEST',
        '1002',
        'PATCH',
        '/api/management/status-requests/1002/reject',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'FAIL',
        'IllegalStateException: 이미 처리된 신청입니다.',
        now() - interval '6 hours' + interval '40 minutes'
    ),
    (
        'demo-export-001',
        'SUPER001',
        'SUPER',
        'EXPORT',
        'AUDIT_LOG',
        NULL,
        'GET',
        '/api/management/audit-logs/export.csv',
        '127.0.0.1',
        'OfficeMate-Dummy-Agent',
        'SUCCESS',
        NULL,
        now() - interval '6 hours'
    )
ON CONFLICT DO NOTHING;

-- Redis 저장 실패/fallback 재이관 화면 확인용 샘플.
-- processed_at이 NULL이면 아직 audit_log로 이관되지 않은 fallback 로그로 간주한다.
INSERT INTO audit_fallback_log (
    payload,
    reason,
    occurred_at,
    processed_at
)
VALUES
    (
        '{
          "traceId": "demo-fallback-001",
          "actorNo": "SUPER001",
          "actorRole": "SUPER",
          "action": "EXPORT",
          "targetType": "AUDIT_LOG",
          "targetId": null,
          "httpMethod": "GET",
          "requestUri": "/api/management/audit-logs/export.csv",
          "clientIp": "127.0.0.1",
          "userAgent": "OfficeMate-Dummy-Agent",
          "result": "SUCCESS",
          "reason": null,
          "occurredAtEpochMillis": 1760000000000
        }'::jsonb,
        'Redis 저장 실패 샘플',
        now() - interval '1 hour',
        NULL
    );
