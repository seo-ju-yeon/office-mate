/* 마이페이지 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', function () {
    // Lucide가 로드되어 있으면 현재 화면 아이콘 렌더링
    if (window.lucide) {
        lucide.createIcons();
    }

    // 현재 로그인 사용자 정보와 재직 상태 신청 이력 조회
    loadMyProfile();
    loadStatusRequestHistory();

    // 비밀번호 변경과 재직 상태 신청 form 이벤트 연결
    document.getElementById('password-form').addEventListener('submit', changePassword);
    document.getElementById('status-request-form').addEventListener('submit', createStatusRequest);
});

/* 내 프로필 정보 조회 처리 */
async function loadMyProfile() {
    // 마이페이지 접근에 필요한 인증 토큰 조회
    const accessToken = localStorage.getItem('accessToken');

    // 토큰이 없으면 로그인 화면 이동 처리
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 현재 로그인 사용자 정보 조회 요청
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        const data = await response.json();

        // 인증 실패 시 토큰 정리 후 로그인 화면 이동
        if (!response.ok) {
            clearTokens();
            window.location.href = '/login';
            return;
        }

        // 프로필 정보 영역에 사용자 정보 반영
        document.getElementById('employee-no').innerText = data.employeeNo;
        document.getElementById('employee-name').innerText = data.name;
        document.getElementById('employee-department').innerText = data.department;
        document.getElementById('employee-position').innerText = data.position;
        document.getElementById('employee-role').innerText = data.role;

        // 레이아웃 상단 사용자 이름 갱신
        const layoutUserName = document.getElementById('layout-user-name');
        if (layoutUserName) {
            layoutUserName.innerText = data.name;
        }
    } catch (error) {
        // 조회 실패 시 인증 정보 정리 후 로그인 화면 이동
        clearTokens();
        window.location.href = '/login';
    }
}

/* 마이페이지 비밀번호 변경 요청 처리 */
async function changePassword(event) {
    // 기본 form 제출 대신 fetch 요청 사용
    event.preventDefault();

    // 입력값, 메시지 영역, 인증 토큰 준비
    const currentPassword = document.getElementById('current-password').value;
    const newPassword = document.getElementById('new-password').value;
    const confirmPassword = document.getElementById('confirm-password').value;
    const messageArea = document.getElementById('password-message');
    const accessToken = localStorage.getItem('accessToken');

    // 이전 메시지 초기화
    hideMessage(messageArea);

    // 새 비밀번호와 확인값이 다르면 서버 호출 전 중단
    if (newPassword !== confirmPassword) {
        showMessage(messageArea, '새 비밀번호가 일치하지 않습니다.', 'error');
        return;
    }

    // 토큰이 없으면 인증 정보 정리 후 로그인 화면 이동
    if (!accessToken) {
        clearTokens();
        window.location.href = '/login';
        return;
    }

    try {
        // 비밀번호 변경 요청
        const response = await fetch('/api/auth/change-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + accessToken
            },
            body: JSON.stringify({
                currentPassword: currentPassword,
                newPassword: newPassword
            })
        });

        const data = await response.json();

        // 비밀번호 불일치 또는 정책 오류 메시지 표시
        if (!response.ok) {
            showMessage(messageArea, data.message || '비밀번호 변경에 실패했습니다.', 'error');
            return;
        }

        // 성공 메시지 표시 후 인증 정보 제거와 로그인 화면 이동
        showMessage(messageArea, '비밀번호가 변경되었습니다. 다시 로그인해주세요.', 'success');
        clearTokens();
        setTimeout(function () {
            window.location.href = '/login';
        }, 900);
    } catch (error) {
        // 네트워크 오류 또는 서버 미응답 메시지 표시
        showMessage(messageArea, '서버와 통신하지 못했습니다.', 'error');
    }
}

/* 재직 상태 신청 이력 조회 처리 */
async function loadStatusRequestHistory() {
    // 신청 이력 조회에 필요한 인증 토큰과 테이블 본문 조회
    const accessToken = localStorage.getItem('accessToken');
    const historyBody = document.getElementById('status-request-history');

    // 토큰이 없으면 인증 정보 정리 후 로그인 화면 이동
    if (!accessToken) {
        clearTokens();
        window.location.href = '/login';
        return;
    }

    try {
        // 본인 재직 상태 신청 이력 조회 요청
        const response = await fetch('/api/my/status-requests', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        const data = await response.json();

        // 조회 실패 시 테이블에 실패 안내 표시
        if (!response.ok) {
            historyBody.innerHTML = '<tr><td colspan="5" class="empty-text">신청 이력을 불러오지 못했습니다.</td></tr>';
            return;
        }

        // 조회된 신청 이력 테이블 렌더링
        renderStatusRequestHistory(data);
    } catch (error) {
        // 네트워크 오류 또는 서버 미응답 안내 표시
        historyBody.innerHTML = '<tr><td colspan="5" class="empty-text">서버와 통신하지 못했습니다.</td></tr>';
    }
}

/* 재직 상태 변경 신청 요청 처리 */
async function createStatusRequest(event) {
    // 기본 form 제출 대신 fetch 요청 사용
    event.preventDefault();

    // 인증 토큰, 메시지 영역, 제출 버튼 조회
    const accessToken = localStorage.getItem('accessToken');
    const messageArea = document.getElementById('status-request-message');
    const submitButton = event.target.querySelector('button[type="submit"]');

    // 이전 메시지 초기화
    hideMessage(messageArea);

    // 토큰이 없으면 인증 정보 정리 후 로그인 화면 이동
    if (!accessToken) {
        clearTokens();
        window.location.href = '/login';
        return;
    }

    // 중복 제출 방지를 위한 버튼 비활성화
    submitButton.disabled = true;

    try {
        // 신청 유형과 사유로 본인 신청 생성 요청
        const response = await fetch('/api/my/status-requests', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + accessToken
            },
            body: JSON.stringify({
                requestType: document.getElementById('request-type').value,
                reason: document.getElementById('request-reason').value
            })
        });

        const data = await response.json();

        // 신청 생성 실패 시 서버 메시지 우선 표시
        if (!response.ok) {
            showMessage(messageArea, data.message || '신청을 생성하지 못했습니다.', 'error');
            return;
        }

        // 성공 메시지 표시 후 폼 초기화와 신청 이력 재조회
        showMessage(messageArea, '신청이 제출되었습니다.', 'success');
        document.getElementById('status-request-form').reset();
        loadStatusRequestHistory();
    } catch (error) {
        // 네트워크 오류 또는 서버 미응답 메시지 표시
        showMessage(messageArea, '서버와 통신하지 못했습니다.', 'error');
    } finally {
        // 요청 완료 후 제출 버튼 재활성화
        submitButton.disabled = false;
    }
}

/* 재직 상태 신청 이력 테이블 렌더링 처리 */
function renderStatusRequestHistory(requests) {
    // 신청 이력을 표시할 tbody 조회
    const historyBody = document.getElementById('status-request-history');

    // 이력이 없으면 빈 상태 안내 표시
    if (!requests || requests.length === 0) {
        historyBody.innerHTML = '<tr><td colspan="5" class="empty-text">아직 신청 이력이 없습니다.</td></tr>';
        return;
    }

    // 신청 이력을 테이블 행 문자열로 변환
    historyBody.innerHTML = requests.map(function (request) {
        return `
            <tr>
                <td>${formatDateTime(request.requestedAt)}</td>
                <td>${escapeHtml(formatRequestType(request.requestType))}</td>
                <td>${renderRequestStatusBadge(request.status)}</td>
                <td>${escapeHtml(request.decidedByName || '-')}</td>
                <td>${escapeHtml(request.decisionComment || '-')}</td>
            </tr>
        `;
    }).join('');
}

/* 신청 유형 표시값 변환 처리 */
function formatRequestType(type) {
    // 휴직/퇴사/복직 신청 유형을 한글 문구로 변환
    if (type === 'LEAVE') {
        return '휴직 신청';
    }
    if (type === 'RESIGN') {
        return '퇴사 신청';
    }
    if (type === 'RETURN_FROM_LEAVE') {
        return '복직 신청';
    }

    // 알 수 없는 값은 원본 값 또는 '-' 표시
    return type || '-';
}

/* 신청 처리 상태 배지 HTML 변환 처리 */
function renderRequestStatusBadge(status) {
    // 상태별 CSS 클래스와 화면 표시 문구 정의
    const classMap = {
        PENDING: 'pending',
        APPROVED: 'approved',
        REJECTED: 'rejected'
    };
    const labelMap = {
        PENDING: '승인 대기',
        APPROVED: '승인 완료',
        REJECTED: '반려'
    };

    // 정의된 상태만 CSS 클래스로 사용
    const className = classMap[status] || '';
    const label = labelMap[status] || status || '-';

    // 배지 문구는 이스케이프 후 삽입
    return `<span class="badge ${className}">${escapeHtml(label)}</span>`;
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

    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/* HTML 특수 문자 이스케이프 처리 */
function escapeHtml(value) {
    // null 계열 값은 빈 문자열로 표시
    if (value === null || value === undefined) {
        return '';
    }

    // HTML 의미 문자를 엔티티로 변환
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

/* 공통 메시지 표시 처리 */
function showMessage(target, message, type) {
    // 메시지 문구와 타입 클래스 적용
    target.innerText = message;
    target.className = 'message-area ' + type;
}

/* 공통 메시지 초기화 처리 */
function hideMessage(target) {
    // 메시지 문구와 상태 클래스 초기화
    target.innerText = '';
    target.className = 'message-area';
}

/* 브라우저 로그인 정보 제거 처리 */
function clearTokens() {
    // localStorage 인증/사용자 정보 제거
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('employeeNo');
    localStorage.removeItem('employeeName');
    localStorage.removeItem('role');
}
