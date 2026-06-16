// 비활성 계정 응답을 중복 처리하지 않기 위한 상태값
let accountInactiveHandled = false;

// window.fetch를 재정의하기 전에 원본 fetch 메서드를 보관
const originalFetch = window.fetch.bind(window);
// accessToken 재발급 요청이 동시에 여러 번 실행되지 않도록 Promise를 공유
let refreshAccessTokenPromise = null;

/* 모든 화면의 fetch 응답을 감시해 토큰 만료와 비활성 계정 처리를 공통으로 수행하는 메서드 */
window.fetch = async function (resource, options = {}) {
    // 원래 fetch로 먼저 요청을 보냄
    const response = await originalFetch(resource, options);

    // accessToken 만료 응답이면 refreshToken으로 재발급 시도
    if (await isAccessTokenExpiredResponse(response, resource)) {
        const refreshed = await refreshAccessToken();

        // 재발급 성공 시 최신 accessToken을 붙여 원래 요청을 한 번 다시 보냄
        if (refreshed) {
            const retryOptions = withLatestAccessToken(options);
            const retryResponse = await originalFetch(resource, retryOptions);
            handleAccountInactiveResponse(retryResponse);
            return retryResponse;
        }

        // 재발급 실패 시 저장된 인증 정보를 정리하고 로그인 화면으로 이동
        clearStoredAuth();
        window.location.href = '/login';
        return response;
    }

    // 만료 응답이 아니어도 비활성 계정 응답인지 공통 확인
    handleAccountInactiveResponse(response);
    return response;
};

/* 레이아웃 최초 로드 시 아이콘, 사용자 정보, 권한별 메뉴, 게시판 아코디언을 초기화하는 메서드 */
document.addEventListener('DOMContentLoaded', async function () {
    // 레이아웃에 있는 lucide 아이콘을 실제 SVG로 렌더링
    lucide.createIcons();

    // 서버에서 현재 로그인 사용자 정보를 조회한 뒤 role 기준으로 메뉴 노출과 페이지 접근을 제어
    const layoutUser = await loadLayoutUser();
    const verifiedRole = layoutUser ? layoutUser.role : null;
    const currentRole = verifiedRole || localStorage.getItem('role');
    applyMenuAccess(currentRole);
    checkPageAccess(verifiedRole);

    // 게시판 하위 화면이면 사이드바 아코디언을 자동으로 열어둠
    const boardPages = ['boardAll', 'notice', 'general', 'projectNotice'];
    const activePage = document.body.dataset.activePage;

    if (boardPages.includes(activePage)) {
        openAccordion();
    }
});

/* 현재 페이지에 선언된 허용 role을 기준으로 직접 URL 접근을 차단하는 메서드 */
function checkPageAccess(verifiedRole) {
    // MainController가 내려준 페이지 접근 가능 role 목록을 조회
    const pageRoles = document.body.dataset.pageRoles;

    // pageRoles가 없으면 별도 권한 제한이 없는 일반 화면으로 처리
    if (!pageRoles) {
        return;
    }

    // 허용 role 문자열을 배열로 변환
    const allowedRoles = pageRoles
        .split(',')
        .map(function (role) {
            return role.trim();
        });

    // 서버에서 확인된 role이 없으면 인증되지 않은 접근으로 보고 로그인 화면으로 이동
    if (!verifiedRole) {
        alert('로그인이 필요한 화면입니다.');
        window.location.href = '/login';
        return;
    }

    // 현재 role이 허용 목록에 없으면 권한 없는 직접 URL 접근으로 보고 대시보드로 이동
    if (!allowedRoles.includes(verifiedRole)) {
        alert('접근 권한이 없습니다.');
        window.location.href = '/dashboard';
    }
}

/* 현재 로그인 사용자의 role 기준으로 접근 가능한 메뉴만 화면에 노출하는 메서드 */
function applyMenuAccess(currentRole) {
    // data-roles가 붙은 메뉴를 모두 조회
    const roleMenus = document.querySelectorAll('[data-roles]');

    roleMenus.forEach(function (menu) {
        // HTML에 선언된 허용 권한 문자열을 배열로 변환
        const allowedRoles = menu.dataset.roles
            .split(',')
            .map(function (role) {
                return role.trim();
            });

        // 현재 사용자 role이 허용 목록에 있으면 메뉴를 노출
        if (currentRole && allowedRoles.includes(currentRole)) {
            menu.classList.remove('role-menu-hidden');
            return;
        }

        // 허용되지 않은 메뉴는 숨김 상태 유지
        menu.classList.add('role-menu-hidden');
    });

    // 숨김 처리 후 새로 노출된 lucide 아이콘을 다시 렌더링
    lucide.createIcons();
}

/* 서버가 비활성 계정 응답을 내려준 경우 알림 후 인증 정보를 정리하는 메서드 */
async function handleAccountInactiveResponse(response) {
    // 이미 처리했거나 정상 응답이면 추가 처리하지 않음
    if (accountInactiveHandled || response.ok) {
        return;
    }

    try {
        // 원본 response body를 소비하지 않도록 복제본으로 에러 코드 확인
        const data = await response.clone().json();
        if (data.error !== 'ACCOUNT_INACTIVE') {
            return;
        }

        // 비활성 계정은 한 번만 처리하고 로그인 화면으로 이동
        accountInactiveHandled = true;
        alert(data.message || '계정 상태가 변경되어 로그아웃됩니다.');
        clearStoredAuth();
        window.location.href = '/login';
    } catch (error) {
        // JSON 응답이 아닌 오류는 각 화면의 기존 오류 처리에 맡김
    }
}

/* fetch 응답이 accessToken 만료 응답인지 확인하는 메서드 */
async function isAccessTokenExpiredResponse(response, resource) {
    // refresh 요청 자체는 자동 재발급 검사에서 제외해 무한 반복을 방지
    const requestUrl = typeof resource === 'string' ? resource : resource.url;
    if (requestUrl && requestUrl.includes('/api/auth/refresh')) {
        return false;
    }

    // 401이 아니면 토큰 만료 응답으로 보지 않음
    if (response.ok || response.status !== 401) {
        return false;
    }

    try {
        // 서버 에러 코드가 EXPIRED인 경우에만 만료 응답으로 판단
        const data = await response.clone().json();
        return data.error === 'EXPIRED';
    } catch (error) {
        return false;
    }
}

/* HttpOnly Cookie의 refreshToken으로 새 accessToken을 발급받는 메서드 */
async function refreshAccessToken() {
    // 동시에 여러 요청이 만료되어도 refresh 요청은 한 번만 보내도록 Promise를 재사용
    if (!refreshAccessTokenPromise) {
        refreshAccessTokenPromise = originalFetch('/api/auth/refresh', {
            method: 'POST',
            credentials: 'include'
        })
            .then(async function (response) {
                // refresh 실패 응답이면 재발급 실패로 처리
                if (!response.ok) {
                    return false;
                }

                // 새 accessToken이 없으면 재발급 실패로 처리
                const data = await response.json();
                if (!data.accessToken) {
                    return false;
                }

                // 새 accessToken을 localStorage에 저장
                localStorage.setItem('accessToken', data.accessToken);
                return true;
            })
            .catch(function () {
                // 네트워크 오류도 재발급 실패로 처리
                return false;
            })
            .finally(function () {
                // 다음 만료 상황에서 다시 refresh 요청을 만들 수 있도록 초기화
                refreshAccessTokenPromise = null;
            });
    }

    return refreshAccessTokenPromise;
}

/* 기존 fetch 옵션을 유지하면서 Authorization 헤더만 최신 accessToken으로 교체하는 메서드 */
function withLatestAccessToken(options) {
    // 저장된 최신 accessToken과 기존 헤더를 준비
    const accessToken = localStorage.getItem('accessToken');
    const headers = new Headers(options.headers || {});

    // accessToken이 있으면 Authorization 헤더를 최신 값으로 갱신
    if (accessToken) {
        headers.set('Authorization', 'Bearer ' + accessToken);
    }

    // 기존 요청 옵션을 유지하고 headers만 교체해 반환
    return {
        ...options,
        headers: headers
    };
}

/* 브라우저에 저장된 로그인 관련 정보를 모두 제거하는 메서드 */
function clearStoredAuth() {
    // localStorage에 남은 인증/사용자 정보를 제거
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('employeeNo');
    localStorage.removeItem('employeeName');
    localStorage.removeItem('role');

    // sessionStorage에 남은 화면 상태도 함께 정리
    sessionStorage.clear();
}

/* 게시판 사이드바 아코디언 메뉴의 열림과 닫힘을 전환하는 메서드 */
function toggleAccordion() {
    // 게시판 하위 메뉴와 화살표 아이콘 DOM 조회
    const subMenu = document.getElementById('boardSubMenu');
    const arrow = document.getElementById('accordionArrow');

    // 이미 열려 있으면 닫고 화살표 회전 상태도 제거
    if (subMenu.classList.contains('open')) {
        subMenu.classList.remove('open');
        arrow.classList.remove('open');
    } else {
        // 닫혀 있으면 공통 열기 메서드로 처리
        openAccordion();
    }
}

/* 게시판 사이드바 아코디언 메뉴를 열린 상태로 만드는 메서드 */
function openAccordion() {
    // 게시판 하위 메뉴와 화살표 아이콘 DOM 조회
    const subMenu = document.getElementById('boardSubMenu');
    const arrow = document.getElementById('accordionArrow');

    // 하위 메뉴를 보여주고 화살표 아이콘을 회전 상태로 변경
    subMenu.classList.add('open');
    arrow.classList.add('open');
}

/* 현재 로그인 사용자 정보를 조회해 상단 사용자 이름을 표시하는 메서드 */
async function loadLayoutUser() {
    // 로그인 시 저장한 accessToken 조회
    const accessToken = localStorage.getItem('accessToken');

    // 토큰이 없으면 방문자 상태 유지
    if (!accessToken) {
        return null;
    }

    // 현재 로그인 사용자 정보 조회
    const response = await fetch('/api/auth/me', {
        headers: {
            'Authorization': 'Bearer ' + accessToken
        }
    });

    // 토큰 오류 시 이름 변경하지 않음
    if (!response.ok) {
        return null;
    }

    // 응답 JSON에서 이름과 권한 정보를 꺼내 레이아웃에 반영
    const data = await response.json();
    const nameBox = document.getElementById('layout-user-name');

    if (nameBox) {
        nameBox.innerText = data.name;
    }

    // 다음 화면 이동에서도 사용할 수 있도록 최신 사용자 정보를 localStorage에 저장
    localStorage.setItem('employeeNo', data.employeeNo);
    localStorage.setItem('employeeName', data.name);
    localStorage.setItem('role', data.role);

    return data;
}

/* 서버 로그아웃 API 호출 후 브라우저 인증 정보를 정리하는 메서드 */
async function logoutFromLayout() {
    // 로그아웃 API 인증에 사용할 accessToken 조회
    const accessToken = localStorage.getItem('accessToken');

    try {
        // accessToken이 있으면 서버에 refreshToken 폐기 요청
        if (accessToken) {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + accessToken
                }
            });
        }
    } finally {
        // 서버 요청 성공 여부와 관계없이 브라우저 인증 정보 제거 후 로그인 화면으로 이동
        clearStoredAuth();
        window.location.href = '/login';
    }
}

// 채팅 팝업 열기
function openChatPopup(roomId, targetNo) {
    let url = '/api/chat/room';

    if (roomId) {
        url += `?roomId=${roomId}`;
        // targetNo가 있다면 쿼리 스트링에 추가
        if (targetNo) url += `&targetNo=${targetNo}`;
    }

    const windowName = 'OfficeMateChat';
    const windowFeatures = 'width=420,height=620,resizable=yes,scrollbars=yes';

    window.open(url, windowName, windowFeatures);
}

// ======================
// 전역 채팅 알림 STOMP 추가!!!! - 재경
// ======================
const layoutEmployeeNo = localStorage.getItem("employeeNo");
console.log("현재 로그인 사번:", layoutEmployeeNo);

const notificationClient = new StompJs.Client({
    brokerURL: `ws://${window.location.hostname}:8080/stomp/chat`,
    connectHeaders: {
        Authorization: "Bearer " + localStorage.getItem("accessToken")
    },
    reconnectDelay: 5000
});

// 연결 성공
notificationClient.onConnect = () => {
    console.log("전역 채팅 알림 연결 완료");

    // 개인 알림 구독
    notificationClient.subscribe(
        `/sub/chat/notification/${layoutEmployeeNo}`,
        (message) => {
            const data = JSON.parse(message.body);
            showChatToast(data);
        }
    );
};

// 연결 시작
notificationClient.activate();

function showChatToast(data) {

    // 토스트 생성
    const toast = document.createElement('div');

    toast.className = 'chat-toast';

    toast.innerHTML = `
        <div class="toast-title">
            ${data.senderName}
        </div>

        <div class="toast-content">
            ${data.content}
        </div>
    `;

    // 클릭 시 채팅창 열기
    toast.onclick = () => {
        openChatPopup(data.chatRoomId, data.senderNo);
        toast.remove();
    };

    document.getElementById('toastContainer').appendChild(toast);

    // 4초 뒤 제거
    setTimeout(() => {
        toast.remove();
    }, 4000);
}