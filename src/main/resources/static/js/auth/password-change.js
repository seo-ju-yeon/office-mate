// 비밀번호 변경 화면 주요 입력 요소
const newPw = document.getElementById('newPw');
const confirmPw = document.getElementById('confirmPw');
const submitBtn = document.getElementById('submitBtn');

/* 새 비밀번호 입력 시 정책 검증 처리 */
newPw.addEventListener('input', function () {
    const val = newPw.value;

    // 비밀번호 정책별 통과 여부 계산
    const policies = {
        length: val.length >= 8,
        alpha: /[a-zA-Z]/.test(val),
        number: /[0-9]/.test(val),
        special: /[!@#$%^&*(),.?":{}|<>]/.test(val)
    };

    // 정책 항목별 성공/실패 UI 갱신
    updatePolicyUI('p-length', policies.length);
    updatePolicyUI('p-alpha', policies.alpha);
    updatePolicyUI('p-number', policies.number);
    updatePolicyUI('p-special', policies.special);

    // 제출 가능 여부 재검증
    validateForm();
});

/* 새 비밀번호 확인 입력 시 폼 검증 처리 */
confirmPw.addEventListener('input', validateForm);

/* 비밀번호 정책 항목 상태 갱신 처리 */
function updatePolicyUI(id, isOk) {
    const el = document.getElementById(id);

    // 정책 충족 여부에 따라 ok/fail 클래스 적용
    el.className = 'policy-item ' + (isOk ? 'ok' : 'fail');
}

/* 비밀번호 정책과 확인값 검증 처리 */
function validateForm() {
    const matchMsg = document.getElementById('match-msg');

    // 새 비밀번호와 확인값 일치 여부 확인
    const isMatch = newPw.value === confirmPw.value && newPw.value !== '';

    // 실패 상태 정책 항목 존재 여부 확인
    const allPolicyOk = document.querySelectorAll('.policy-item.fail').length === 0;

    // 확인값 상태에 따라 일치 메시지 표시
    if (confirmPw.value === '') {
        matchMsg.innerText = '';
    } else if (isMatch) {
        matchMsg.innerText = '비밀번호가 일치합니다.';
        matchMsg.style.color = '#36B37E';
    } else {
        matchMsg.innerText = '비밀번호가 일치하지 않습니다.';
        matchMsg.style.color = '#FF5630';
    }

    // 모든 정책을 만족하고 확인값이 일치할 때만 제출 버튼 활성화
    submitBtn.disabled = !(allPolicyOk && isMatch);
}

/* 비밀번호 변경 제출 요청 처리 */
document.getElementById('pwForm').addEventListener('submit', async function (event) {
    // 기본 form 제출 대신 fetch 요청 사용
    event.preventDefault();

    // 인증 토큰과 오류 메시지 영역 준비
    const accessToken = localStorage.getItem('accessToken');
    const errorArea = document.getElementById('error-msg');

    errorArea.style.display = 'none';

    // 토큰이 없으면 저장 정보 정리 후 로그인 화면 이동
    if (!accessToken) {
        clearTokens();
        window.location.href = '/login';
        return;
    }

    try {
        // 현재 비밀번호와 새 비밀번호 변경 요청
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

        const data = await response.json();

        // 서버 검증 실패 메시지 표시
        if (!response.ok) {
            errorArea.innerText = data.message || '비밀번호 변경에 실패했습니다.';
            errorArea.style.display = 'block';
            return;
        }

        // 변경 성공 후 토큰 정리와 재로그인 안내 처리
        clearTokens();
        alert('비밀번호가 성공적으로 변경되었습니다. 다시 로그인해주세요.');
        window.location.href = '/login';
    } catch (error) {
        // 통신 실패 메시지 표시
        errorArea.innerText = '서버와 통신하지 못했습니다. 애플리케이션 실행 상태를 확인해주세요.';
        errorArea.style.display = 'block';
    }
});

/* 브라우저 로그인 정보 제거 처리 */
function clearTokens() {
    // 인증 토큰 제거
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');

    // 로그인 사용자 식별 정보 제거
    localStorage.removeItem('employeeNo');
    localStorage.removeItem('employeeName');
    localStorage.removeItem('role');
}

/* 로그아웃 후 로그인 화면 이동 처리 */
async function logoutAndMoveToLogin(event) {
    // 기본 링크 이동 대신 서버 로그아웃 요청 사용
    event.preventDefault();

    const accessToken = localStorage.getItem('accessToken');

    try {
        // 토큰이 있으면 서버 로그아웃 요청 처리
        if (accessToken) {
            await fetch('/api/auth/logout', {
                method: 'POST',
                headers: {
                    'Authorization': 'Bearer ' + accessToken
                }
            });
        }
    } finally {
        // 서버 요청 결과와 관계없이 브라우저 정보 정리
        clearTokens();
        window.location.href = '/login';
    }
}
