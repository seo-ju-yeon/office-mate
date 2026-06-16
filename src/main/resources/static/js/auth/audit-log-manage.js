// 현재 조회 중인 페이지 번호
let auditCurrentPage = 0;

// 서버가 내려준 전체 페이지 수
let auditTotalPages = 0;

// 현재 화면에 렌더링된 감사 로그 목록. 상세 모달에서 재사용
let auditCurrentLogs = [];

/* 감사 로그 화면 최초 진입 시 첫 페이지 조회 이벤트를 등록하는 초기화 메서드 */
document.addEventListener('DOMContentLoaded', function () {
    // 화면 최초 진입 시 첫 페이지 감사 로그 조회
    loadAuditLogs(0);
});

/* 감사 로그 목록을 현재 필터 조건과 페이지 번호 기준으로 조회하는 메서드 */
async function loadAuditLogs(page) {
    // API 인증을 위해 로그인 시 저장된 accessToken 조회
    const token = localStorage.getItem('accessToken');
    const params = new URLSearchParams();

    // 조회 API에 전달할 페이지/필터 조건 생성
    auditCurrentPage = Math.max(page, 0);
    params.set('page', auditCurrentPage);
    params.set('size', '20');
    appendParam(params, 'action', document.getElementById('auditAction').value);
    appendParam(params, 'result', document.getElementById('auditResult').value);
    appendParam(params, 'actorNo', document.getElementById('auditActorNo').value.trim());
    appendParam(params, 'startDate', document.getElementById('auditStartDate').value);

    try {
        // 감사 로그 조회 API 호출
        const response = await fetch('/api/management/audit-logs?' + params.toString(), {
            headers: token ? {Authorization: 'Bearer ' + token} : {}
        });

        // 401/403/500 등 정상 응답이 아니면 catch로 넘김
        if (!response.ok) {
            throw new Error('감사 로그 조회 실패: ' + response.status);
        }

        // 서버 응답을 현재 화면 상태에 저장하고 테이블/페이지 정보 갱신
        const data = await response.json();
        auditCurrentLogs = data.content || [];
        auditCurrentPage = data.page || 0;
        auditTotalPages = data.totalPages || 0;

        renderAuditLogTable(auditCurrentLogs);
        renderAuditPageInfo(data);
    } catch (error) {
        // 조회 실패 시 콘솔에는 원인을 남기고 화면에는 사용자용 메시지 표시
        console.error(error);
        renderAuditError('감사 로그를 불러오지 못했습니다.');
    }
}

/* 비어 있지 않은 검색 조건만 URLSearchParams에 추가하는 메서드 */
function appendParam(params, key, value) {
    // 값이 있는 필터만 query string에 추가
    if (value) {
        params.set(key, value);
    }
}

/* 조회된 감사 로그 목록을 테이블 행 HTML로 렌더링하는 메서드 */
function renderAuditLogTable(logs) {
    // 감사 로그 목록을 삽입할 tbody 요소 조회
    const tbody = document.getElementById('auditLogTableBody');

    // 조회 결과가 없으면 빈 결과 안내 행 표시
    if (!logs.length) {
        tbody.innerHTML = '<tr><td class="audit-empty-row" colspan="7">조회된 감사 로그가 없습니다.</td></tr>';
        return;
    }

    // 조회 결과를 테이블 row HTML로 변환
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

/* 현재 페이지, 전체 페이지, 전체 건수를 화면 하단에 표시하는 메서드 */
function renderAuditPageInfo(data) {
    // 페이지 정보 텍스트를 계산해서 페이지네이션 영역에 반영
    document.getElementById('auditPageInfo').innerText =
        `${(data.page || 0) + 1} / ${data.totalPages || 1} (총 ${data.totalElements || 0}건)`;
}

/* 조회 실패 또는 오류 상황을 테이블 영역에 표시하는 메서드 */
function renderAuditError(message) {
    // 오류 메시지를 테이블 빈 행으로 표시
    const tbody = document.getElementById('auditLogTableBody');
    tbody.innerHTML = `<tr><td class="audit-empty-row" colspan="7">${escapeHtml(message)}</td></tr>`;

    // 페이지 정보도 빈 상태로 초기화
    document.getElementById('auditPageInfo').innerText = '0 / 0';
}

/* 이전/다음 버튼 클릭 시 감사 로그 페이지를 이동하는 메서드 */
function moveAuditPage(delta) {
    // 현재 페이지 기준으로 이동할 페이지 번호 계산
    const nextPage = auditCurrentPage + delta;

    // 범위를 벗어난 페이지 요청은 무시
    if (nextPage < 0 || nextPage >= auditTotalPages) {
        return;
    }

    // 유효한 페이지면 해당 페이지 감사 로그 조회
    loadAuditLogs(nextPage);
}

/* 선택한 감사 로그 행의 상세 정보를 모달에 표시하는 메서드 */
function showAuditLogDetail(index) {
    // 테이블에서 클릭한 row의 로그 데이터를 현재 목록에서 조회
    const log = auditCurrentLogs[index];

    // 대상 로그가 없으면 상세 모달 표시 중단
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

    // 상세 모달을 화면에 표시
    document.getElementById('auditLogModal').style.display = 'flex';
}

/* 감사 로그 상세 모달을 닫는 메서드 */
function closeAuditLogModal() {
    // 상세 모달을 숨김 처리
    document.getElementById('auditLogModal').style.display = 'none';
}

/* 현재 필터 조건을 적용해 감사 로그 CSV 파일을 다운로드하는 메서드 */
async function exportAuditCsv() {
    // CSV 다운로드 API에도 JWT 인증 헤더가 필요하므로 accessToken 조회
    const token = localStorage.getItem('accessToken');
    const params = new URLSearchParams();

    // 현재 화면 필터 조건을 CSV 다운로드에도 동일하게 적용
    appendParam(params, 'action', document.getElementById('auditAction').value);
    appendParam(params, 'result', document.getElementById('auditResult').value);
    appendParam(params, 'actorNo', document.getElementById('auditActorNo').value.trim());
    appendParam(params, 'startDate', document.getElementById('auditStartDate').value);

    try {
        // CSV 파일 다운로드 API 호출
        const response = await fetch('/api/management/audit-logs/export.csv?' + params.toString(), {
            headers: token ? {Authorization: 'Bearer ' + token} : {}
        });

        // 다운로드 실패 응답이면 catch로 넘김
        if (!response.ok) {
            throw new Error('CSV 다운로드 실패: ' + response.status);
        }

        // CSV 응답을 Blob으로 변환
        const blob = await response.blob();
        const url = URL.createObjectURL(blob);

        // 임시 링크를 만들어 클릭시킨 뒤 다운로드 시작
        const link = document.createElement('a');
        link.href = url;
        link.download = 'audit-logs.csv';
        document.body.appendChild(link);
        link.click();
        link.remove();

        // 임시 Blob URL 해제
        URL.revokeObjectURL(url);
    } catch (error) {
        // 실패 원인을 콘솔에 남기고 사용자에게 알림 표시
        console.error(error);
        alert('CSV 다운로드에 실패했습니다.');
    }
}

/* 행위자 사번과 역할을 한 칸에 표시하기 위한 문자열로 변환하는 메서드 */
function formatActor(log) {
    // 행위자 사번과 역할이 모두 없으면 '-' 표시
    if (!log.actorNo && !log.actorRole) {
        return '-';
    }

    // 사번과 역할을 함께 표시
    return `${log.actorNo || '-'} (${log.actorRole || '-'})`;
}

/* 서버에서 받은 날짜 문자열을 한국어 로케일 기준으로 변환하는 메서드 */
function formatDateTime(value) {
    // 날짜 값이 없으면 '-' 표시
    if (!value) {
        return '-';
    }

    // 서버 날짜 값을 Date 객체로 변환
    const date = new Date(value);

    // 브라우저가 해석하지 못하는 날짜 값이면 원본 값 반환
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    // 유효한 날짜 값이면 한국어 로케일 문자열로 변환
    return date.toLocaleString('ko-KR');
}

/* 감사 로그 액션 enum 값을 화면 표시용 라벨로 변환하는 메서드 */
function formatAction(action) {
    // DB enum 값은 유지하고, 화면에서는 영어(한글) 형태로 표시
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

    // 매핑된 라벨이 있으면 사용하고 없으면 원본 값 또는 '-' 표시
    return actionLabels[action] || action || '-';
}

/* 감사 대상 리소스 타입 enum 값을 화면 표시용 라벨로 변환하는 메서드 */
function formatTargetType(targetType) {
    // 감사 대상 리소스도 화면에서만 한글 설명을 덧붙임
    const targetTypeLabels = {
        AUTH: 'AUTH(인증)',
        EMPLOYEE: 'EMPLOYEE(직원)',
        ACCOUNT_SECURITY: 'ACCOUNT_SECURITY(계정 보안)',
        AUDIT_LOG: 'AUDIT_LOG(감사 로그)',
        BOARD_POST: 'BOARD_POST(게시글)',
        BOARD_COMMENT: 'BOARD_COMMENT(댓글)'
    };

    // 매핑된 라벨이 있으면 사용하고 없으면 원본 값 또는 '-' 표시
    return targetTypeLabels[targetType] || targetType || '-';
}

/* 감사 액션 타입에 따라 배지 색상 클래스를 선택하는 메서드 */
function getActionClass(action) {
    // 생성 액션은 create 색상 사용
    if (action === 'CREATE') {
        return 'create';
    }

    // 조회 액션은 read 색상 사용
    if (action === 'READ') {
        return 'read';
    }

    // 수정 액션은 update 색상 사용
    if (action === 'UPDATE') {
        return 'update';
    }

    // 삭제 액션은 delete 색상 사용
    if (action === 'DELETE') {
        return 'delete';
    }

    // 로그인 액션은 login 색상 사용
    if (action === 'LOGIN') {
        return 'login';
    }

    // 로그인 실패 액션은 login-fail 색상 사용
    if (action === 'LOGIN_FAIL') {
        return 'login-fail';
    }

    // 로그아웃 액션은 logout 색상 사용
    if (action === 'LOGOUT') {
        return 'logout';
    }

    // 권한 변경 액션은 permission 색상 사용
    if (action === 'PERMISSION_CHANGE') {
        return 'permission';
    }

    // AI 요청 액션은 ai-request 색상 사용
    if (action === 'AI_REQUEST') {
        return 'ai-request';
    }

    // 내보내기 액션은 export 색상 사용
    if (action === 'EXPORT') {
        return 'export';
    }

    // 알 수 없는 액션은 기본 read 색상 사용
    return 'read';
}

/* 감사 로그 결과 값에 따라 성공/실패 CSS 클래스를 반환하는 메서드 */
function getResultClass(result) {
    // 실패 로그는 빨간색, 그 외 결과는 성공 색상으로 표시
    return result === 'FAIL' ? 'audit-status-fail' : 'audit-status-success';
}

/* 서버 응답 문자열을 HTML에 안전하게 삽입하기 위해 특수문자를 이스케이프하는 메서드 */
function escapeHtml(value) {
    // 서버 응답 값이 null/undefined여도 문자열로 변환해 처리
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

/* 모달 바깥 영역 클릭 시 상세 모달을 닫는 이벤트 메서드 */
window.addEventListener('click', function (event) {
    // 현재 상세 모달 DOM 조회
    const modal = document.getElementById('auditLogModal');

    // 클릭 대상이 모달 배경이면 상세 모달 닫기
    if (event.target === modal) {
        closeAuditLogModal();
    }
});
