// 현재 조회 중인 페이지 번호
let accountSecurityCurrentPage = 0;

// 서버가 내려준 전체 페이지 수
let accountSecurityTotalPages = 0;

// 계정 보안 관리 목록의 페이지당 조회 개수
const ACCOUNT_SECURITY_PAGE_SIZE = 10;

/* 계정 보안 관리 화면이 처음 로드될 때 접근 권한 확인과 검색 입력 이벤트 등록을 담당하는 초기화 메서드 */
document.addEventListener('DOMContentLoaded', function () {
    // 현재 로그인 사용자가 계정 보안 관리 화면에 접근 가능한지 먼저 확인
    checkAccountSecurityPageAccess();

    // 검색어 입력창 DOM을 찾아 Enter 키 조회 이벤트를 연결
    const keywordInput = document.getElementById('keyword-input');
    keywordInput.addEventListener('keydown', function (event) {
        // Enter 키 입력 시 조회 버튼 클릭과 동일하게 계정 보안 상태 목록 조회
        if (event.key === 'Enter') {
            loadAccountSecurityStatuses(0);
        }
    });
});

/* 현재 로그인 사용자가 계정 보안 관리 화면에 접근 가능한 ADMIN 또는 SUPER 권한인지 확인하는 메서드 */
async function checkAccountSecurityPageAccess() {
    // API 인증 헤더에 사용할 accessToken을 localStorage에서 조회
    const accessToken = localStorage.getItem('accessToken');

    // 토큰이 없으면 로그인하지 않은 상태이므로 로그인 화면으로 이동
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 현재 로그인 사용자 정보를 조회해서 role 값을 확인
        const response = await fetch('/api/auth/me', {
            headers: {'Authorization': 'Bearer ' + accessToken}
        });

        // 사용자 정보 조회 실패 시 토큰 만료 또는 비정상 접근으로 보고 로그인 화면으로 이동
        if (!response.ok) {
            window.location.href = '/login';
            return;
        }

        // 응답 본문을 JSON으로 변환해 현재 사용자 정보를 사용
        const data = await response.json();

        // ADMIN/SUPER가 아니면 계정 보안 관리 권한이 없으므로 안내 후 대시보드로 이동
        if (data.role !== 'ADMIN' && data.role !== 'SUPER') {
            showMessage('계정 보안 관리는 관리자만 접근할 수 있습니다.', 'error');
            setTimeout(function () {
                window.location.href = '/dashboard';
            }, 800);
            return;
        }

        // 권한 확인 완료 후 초기 계정 보안 상태 목록 조회
        loadAccountSecurityStatuses(0);
    } catch (error) {
        // 네트워크 오류나 예외 발생 시 화면 상단에 실패 메시지 표시
        showMessage('계정 보안 관리 화면을 불러오지 못했습니다.', 'error');
    }
}

/* 검색어와 잠긴 계정 필터 조건을 기준으로 계정 보안 상태 목록을 조회하는 메서드 */
async function loadAccountSecurityStatuses(page) {
    // API 호출에 필요한 accessToken과 화면 검색 조건을 읽어옴
    const accessToken = localStorage.getItem('accessToken');
    const keyword = document.getElementById('keyword-input').value.trim();
    const lockedOnly = document.getElementById('locked-only-input').checked;
    const params = new URLSearchParams();
    const nextPage = Math.max(page || 0, 0);

    // 서버 페이징 조회에 사용할 page/size 쿼리 파라미터 추가
    params.set('page', nextPage);
    params.set('size', ACCOUNT_SECURITY_PAGE_SIZE);

    // 검색어가 입력되어 있으면 keyword 쿼리 파라미터 추가
    if (keyword) {
        params.set('keyword', keyword);
    }

    // 잠긴 계정만 체크되어 있으면 lockedOnly=true 쿼리 파라미터 추가
    if (lockedOnly) {
        params.set('lockedOnly', 'true');
    }

    // 쿼리 파라미터가 있을 때만 URL 뒤에 붙여 최종 조회 API 주소 생성
    const queryString = params.toString();
    const url = '/api/management/account-security' + (queryString ? '?' + queryString : '');

    try {
        // 계정 보안 상태 목록 조회 API 호출
        const response = await fetch(url, {
            headers: {'Authorization': 'Bearer ' + accessToken}
        });

        // 오류 응답이면 서버 메시지를 우선 사용하고 없으면 기본 메시지 사용
        if (!response.ok) {
            const error = await response.json().catch(function () {
                return {};
            });
            throw new Error(error.message || '계정 보안 상태를 조회하지 못했습니다.');
        }

        // 조회 결과를 페이지 응답 객체로 변환
        const pageData = await response.json();
        const items = pageData.content || [];

        // 조회 결과를 테이블에 렌더링
        renderAccountSecurityTable(items);

        // 조회 결과와 페이지 응답 기준으로 요약 카운트 갱신
        updateSummary(items, pageData);

        // 현재 페이지와 전체 페이지 정보를 갱신
        renderPagination(pageData);

        // 조회 성공 메시지 표시
        showMessage('계정 보안 상태를 조회했습니다.', 'success');
    } catch (error) {
        // 조회 실패 시 테이블과 요약 정보를 빈 상태로 초기화
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

        // 실패 원인을 화면 상단 메시지 영역에 표시
        showMessage(error.message, 'error');
    }
}

/* 서버에서 조회한 계정 보안 상태 목록을 테이블 행 HTML로 렌더링하는 메서드 */
function renderAccountSecurityTable(items) {
    // 계정 보안 목록을 삽입할 tbody 요소 조회
    const tableBody = document.getElementById('account-security-table-body');

    // 조회 결과가 없으면 빈 결과 안내 행만 표시
    if (!items || items.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="9" class="empty-row">조회 결과가 없습니다.</td></tr>';
        return;
    }

    // 조회된 계정 목록을 순회하며 테이블 행 문자열 생성
    tableBody.innerHTML = items.map(function (item) {
        // 계정 잠금 여부에 따라 상태 배지 CSS 클래스 결정
        const badgeClass = item.accountLocked ? 'locked' : 'normal';

        // 계정 잠금 여부에 따라 사용자에게 보여줄 상태 문구 결정
        const badgeText = item.accountLocked ? '잠김' : '정상';

        // 잠긴 계정이면 해제 버튼, 정상 계정이면 비활성 정상 버튼 생성
        const unlockButton = item.accountLocked
            ? `<button type="button" class="btn-danger" onclick="unlockAccount('${item.employeeNo}')">
                   <i data-lucide="lock-open"></i>
                   해제
               </button>`
            : `<button type="button" class="btn-secondary" disabled>
                   <i data-lucide="check"></i>
                   정상
               </button>`;

        // 서버에서 받은 문자열 값은 escapeHtml을 거쳐 화면에 안전하게 출력
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

    // 동적으로 삽입된 lucide 아이콘을 실제 SVG 아이콘으로 변환
    lucide.createIcons();
}

/* 선택한 직원 계정의 잠금 상태를 해제하도록 서버에 요청하는 메서드 */
async function unlockAccount(employeeNo) {
    // 사용자가 실수로 잠금 해제하지 않도록 확인창 표시
    if (!confirm(employeeNo + ' 계정의 잠금을 해제하시겠습니까?')) {
        return;
    }

    // 잠금 해제 API 인증에 사용할 accessToken 조회
    const accessToken = localStorage.getItem('accessToken');

    try {
        // 사번을 URL 인코딩한 뒤 계정 잠금 해제 API 호출
        const response = await fetch('/api/management/account-security/' + encodeURIComponent(employeeNo) + '/unlock', {
            method: 'PATCH',
            headers: {'Authorization': 'Bearer ' + accessToken}
        });

        // 잠금 해제 실패 시 서버 메시지를 우선 사용하고 없으면 기본 메시지 사용
        if (!response.ok) {
            const error = await response.json().catch(function () {
                return {};
            });
            throw new Error(error.message || '계정 잠금 해제에 실패했습니다.');
        }

        // 잠금 해제 성공 메시지 표시
        showMessage(employeeNo + ' 계정 잠금을 해제했습니다.', 'success');

        // 목록을 다시 조회해 해제된 상태를 즉시 반영
        loadAccountSecurityStatuses(accountSecurityCurrentPage);
    } catch (error) {
        // API 호출 실패 또는 예외 발생 시 오류 메시지 표시
        showMessage(error.message, 'error');
    }
}

/* 현재 조회 조건의 전체 건수와 현재 페이지의 잠김/정상 계정 수 요약을 갱신하는 메서드 */
function updateSummary(items, pageData) {
    // 서버가 내려준 현재 검색 조건의 전체 조회 계정 수 사용
    const totalCount = pageData.totalElements || 0;

    // 현재 페이지 목록에서 accountLocked 값이 true인 계정만 골라 잠긴 계정 수 계산
    const lockedCount = items.filter(function (item) {
        return item.accountLocked;
    }).length;

    // 전체 계정 수를 요약 영역에 반영
    document.getElementById('total-count').innerText = totalCount;

    // 잠긴 계정 수를 요약 영역에 반영
    document.getElementById('locked-count').innerText = lockedCount;

    // 정상 계정 수는 현재 페이지 목록 수에서 잠긴 계정 수를 뺀 값으로 반영
    document.getElementById('normal-count').innerText = items.length - lockedCount;
}

/* 서버 페이지 응답을 기준으로 현재 페이지 정보와 이전/다음 버튼 상태를 갱신하는 메서드 */
function renderPagination(pageData) {
    // 서버 응답이 없을 때도 화면이 깨지지 않도록 기본값 적용
    accountSecurityCurrentPage = pageData.page || 0;
    accountSecurityTotalPages = pageData.totalPages || 0;

    // 전체 페이지가 0이면 사용자에게는 0 / 0으로 표시
    const displayCurrentPage = accountSecurityTotalPages === 0 ? 0 : accountSecurityCurrentPage + 1;
    document.getElementById('page-info').innerText =
        displayCurrentPage + ' / ' + accountSecurityTotalPages + ' (총 ' + (pageData.totalElements || 0) + '건)';

    // 첫 페이지이거나 조회 결과가 없으면 이전 버튼 비활성화
    document.getElementById('prev-page-button').disabled =
        accountSecurityCurrentPage <= 0 || accountSecurityTotalPages === 0;

    // 마지막 페이지이거나 조회 결과가 없으면 다음 버튼 비활성화
    document.getElementById('next-page-button').disabled =
        accountSecurityTotalPages === 0 || accountSecurityCurrentPage >= accountSecurityTotalPages - 1;
}

/* 이전/다음 버튼 클릭 시 이동할 페이지를 계산해 계정 보안 상태 목록을 다시 조회하는 메서드 */
function moveAccountSecurityPage(delta) {
    // 현재 페이지에 이동 방향을 더해 다음 페이지 번호 계산
    const nextPage = accountSecurityCurrentPage + delta;

    // 조회 가능한 페이지 범위를 벗어나면 요청하지 않음
    if (nextPage < 0 || nextPage >= accountSecurityTotalPages) {
        return;
    }

    // 계산된 페이지 번호로 목록 다시 조회
    loadAccountSecurityStatuses(nextPage);
}

/* 화면 상단 메시지 영역에 안내 문구와 성공/오류 상태 클래스를 적용하는 메서드 */
function showMessage(message, type) {
    // 메시지를 표시할 DOM 요소 조회
    const messageBox = document.getElementById('account-security-message');

    // message 값이 없으면 빈 문자열로 처리해 기존 문구 제거
    messageBox.innerText = message || '';

    // 메시지 타입에 따라 success 또는 error 클래스를 붙여 색상 변경
    messageBox.className = 'message-area' + (type ? ' ' + type : '');
}

/* 서버에서 받은 날짜/시간 값을 한국어 로케일의 날짜 문자열로 변환하는 메서드 */
function formatDateTime(value) {
    // 날짜 값이 없으면 화면에서 '-'로 표시
    if (!value) {
        return '-';
    }

    // 서버에서 받은 값을 Date 객체로 변환
    const date = new Date(value);

    // 브라우저가 해석하지 못하는 날짜 값이면 원본 값 그대로 반환
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    // 유효한 날짜 값이면 한국어 날짜/시간 형식으로 변환
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/* 서버에서 받은 문자열을 HTML에 안전하게 삽입하기 위해 특수 문자를 이스케이프하는 메서드 */
function escapeHtml(value) {
    // null 또는 undefined는 화면에 빈 문자열로 표시
    if (value === null || value === undefined) {
        return '';
    }

    // HTML에서 의미가 있는 특수 문자를 엔티티로 변환해 스크립트 삽입 방지
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
