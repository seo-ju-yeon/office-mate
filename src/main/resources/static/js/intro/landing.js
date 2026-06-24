// 스포트라이트 효과를 적용할 배경 요소
const spotlight = document.getElementById('spotlight');

/* 배경 스포트라이트 위치 갱신 처리 */
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

/* 랜딩 진입 링크 문구와 이동 경로 변경 처리 */
function setEntryLink(label, href) {
    // 버튼에 표시할 문구 변경
    entryText.innerText = label;

    // 버튼 클릭 시 이동할 경로 변경
    entryLink.href = href;
}

/* 브라우저 로그인 정보 제거 처리 */
function clearStoredAuth() {
    // 초기 진입 페이지에서는 이전 브라우저 세션 값을 모두 제거
    localStorage.clear();

    // 비밀번호 재설정 등 임시 세션 정보 제거
    sessionStorage.clear();
}

/* accessToken 만료 여부 사전 확인 처리 */
// 보안 검증이 아니라 만료가 확실한 토큰으로 /api/auth/me 호출을 피하기 위한 사전 체크
function isJwtExpired(token) {
    try {
        const payloadBase64Url = token.split('.')[1];

        // JWT payload를 atob가 읽을 수 있는 base64 형식으로 보정
        const payloadBase64 = payloadBase64Url
            .replace(/-/g, '+')
            .replace(/_/g, '/');

        const payload = JSON.parse(atob(payloadBase64));

        // exp는 초 단위이므로 밀리초 기준 비교를 위해 1000 곱셈
        return !payload.exp || payload.exp * 1000 <= Date.now();
    } catch (error) {
        // 토큰 형식이 이상하면 만료 토큰으로 처리
        return true;
    }
}

/* 서버 refreshToken 폐기 요청 처리 */
async function revokeServerSession(accessToken) {
    // 토큰이 없으면 서버 로그아웃 요청 생략
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
        // 서버 로그아웃 실패 여부와 관계없이 이후 브라우저 저장소 정리
    }
}

/* 랜딩 진입 버튼 목적지 결정 처리 */
async function initializeEntryLink() {
    // 서버 검증에 사용할 accessToken 조회
    const token = localStorage.getItem('accessToken');

    // 토큰이 없으면 로그인 화면 진입으로 설정
    if (!token) {
        clearStoredAuth();
        setEntryLink('GO TO LOGIN', '/login');
        return;
    }

    // 토큰이 이미 만료됐으면 /api/auth/me 호출 없이 정리
    // 랜딩 페이지에서는 refreshToken으로 자동 재발급하지 않음
    if (isJwtExpired(token)) {
        clearStoredAuth();
        setEntryLink('GO TO LOGIN', '/login');
        return;
    }

    try {
        // 현재 토큰이 실제 유효한지 /api/auth/me로 검증
        const response = await fetch('/api/auth/me', {
            headers: {
                'Authorization': 'Bearer ' + token
            }
        });

        // 토큰 검증 실패 시 인증 정보 제거 후 로그인 화면 설정
        if (!response.ok) {
            clearStoredAuth();
            setEntryLink('GO TO LOGIN', '/login');
            return;
        }

        // 현재 사용자 상태 조회
        const data = await response.json();

        // 임시 비밀번호 변경 필요 계정은 세션 정리 후 로그인 화면 설정
        if (data.tempPasswordRequired) {
            await revokeServerSession(token);
            clearStoredAuth();
            setEntryLink('GO TO LOGIN', '/login');
            return;
        }

        // 정상 로그인 상태면 대시보드 진입으로 설정
        setEntryLink('GO TO DASHBOARD', '/dashboard');
    } catch (error) {
        // 서버 통신 실패 시 인증 정보 제거 후 로그인 화면 설정
        clearStoredAuth();
        setEntryLink('GO TO LOGIN', '/login');
    }
}

// 랜딩 페이지 진입 시 버튼 목적지 초기화
initializeEntryLink();
