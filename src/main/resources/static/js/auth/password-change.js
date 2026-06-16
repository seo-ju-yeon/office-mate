// 자주 접근하는 입력 요소를 변수에 담아 반복 DOM 조회 방지
const newPw = document.getElementById('newPw');
const confirmPw = document.getElementById('confirmPw');
const submitBtn = document.getElementById('submitBtn');

/* 새 비밀번호 입력 시 비밀번호 정책 충족 여부를 검사하는 이벤트 메서드 */
newPw.addEventListener('input', function () {
    // 새 비밀번호 입력값 조회
    const val = newPw.value;

    // 비밀번호 정책별 통과 여부 계산
    const policies = {
        length: val.length >= 8,
        alpha: /[a-zA-Z]/.test(val),
        number: /[0-9]/.test(val),
        special: /[!@#$%^&*(),.?":{}|<>]/.test(val)
    };

    // 각 정책 항목의 성공/실패 UI 갱신
    updatePolicyUI('p-length', policies.length);
    updatePolicyUI('p-alpha', policies.alpha);
    updatePolicyUI('p-number', policies.number);
    updatePolicyUI('p-special', policies.special);

    // 정책 변경 결과를 기준으로 폼 제출 가능 여부 재검사
    validateForm();
});

/* 새 비밀번호 확인 입력 시 일치 여부를 다시 검사하는 이벤트 메서드 */
confirmPw.addEventListener('input', validateForm);

/* 비밀번호 정책 항목의 성공/실패 클래스를 갱신하는 메서드 */
function updatePolicyUI(id, isOk) {
    // 대상 정책 항목 DOM 조회
    const el = document.getElementById(id);

    // 정책을 만족하면 ok, 만족하지 못하면 fail 클래스 적용
    el.className = 'policy-item ' + (isOk ? 'ok' : 'fail');
}

/* 새 비밀번호 정책과 확인값 일치 여부를 검사해 제출 버튼 상태를 갱신하는 메서드 */
function validateForm() {
    // 비밀번호 확인 메시지 DOM 조회
    const matchMsg = document.getElementById('match-msg');

    // 새 비밀번호와 확인값이 모두 입력되고 서로 일치하는지 확인
    const isMatch = newPw.value === confirmPw.value && newPw.value !== '';

    // 실패 상태인 정책 항목이 하나도 없는지 확인
    const allPolicyOk = document.querySelectorAll('.policy-item.fail').length === 0;

    // 확인값이 비어 있으면 일치 메시지 제거
    if (confirmPw.value === '') {
        matchMsg.innerText = '';
    } else if (isMatch) {
        // 비밀번호가 일치하면 성공 메시지와 초록색 표시
        matchMsg.innerText = '비밀번호가 일치합니다.';
        matchMsg.style.color = '#36B37E';
    } else {
        // 비밀번호가 다르면 실패 메시지와 빨간색 표시
        matchMsg.innerText = '비밀번호가 일치하지 않습니다.';
        matchMsg.style.color = '#FF5630';
    }

    // 모든 정책을 만족하고 확인값이 일치할 때만 제출 버튼 활성화
    submitBtn.disabled = !(allPolicyOk && isMatch);
}

/* 비밀번호 변경 폼 제출 시 서버에 변경 요청을 보내는 메서드 */
document.getElementById('pwForm').addEventListener('submit', async function (event) {
    // form 기본 제출을 막고 fetch 기반 비밀번호 변경 흐름 사용
    event.preventDefault();

    // 인증 토큰과 오류 메시지 영역 조회
    const accessToken = localStorage.getItem('accessToken');
    const errorArea = document.getElementById('error-msg');

    // 이전 오류 메시지 숨김 처리
    errorArea.style.display = 'none';

    // 토큰이 없으면 브라우저 토큰을 정리하고 로그인 화면으로 이동
    if (!accessToken) {
        clearTokens();
        window.location.href = '/login';
        return;
    }

    try {
        // 현재 비밀번호와 새 비밀번호를 서버로 보내 employee.password 변경 요청
        const response = await fetch('/api/auth/change-password', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + accessToken
            },
            body: JSON.stringify({
                currentPassword: document.getElementById('currPw').value,
                newPassword: document.getElementById('newPw').value
            })
        });

        // 서버 응답 본문을 JSON으로 변환
        const data = await response.json();

        // 현재 비밀번호 불일치 등 서버 검증 실패를 화면에 표시
        if (!response.ok) {
            errorArea.innerText = data.message || '비밀번호 변경에 실패했습니다.';
            errorArea.style.display = 'block';
            return;
        }

        // 비밀번호 변경 성공 시 기존 토큰 정리 후 다시 로그인하도록 이동
        clearTokens();
        alert('비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.');
        window.location.href = '/login';
    } catch (error) {
        // 네트워크 오류나 서버 미기동 상태를 사용자에게 안내
        errorArea.innerText = '서버와 통신하지 못했습니다. 애플리케이션 실행 상태를 확인해주세요.';
        errorArea.style.display = 'block';
    }
});

/* 브라우저에 저장된 로그인 토큰과 사용자 정보를 모두 제거하는 메서드 */
function clearTokens() {
    // 기존 access token 제거
    localStorage.removeItem('accessToken');

    // 기존 refresh token 제거
    localStorage.removeItem('refreshToken');

    // 로그인 사용자 식별 정보 제거
    localStorage.removeItem('employeeNo');
    localStorage.removeItem('employeeName');
    localStorage.removeItem('role');
}

/* 로그아웃 API를 호출한 뒤 로그인 화면으로 이동하는 메서드 */
async function logoutAndMoveToLogin(event) {
    // a 태그의 기본 이동을 막고 서버 로그아웃 API를 먼저 호출
    event.preventDefault();

    // 로그아웃 API 인증에 사용할 accessToken 조회
    const accessToken = localStorage.getItem('accessToken');

    try {
        // 토큰이 있으면 서버 로그아웃 API를 호출해 활성 refresh token 폐기 요청
        if (accessToken) {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + accessToken
                }
            });
        }
    } finally {
        // 서버 요청 성공/실패와 관계없이 브라우저 토큰 제거 후 로그인 화면 이동
        clearTokens();
        window.location.href = '/login';
    }
}
