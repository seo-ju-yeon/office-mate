// 스포트라이트 효과를 적용할 배경 요소
const spotlight = document.getElementById('spotlight');

/* 마우스 좌표를 CSS 변수로 전달해 배경 스포트라이트 위치를 움직이는 이벤트 메서드 */
window.addEventListener('mousemove', function (event) {
    // 현재 마우스 X 좌표를 CSS 변수로 반영
    spotlight.style.setProperty('--x', event.clientX + 'px');

    // 현재 마우스 Y 좌표를 CSS 변수로 반영
    spotlight.style.setProperty('--y', event.clientY + 'px');
});

// 로그인 상태에 따라 목적지가 바뀌는 진입 링크 요소
const entryLink = document.getElementById('entry-link');

// 진입 링크 내부에 표시되는 텍스트 요소
const entryText = document.querySelector('#entry-link span');

/* 랜딩 페이지의 진입 버튼 문구와 이동 경로를 함께 변경하는 메서드 */
function setEntryLink(label, href) {
    // 버튼에 표시할 문구 변경
    entryText.innerText = label;

    // 버튼 클릭 시 이동할 경로 변경
    entryLink.href = href;
}

/* 브라우저에 남아 있는 로그인 토큰과 사용자 정보를 제거하는 메서드 */
function clearStoredAuth() {
    // 초기 진입 페이지에서는 이전 브라우저 세션 값을 모두 제거
    localStorage.clear();

    // 비밀번호 재설정 등 임시 세션 정보 제거
    sessionStorage.clear();
}

/* accessToken이 이미 만료됐는지 확인하는 메서드 */
// 보안 검증용이 아니라, 만료가 확실한 토큰으로 /api/auth/me를 호출하지 않기 위해 사전 체크
function isJwtExpired(token) {
    try {
        const payloadBase64Url = token.split('.')[1];

        // JWT payload는 base64url 형식이라 atob가 읽을 수 있는 base64 형식으로 보정
        const payloadBase64 = payloadBase64Url
            .replace(/-/g, '+')
            .replace(/_/g, '/');

        const payload = JSON.parse(atob(payloadBase64));

        // exp는 초 단위, Date.now()는 밀리초 단위라 1000을 곱함
        return !payload.exp || payload.exp * 1000 <= Date.now();
    } catch (error) {
        // 토큰 형식이 이상하면 만료된 토큰처럼 취급하고 정리
        return true;
    }
}

/* 서버에 남은 refresh token도 폐기해 브라우저 세션 재발급을 막는 메서드 */
async function revokeServerSession(accessToken) {
    // 토큰이 없으면 서버 로그아웃 요청 없이 브라우저 저장소만 정리
    if (!accessToken) {
        return;
    }

    try {
        await fetch('/api/auth/logout', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });
    } catch (error) {
        // 서버 로그아웃 실패 여부와 관계없이 브라우저 저장소는 정리한다.
    }
}

/* 현재 로그인 상태를 서버에 검증해 랜딩 진입 버튼의 목적지를 결정하는 메서드 */
async function initializeEntryLink() {
    // 단순 localStorage 존재 여부가 아니라 서버 검증에 사용할 accessToken 조회
    const token = localStorage.getItem('accessToken');

    // 토큰이 없으면 로그인 화면으로 진입하도록 설정
    if (!token) {
        clearStoredAuth();
        setEntryLink('GO TO LOGIN', '/login');
        return;
    }

    // 토큰이 이미 만료됐으면 /api/auth/me를 호출하지 않고 바로 정리
    // 랜딩 페이지에서는 refreshToken으로 자동 재발급하지 않음
    if (isJwtExpired(token)) {
        clearStoredAuth();
        setEntryLink('GO TO LOGIN', '/login');
        return;
    }

    try {
        // 현재 토큰이 실제 유효한지 서버의 /api/auth/me로 검증
        const response = await fetch('/api/auth/me', {
            headers: {
                'Authorization': 'Bearer ' + token
            }
        });

        // 토큰 검증 실패 시 저장된 인증 정보를 지우고 로그인 화면으로 설정
        if (!response.ok) {
            clearStoredAuth();
            setEntryLink('GO TO LOGIN', '/login');
            return;
        }

        // 현재 사용자 상태 조회
        const data = await response.json();

        // 임시 비밀번호 변경이 필요한 계정이면 세션을 정리하고 로그인 화면으로 설정
        if (data.tempPasswordRequired) {
            await revokeServerSession(token);
            clearStoredAuth();
            setEntryLink('GO TO LOGIN', '/login');
            return;
        }

        // 정상 로그인 상태면 대시보드로 진입하도록 설정
        setEntryLink('GO TO DASHBOARD', '/dashboard');
    } catch (error) {
        // 서버 통신 실패(fetch('/api/auth/me') HTTP 요청 실패) 시 저장된 인증 정보를 지우고 로그인 화면으로 설정
        clearStoredAuth();
        setEntryLink('GO TO LOGIN', '/login');
    }
}

// 랜딩 페이지 진입 시 버튼 목적지 초기화
initializeEntryLink();
