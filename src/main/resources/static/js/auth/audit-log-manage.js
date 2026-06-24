// 현재 조회 중인 페이지 번호
let auditCurrentPage = 0;

// 서버가 내려준 전체 페이지 수
let auditTotalPages = 0;

// 현재 화면에 렌더링된 감사 로그 목록
let auditCurrentLogs = [];

/* 감사 로그 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', function () {
    loadAuditLogs(0);
});

/* 감사 로그 목록 조회 처리 */
async function loadAuditLogs(page) {
    // 조회 조건과 인증 토큰 준비
    const token = localStorage.getItem('accessToken');
    const params = new URLSearchParams();

    // 페이지와 필터 조건 생성
    auditCurrentPage = Math.max(page, 0);
    params.set('page', auditCurrentPage);
    params.set('size', '20');
    appendParam(params, 'action', document.getElementById('auditAction').value);
    appendParam(params, 'result', document.getElementById('auditResult').value);
    appendParam(params, 'actorNo', document.getElementById('auditActorNo').value.trim());
    appendParam(params, 'startDate', document.getElementById('auditStartDate').value);

    try {
        // 감사 로그 조회 요청
        const response = await fetch('/api/management/audit-logs?' + params.toString(), {
            headers: token ? {Authorization: 'Bearer ' + token} : {}
        });

        // 오류 응답이면 예외 처리로 이동
        if (!response.ok) {
            throw new Error('감사 로그 조회 실패: ' + response.status);
        }

        // 서버 응답을 현재 화면 상태에 저장
        const data = await response.json();
        auditCurrentLogs = data.content || [];
        auditCurrentPage = data.page || 0;
        auditTotalPages = data.totalPages || 0;

        // 테이블과 페이지 정보 렌더링
        renderAuditLogTable(auditCurrentLogs);
        renderAuditPageInfo(data);
    } catch (error) {
        // 오류 추적용 콘솔 기록 유지
        console.error(error);
        renderAuditError('감사 로그를 불러오지 못했습니다.');
    }
}

/* 비어 있지 않은 검색 조건 추가 처리 */
function appendParam(params, key, value) {
    if (value) {
        params.set(key, value);
    }
}

/* 감사 로그 테이블 렌더링 처리 */
function renderAuditLogTable(logs) {
    const tbody = document.getElementById('auditLogTableBody');

    // 조회 결과가 없으면 빈 상태 행 표시
    if (!logs.length) {
        tbody.innerHTML = '<tr><td class="audit-empty-row" colspan="7">조회된 감사 로그가 없습니다.</td></tr>';
        return;
    }

    // 조회 결과를 테이블 행 HTML로 변환
    tbody.innerHTML = logs.map((log, index) => `
        <tr onclick="showAuditLogDetail(${index})">
            <td>${escapeHtml(formatDateTime(log.occurredAt))}</td>
            <td>${escapeHtml(formatActor(log))}</td>
            <td><span class="audit-badge ${getActionClass(log.action)}">${escapeHtml(formatAction(log.action))}</span></td>
            <td>${escapeHtml(formatTargetType(log.targetType))}</td>
            <td>${escapeHtml(log.targetId || '-')}</td>
            <td>${escapeHtml(log.clientIp || '-')}</td>
            <td><span class="${getResultClass(log.result)}">${escapeHtml(log.result || '-')}</span></td>
        </tr>
    `).join('');
}

/* 감사 로그 페이지 정보 표시 처리 */
function renderAuditPageInfo(data) {
    document.getElementById('auditPageInfo').innerText =
        `${(data.page || 0) + 1} / ${data.totalPages || 1} (총 ${data.totalElements || 0}건)`;
}

/* 감사 로그 조회 오류 표시 처리 */
function renderAuditError(message) {
    const tbody = document.getElementById('auditLogTableBody');
    tbody.innerHTML = `<tr><td class="audit-empty-row" colspan="7">${escapeHtml(message)}</td></tr>`;

    // 페이지 정보도 빈 상태로 초기화 처리
    document.getElementById('auditPageInfo').innerText = '0 / 0';
}

/* 감사 로그 페이지 이동 처리 */
function moveAuditPage(delta) {
    const nextPage = auditCurrentPage + delta;

    // 조회 가능 범위를 벗어나면 요청하지 않음
    if (nextPage < 0 || nextPage >= auditTotalPages) {
        return;
    }

    loadAuditLogs(nextPage);
}

/* 감사 로그 상세 모달 표시 처리 */
function showAuditLogDetail(index) {
    const log = auditCurrentLogs[index];

    // 대상 로그가 없으면 모달 표시 중단
    if (!log) {
        return;
    }

    // 상세 모달 제목에 로그 ID 표시
    document.getElementById('auditDetailId').innerText = log.id;

    // 요청 관련 정보를 JSON 형태로 표시
    document.getElementById('auditRequestJson').innerText = JSON.stringify({
        traceId: log.traceId,
        httpMethod: log.httpMethod,
        requestUri: log.requestUri,
        clientIp: log.clientIp,
        userAgent: log.userAgent
    }, null, 2);

    // 처리 결과 관련 정보를 JSON 형태로 표시
    document.getElementById('auditResultJson').innerText = JSON.stringify({
        actorNo: log.actorNo,
        actorRole: log.actorRole,
        action: formatAction(log.action),
        targetType: formatTargetType(log.targetType),
        targetId: log.targetId,
        result: log.result,
        reason: log.reason,
        occurredAt: log.occurredAt,
        flushedAt: log.flushedAt
    }, null, 2);

    document.getElementById('auditLogModal').style.display = 'flex';
}

/* 감사 로그 상세 모달 닫기 처리 */
function closeAuditLogModal() {
    document.getElementById('auditLogModal').style.display = 'none';
}

/* 감사 로그 CSV 다운로드 처리 */
async function exportAuditCsv() {
    // 현재 필터 조건과 인증 토큰 준비
    const token = localStorage.getItem('accessToken');
    const params = new URLSearchParams();

    appendParam(params, 'action', document.getElementById('auditAction').value);
    appendParam(params, 'result', document.getElementById('auditResult').value);
    appendParam(params, 'actorNo', document.getElementById('auditActorNo').value.trim());
    appendParam(params, 'startDate', document.getElementById('auditStartDate').value);

    try {
        // CSV 파일 다운로드 요청
        const response = await fetch('/api/management/audit-logs/export.csv?' + params.toString(), {
            headers: token ? {Authorization: 'Bearer ' + token} : {}
        });

        // 다운로드 실패 응답이면 예외 처리로 이동
        if (!response.ok) {
            throw new Error('CSV 다운로드 실패: ' + response.status);
        }

        // Blob URL과 임시 링크로 파일 다운로드 처리
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'audit-logs.csv';
        document.body.appendChild(link);
        link.click();
        link.remove();

        URL.revokeObjectURL(url);
    } catch (error) {
        // 오류 추적용 콘솔 기록 유지
        console.error(error);
        alert('CSV 다운로드에 실패했습니다.');
    }
}

/* 감사 로그 행위자 표시값 변환 처리 */
function formatActor(log) {
    if (!log.actorNo && !log.actorRole) {
        return '-';
    }

    return `${log.actorNo || '-'} (${log.actorRole || '-'})`;
}

/* 날짜/시간 표시 형식 변환 처리 */
function formatDateTime(value) {
    // 값이 없으면 빈 날짜 표시
    if (!value) {
        return '-';
    }

    const date = new Date(value);

    // 해석할 수 없는 값은 원본 유지
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString('ko-KR');
}

/* 감사 로그 액션 표시값 변환 처리 */
function formatAction(action) {
    // DB enum 값은 유지하고 화면에는 한글 설명을 함께 표시
    const actionLabels = {
        CREATE: 'CREATE(생성)',
        READ: 'READ(조회)',
        UPDATE: 'UPDATE(수정)',
        DELETE: 'DELETE(삭제)',
        LOGIN: 'LOGIN(로그인)',
        LOGIN_FAIL: 'LOGIN_FAIL(로그인 실패)',
        LOGOUT: 'LOGOUT(로그아웃)',
        PERMISSION_CHANGE: 'PERMISSION_CHANGE(권한 변경)',
        AI_REQUEST: 'AI_REQUEST(AI 요청)',
        EXPORT: 'EXPORT(내보내기)'
    };

    return actionLabels[action] || action || '-';
}

/* 감사 대상 리소스 표시값 변환 처리 */
function formatTargetType(targetType) {
    // DB enum 값은 유지하고 화면에는 한글 설명을 함께 표시
    const targetTypeLabels = {
        AUTH: 'AUTH(인증)',
        EMPLOYEE: 'EMPLOYEE(직원)',
        ACCOUNT_SECURITY: 'ACCOUNT_SECURITY(계정 보안)',
        AUDIT_LOG: 'AUDIT_LOG(감사 로그)',
        BOARD_POST: 'BOARD_POST(게시글)',
        BOARD_COMMENT: 'BOARD_COMMENT(댓글)'
    };

    return targetTypeLabels[targetType] || targetType || '-';
}

/* 감사 액션 배지 클래스 선택 처리 */
function getActionClass(action) {
    if (action === 'CREATE') {
        return 'create';
    }

    if (action === 'READ') {
        return 'read';
    }

    if (action === 'UPDATE') {
        return 'update';
    }

    if (action === 'DELETE') {
        return 'delete';
    }

    if (action === 'LOGIN') {
        return 'login';
    }

    if (action === 'LOGIN_FAIL') {
        return 'login-fail';
    }

    if (action === 'LOGOUT') {
        return 'logout';
    }

    if (action === 'PERMISSION_CHANGE') {
        return 'permission';
    }

    if (action === 'AI_REQUEST') {
        return 'ai-request';
    }

    if (action === 'EXPORT') {
        return 'export';
    }

    // 알 수 없는 액션은 기본 read 색상 처리
    return 'read';
}

/* 감사 로그 결과 상태 클래스 선택 처리 */
function getResultClass(result) {
    return result === 'FAIL' ? 'audit-status-fail' : 'audit-status-success';
}

/* HTML 특수 문자 이스케이프 처리 */
function escapeHtml(value) {
    // 서버 응답 값을 문자열로 변환한 뒤 스크립트 삽입 방지
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

/* 모달 배경 클릭 시 상세 모달 닫기 처리 */
window.addEventListener('click', function (event) {
    const modal = document.getElementById('auditLogModal');

    if (event.target === modal) {
        closeAuditLogModal();
    }
});
