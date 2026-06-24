// 비밀번호 재설정 화면 주요 DOM 요소
const employeeNoInput = document.getElementById('employee-no');
const newPasswordInput = document.getElementById('new-password');
const confirmPasswordInput = document.getElementById('confirm-password');
const submitButton = document.getElementById('submit-btn');
const messageArea = document.getElementById('message-area');

/* 비밀번호 재설정 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', function () {
    // 비밀번호 찾기 화면에서 저장한 사번 자동 입력 처리
    const savedEmployeeNo = sessionStorage.getItem('passwordResetEmployeeNo');

    if (savedEmployeeNo) {
        employeeNoInput.value = savedEmployeeNo;
    }
});

/* 새 비밀번호 입력 시 폼 검증 처리 */
newPasswordInput.addEventListener('input', validateForm);

/* 새 비밀번호 확인 입력 시 폼 검증 처리 */
confirmPasswordInput.addEventListener('input', validateForm);

/* 비밀번호 재설정 제출 요청 처리 */
document.getElementById('reset-form').addEventListener('submit', async function (event) {
    // 로그인 전 재설정 API를 fetch로 호출하기 위해 기본 제출 방지
    event.preventDefault();

    hideMessage();

    try {
        // 임시 비밀번호와 새 비밀번호로 재설정 확인 요청
        const response = await fetch('/api/auth/password-reset/confirm', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                employeeNo: employeeNoInput.value.trim(),
                tempPassword: document.getElementById('temp-password').value,
                newPassword: newPasswordInput.value
            })
        });

        const data = await response.json();

        // 임시 비밀번호 만료/불일치 등 서버 검증 실패 표시
        if (!response.ok) {
            showMessage(data.message || '비밀번호 재설정에 실패했습니다.', 'error');
            return;
        }

        // 재설정 성공 후 기존 로그인 정보 정리
        clearTokens();
        sessionStorage.removeItem('passwordResetEmployeeNo');

        // 성공 메시지 표시 후 로그인 화면 이동
        showMessage('비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요.', 'success');
        setTimeout(function () {
            window.location.href = '/login';
        }, 1200);
    } catch (error) {
        // 통신 실패 메시지 표시
        showMessage('서버와 통신하지 못했습니다. 애플리케이션 실행 상태를 확인해주세요.', 'error');
    }
});

/* 비밀번호 정책과 확인값 검증 처리 */
function validateForm() {
    const value = newPasswordInput.value;

    // 비밀번호 정책별 통과 여부 계산
    const policies = {
        length: value.length >= 8,
        alpha: /[a-zA-Z]/.test(value),
        number: /[0-9]/.test(value),
        special: /[!@#$%^&*(),.?":{}|<>]/.test(value)
    };

    // 정책 항목별 성공/실패 UI 갱신
    updatePolicyUI('p-length', policies.length);
    updatePolicyUI('p-alpha', policies.alpha);
    updatePolicyUI('p-number', policies.number);
    updatePolicyUI('p-special', policies.special);

    // 새 비밀번호와 확인값 일치 여부 확인
    const isMatch = newPasswordInput.value === confirmPasswordInput.value && newPasswordInput.value !== '';

    // 모든 비밀번호 정책 충족 여부 확인
    const allPolicyOk = policies.length && policies.alpha && policies.number && policies.special;

    const matchMsg = document.getElementById('match-msg');

    // 확인값 상태에 따라 일치 메시지 표시
    if (confirmPasswordInput.value === '') {
        matchMsg.innerText = '';
    } else if (isMatch) {
        matchMsg.innerText = '비밀번호가 일치합니다.';
        matchMsg.style.color = '#36B37E';
    } else {
        matchMsg.innerText = '비밀번호가 일치하지 않습니다.';
        matchMsg.style.color = '#FF5630';
    }

    // 모든 정책을 만족하고 확인값이 일치할 때만 제출 버튼 활성화
    submitButton.disabled = !(allPolicyOk && isMatch);
}

/* 비밀번호 정책 항목 상태 갱신 처리 */
function updatePolicyUI(id, isOk) {
    // 정책 충족 여부에 따라 ok/fail 클래스 적용
    document.getElementById(id).className = 'policy-item ' + (isOk ? 'ok' : 'fail');
}

/* 성공/실패 메시지 표시 처리 */
function showMessage(message, type) {
    messageArea.innerText = message;

    // 메시지 타입에 따른 상태 클래스 적용
    messageArea.className = 'message-area ' + type;
}

/* 메시지 영역 초기화 처리 */
function hideMessage() {
    messageArea.innerText = '';

    messageArea.className = 'message-area';
}

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
