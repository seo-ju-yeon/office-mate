// 현재 조회 중인 페이지 번호
let accountSecurityCurrentPage = 0;

// 서버가 내려준 전체 페이지 수
let accountSecurityTotalPages = 0;

// 계정 보안 관리 목록의 페이지당 조회 개수
const ACCOUNT_SECURITY_PAGE_SIZE = 10;

/* 계정 보안 관리 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', function () {
    // 접근 권한 확인 후 초기 목록 조회 처리
    checkAccountSecurityPageAccess();

    // 검색어 입력 후 Enter 키로 재조회 처리
    const keywordInput = document.getElementById('keyword-input');
    keywordInput.addEventListener('keydown', function (event) {
        if (event.key === 'Enter') {
            loadAccountSecurityStatuses(0);
        }
    });
});

/* 계정 보안 관리 접근 권한 검증 처리 */
async function checkAccountSecurityPageAccess() {
    // 인증 토큰 없으면 로그인 화면으로 이동 처리
    const accessToken = localStorage.getItem('accessToken');

    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 현재 로그인 사용자 정보 조회 요청
        const response = await fetch('/api/auth/me', {
            headers: {'Authorization': 'Bearer ' + accessToken}
        });

        // 인증 실패 또는 비정상 응답 시 로그인 화면 이동 처리
        if (!response.ok) {
            window.location.href = '/login';
            return;
        }

        const data = await response.json();

        // 관리자 권한이 아니면 안내 후 대시보드로 이동 처리
        if (data.role !== 'ADMIN' && data.role !== 'SUPER') {
            showMessage('계정 보안 관리는 관리자만 접근할 수 있습니다.', 'error');
            setTimeout(function () {
                window.location.href = '/dashboard';
            }, 800);
            return;
        }

        // 권한 확인 후 첫 페이지 목록 조회
        loadAccountSecurityStatuses(0);
    } catch (error) {
        // 사용자 정보 조회 실패 메시지 표시
        showMessage('계정 보안 관리 화면을 불러오지 못했습니다.', 'error');
    }
}

/* 계정 보안 상태 목록 조회 처리 */
async function loadAccountSecurityStatuses(page) {
    // 조회 조건과 인증 토큰 준비
    const accessToken = localStorage.getItem('accessToken');
    const keyword = document.getElementById('keyword-input').value.trim();
    const lockedOnly = document.getElementById('locked-only-input').checked;
    const params = new URLSearchParams();
    const nextPage = Math.max(page || 0, 0);

    // 서버 페이징 조회에 사용할 page/size 쿼리 파라미터 추가
    params.set('page', nextPage);
    params.set('size', ACCOUNT_SECURITY_PAGE_SIZE);

    // 검색어 조건 추가 처리
    if (keyword) {
        params.set('keyword', keyword);
    }

    // 잠긴 계정만 보기 조건 추가 처리
    if (lockedOnly) {
        params.set('lockedOnly', 'true');
    }

    // 최종 조회 API 주소 생성
    const queryString = params.toString();
    const url = '/api/management/account-security' + (queryString ? '?' + queryString : '');

    try {
        // 계정 보안 상태 목록 조회 요청
        const response = await fetch(url, {
            headers: {'Authorization': 'Bearer ' + accessToken}
        });

        // 오류 응답이면 서버 메시지 우선 표시 처리
        if (!response.ok) {
            const error = await response.json().catch(function () {
                return {};
            });
            throw new Error(error.message || '계정 보안 상태를 조회하지 못했습니다.');
        }

        const pageData = await response.json();
        const items = pageData.content || [];

        // 목록, 요약, 페이지 정보를 화면에 반영
        renderAccountSecurityTable(items);
        updateSummary(items, pageData);
        renderPagination(pageData);

        showMessage('계정 보안 상태를 조회했습니다.', 'success');
    } catch (error) {
        // 조회 실패 시 화면 데이터를 빈 상태로 초기화
        renderAccountSecurityTable([]);
        updateSummary([], {
            page: 0,
            totalPages: 0,
            totalElements: 0
        });
        renderPagination({
            page: 0,
            totalPages: 0,
            totalElements: 0
        });

        showMessage(error.message, 'error');
    }
}

/* 계정 보안 상태 테이블 렌더링 처리 */
function renderAccountSecurityTable(items) {
    const tableBody = document.getElementById('account-security-table-body');

    // 조회 결과가 없으면 빈 상태 행 표시
    if (!items || items.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="9" class="empty-row">조회 결과가 없습니다.</td></tr>';
        return;
    }

    // 조회된 계정 목록을 테이블 행 문자열로 변환
    tableBody.innerHTML = items.map(function (item) {
        // 잠금 여부에 따른 상태 배지와 버튼 처리
        const badgeClass = item.accountLocked ? 'locked' : 'normal';
        const badgeText = item.accountLocked ? '잠김' : '정상';
        const unlockButton = item.accountLocked
            ? `<button type="button" class="btn-danger" onclick="unlockAccount('${item.employeeNo}')">
                   <i data-lucide="lock-open"></i>
                   해제
               </button>`
            : `<button type="button" class="btn-secondary" disabled>
                   <i data-lucide="check"></i>
                   정상
               </button>`;

        // 서버 문자열은 escapeHtml 처리 후 출력
        return `
            <tr>
                <td>
                    <div class="employee-no">${escapeHtml(item.employeeNo)}</div>
                    <div class="employee-meta">${escapeHtml(item.name)}</div>
                </td>
                <td>
                    <div>${escapeHtml(item.departmentName)}</div>
                    <div class="employee-meta">${escapeHtml(item.positionName)}</div>
                </td>
                <td>${escapeHtml(item.roleName)}</td>
                <td>${item.loginFailCount}</td>
                <td><span class="status-badge ${badgeClass}">${badgeText}</span></td>
                <td>${formatDateTime(item.lastFailedAt)}</td>
                <td>${formatDateTime(item.lockedAt)}</td>
                <td>
                    <div>${formatDateTime(item.unlockedAt)}</div>
                    <div class="employee-meta">${escapeHtml(item.unlockedBy || '-')}</div>
                </td>
                <td>${unlockButton}</td>
            </tr>
        `;
    }).join('');

    // 동적 HTML 삽입 후 Lucide 아이콘 렌더링
    lucide.createIcons();
}

/* 계정 잠금 해제 요청 처리 */
async function unlockAccount(employeeNo) {
    // 실수 방지를 위한 잠금 해제 확인 처리
    if (!confirm(employeeNo + ' 계정의 잠금을 해제하시겠습니까?')) {
        return;
    }

    const accessToken = localStorage.getItem('accessToken');

    try {
        // 사번 인코딩 후 잠금 해제 API 요청
        const response = await fetch('/api/management/account-security/' + encodeURIComponent(employeeNo) + '/unlock', {
            method: 'PATCH',
            headers: {'Authorization': 'Bearer ' + accessToken}
        });

        // 오류 응답이면 서버 메시지 우선 표시 처리
        if (!response.ok) {
            const error = await response.json().catch(function () {
                return {};
            });
            throw new Error(error.message || '계정 잠금 해제에 실패했습니다.');
        }

        showMessage(employeeNo + ' 계정 잠금을 해제했습니다.', 'success');

        // 현재 페이지 재조회로 변경 상태 반영
        loadAccountSecurityStatuses(accountSecurityCurrentPage);
    } catch (error) {
        showMessage(error.message, 'error');
    }
}

/* 계정 보안 요약 정보 갱신 처리 */
function updateSummary(items, pageData) {
    // 전체 건수는 서버 페이지 응답 기준으로 표시
    const totalCount = pageData.totalElements || 0;

    // 잠김/정상 수는 현재 페이지 목록 기준으로 계산
    const lockedCount = items.filter(function (item) {
        return item.accountLocked;
    }).length;

    document.getElementById('total-count').innerText = totalCount;
    document.getElementById('locked-count').innerText = lockedCount;
    document.getElementById('normal-count').innerText = items.length - lockedCount;
}

/* 페이지 정보와 이전/다음 버튼 렌더링 처리 */
function renderPagination(pageData) {
    // 서버 응답이 없을 때도 화면이 깨지지 않도록 기본값 적용
    accountSecurityCurrentPage = pageData.page || 0;
    accountSecurityTotalPages = pageData.totalPages || 0;

    // 조회 결과가 없으면 0 / 0으로 표시
    const displayCurrentPage = accountSecurityTotalPages === 0 ? 0 : accountSecurityCurrentPage + 1;
    document.getElementById('page-info').innerText =
        displayCurrentPage + ' / ' + accountSecurityTotalPages + ' (총 ' + (pageData.totalElements || 0) + '건)';

    // 첫 페이지 또는 결과 없음이면 이전 버튼 비활성화
    document.getElementById('prev-page-button').disabled =
        accountSecurityCurrentPage <= 0 || accountSecurityTotalPages === 0;

    // 마지막 페이지 또는 결과 없음이면 다음 버튼 비활성화
    document.getElementById('next-page-button').disabled =
        accountSecurityTotalPages === 0 || accountSecurityCurrentPage >= accountSecurityTotalPages - 1;
}

/* 이전/다음 페이지 이동 처리 */
function moveAccountSecurityPage(delta) {
    const nextPage = accountSecurityCurrentPage + delta;

    // 조회 가능 범위를 벗어나면 요청하지 않음
    if (nextPage < 0 || nextPage >= accountSecurityTotalPages) {
        return;
    }

    loadAccountSecurityStatuses(nextPage);
}

/* 화면 상단 메시지 표시 처리 */
function showMessage(message, type) {
    const messageBox = document.getElementById('account-security-message');

    // 메시지가 없으면 기존 문구 제거
    messageBox.innerText = message || '';

    // 메시지 타입에 따른 상태 클래스 적용
    messageBox.className = 'message-area' + (type ? ' ' + type : '');
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

    // HTML 의미 문자를 엔티티로 변환해 스크립트 삽입 방지
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
