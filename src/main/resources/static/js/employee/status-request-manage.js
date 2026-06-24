// 현재 모달에서 처리할 신청 ID
let selectedRequestId = null;

// 현재 모달에서 수행할 처리 유형
let selectedDecisionAction = null;

/* 상태 신청 관리 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', function () {
    // Lucide가 로드되어 있으면 아이콘 렌더링
    if (window.lucide) {
        lucide.createIcons();
    }

    loadPendingRequests();
});

/* 승인 대기 상태 신청 목록 조회 처리 */
async function loadPendingRequests() {
    // 인증 토큰과 목록/메시지 영역 준비
    const accessToken = localStorage.getItem('accessToken');
    const body = document.getElementById('pending-request-body');
    const messageArea = document.getElementById('status-request-message');

    hideMessage(messageArea);

    // 토큰이 없으면 인증 정보 정리 후 로그인 화면 이동
    if (!accessToken) {
        clearStoredAuth();
        window.location.href = '/login';
        return;
    }

    try {
        // 승인 대기 상태 신청 목록 조회 요청
        const response = await fetch('/api/management/status-requests', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        const data = await response.json();

        // SUPER 권한이 아니면 권한 안내 행 표시
        if (response.status === 403) {
            body.innerHTML = '<tr><td colspan="6" class="empty-state">SUPER 권한만 신청 목록을 확인할 수 있습니다.</td></tr>';
            return;
        }

        // 정상 응답이 아니면 오류 메시지와 빈 상태 행 표시
        if (!response.ok) {
            body.innerHTML = '<tr><td colspan="6" class="empty-state">승인 대기 목록을 불러오지 못했습니다.</td></tr>';
            showMessage(messageArea, data.message || '승인 대기 목록 조회에 실패했습니다.', 'error');
            return;
        }

        // 조회된 신청 목록 테이블 렌더링
        renderPendingRequests(data);
    } catch (error) {
        // 서버 통신 실패 시 빈 상태 행 표시
        body.innerHTML = '<tr><td colspan="6" class="empty-state">서버와 통신하지 못했습니다.</td></tr>';
    }
}

/* 승인 대기 신청 테이블 렌더링 처리 */
function renderPendingRequests(requests) {
    const body = document.getElementById('pending-request-body');

    // 신청 목록이 없으면 빈 상태 행 표시
    if (!requests || requests.length === 0) {
        body.innerHTML = '<tr><td colspan="6" class="empty-state">현재 승인 대기 중인 신청이 없습니다.</td></tr>';
        return;
    }

    // 신청 목록을 테이블 행 문자열로 변환
    body.innerHTML = requests.map(function (request) {
        return `
            <tr>
                <td>
                    <div class="employee-main">${escapeHtml(request.employeeName)} (${escapeHtml(request.employeeNo)})</div>
                    <div class="employee-sub">${escapeHtml(request.department)} · ${escapeHtml(request.position)}</div>
                </td>
                <td>${renderRequestTypeBadge(request.requestType)}</td>
                <td>${formatEmployeeStatus(request.employeeStatus)}</td>
                <td>${formatDateTime(request.requestedAt)}</td>
                <td class="reason-cell">${escapeHtml(request.reason || '-')}</td>
                <td>
                    <div class="action-group">
                        <button type="button" class="action-btn approve-btn" onclick="openDecisionModal(${request.id}, 'approve')">승인</button>
                        <button type="button" class="action-btn reject-btn" onclick="openDecisionModal(${request.id}, 'reject')">반려</button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

/* 승인/반려 처리 모달 열기 처리 */
function openDecisionModal(requestId, action) {
    // 처리 대상 신청 ID와 처리 유형 저장
    selectedRequestId = requestId;
    selectedDecisionAction = action;

    // 모달 관련 DOM 조회
    const modal = document.getElementById('decision-modal');
    const title = document.getElementById('decision-modal-title');
    const confirmBtn = document.getElementById('decision-confirm-btn');
    const comment = document.getElementById('decision-comment');

    // 처리 유형에 맞춰 모달 제목과 확인 버튼 문구 변경
    title.innerText = action === 'approve' ? '신청 승인' : '신청 반려';
    confirmBtn.innerText = action === 'approve' ? '승인' : '반려';

    // 이전 코멘트 초기화
    comment.value = '';

    // 모달 열기
    modal.classList.add('open');
}

/* 신청 처리 모달 닫기 처리 */
function closeDecisionModal() {
    // 선택된 신청 ID와 처리 유형 초기화
    selectedRequestId = null;
    selectedDecisionAction = null;

    // 모달 닫기
    document.getElementById('decision-modal').classList.remove('open');
}

/* 승인/반려 결정 제출 처리 */
async function submitDecision() {
    // 인증 토큰, 메시지 영역, 확인 버튼 조회
    const accessToken = localStorage.getItem('accessToken');
    const messageArea = document.getElementById('status-request-message');
    const confirmBtn = document.getElementById('decision-confirm-btn');

    // 처리 대상이나 처리 유형이 없으면 모달만 닫고 중단
    if (!selectedRequestId || !selectedDecisionAction) {
        closeDecisionModal();
        return;
    }

    // 중복 제출 방지와 이전 메시지 초기화
    confirmBtn.disabled = true;
    hideMessage(messageArea);

    try {
        // 선택된 승인/반려 요청
        const response = await fetch(`/api/management/status-requests/${selectedRequestId}/${selectedDecisionAction}`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + accessToken
            },
            body: JSON.stringify({
                decisionComment: document.getElementById('decision-comment').value
            })
        });

        const data = await response.json();

        // 처리 실패 시 오류 메시지 표시
        if (!response.ok) {
            showMessage(messageArea, data.message || '신청 처리에 실패했습니다.', 'error');
            return;
        }

        // 처리 성공 메시지 표시 후 모달 닫고 목록 새로고침
        showMessage(messageArea, '신청 처리가 완료되었습니다.', 'success');
        closeDecisionModal();
        loadPendingRequests();
    } catch (error) {
        // 서버 통신 실패 메시지 표시
        showMessage(messageArea, '서버와 통신하지 못했습니다.', 'error');
    } finally {
        // 처리 종료 후 확인 버튼 다시 활성화
        confirmBtn.disabled = false;
    }
}

/* 신청 유형 배지 HTML 변환 처리 */
function renderRequestTypeBadge(type) {
    // 신청 유형별 화면 문구 매핑
    const labelMap = {
        LEAVE: '휴직 신청',
        RESIGN: '퇴사 신청',
        RETURN_FROM_LEAVE: '복직 신청'
    };

    // 신청 유형별 CSS 클래스 매핑
    const classNameMap = {
        LEAVE: 'leave',
        RESIGN: 'resign',
        RETURN_FROM_LEAVE: 'return'
    };

    // 알 수 없는 값이면 원본 값이나 '-' 표시
    const label = labelMap[type] || type || '-';
    const className = classNameMap[type] || 'leave';
    return `<span class="badge ${className}">${label}</span>`;
}

/* 직원 재직 상태 표시값 변환 처리 */
function formatEmployeeStatus(status) {
    // 재직 상태별 화면 문구 매핑
    const labelMap = {
        ACTIVE: '재직',
        ON_LEAVE: '휴직',
        RESIGNED: '퇴사'
    };

    // 알 수 없는 값이면 원본 값이나 '-' 표시
    return labelMap[status] || status || '-';
}

/* 날짜/시간 표시 형식 변환 처리 */
function formatDateTime(value) {
    // 값이 없으면 빈 날짜 표시
    if (!value) return '-';

    const date = new Date(value);

    // 해석할 수 없는 값은 원본 유지
    if (Number.isNaN(date.getTime())) return value;

    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/* 공통 메시지 표시 처리 */
function showMessage(target, message, type) {
    // 메시지 문구와 상태 클래스 반영
    target.innerText = message;
    target.className = 'message-area ' + type;
}

/* 공통 메시지 초기화 처리 */
function hideMessage(target) {
    // 메시지 문구와 상태 클래스 초기화
    target.innerText = '';
    target.className = 'message-area';
}

/* HTML 특수 문자 이스케이프 처리 */
function escapeHtml(value) {
    // HTML 의미 문자를 엔티티로 변환해 스크립트 삽입 방지
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
