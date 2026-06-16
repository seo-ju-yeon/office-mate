// 비밀번호 재설정 화면에서 반복해서 사용할 DOM 요소를 미리 조회
const employeeNoInput = document.getElementById('employee-no');
const newPasswordInput = document.getElementById('new-password');
const confirmPasswordInput = document.getElementById('confirm-password');
const submitButton = document.getElementById('submit-btn');
const messageArea = document.getElementById('message-area');

/* 비밀번호 찾기 화면에서 저장한 사번을 재설정 화면에 자동 입력하는 초기화 메서드 */
document.addEventListener('DOMContentLoaded', function () {
    // 비밀번호 찾기 화면에서 sessionStorage에 저장한 사번 조회
    const savedEmployeeNo = sessionStorage.getItem('passwordResetEmployeeNo');

    // 저장된 사번이 있으면 입력칸에 자동으로 채움
    if (savedEmployeeNo) {
        employeeNoInput.value = savedEmployeeNo;
    }
});

/* 새 비밀번호 입력 시 정책과 일치 여부를 검사하는 이벤트 메서드 */
newPasswordInput.addEventListener('input', validateForm);

/* 새 비밀번호 확인 입력 시 정책과 일치 여부를 검사하는 이벤트 메서드 */
confirmPasswordInput.addEventListener('input', validateForm);

/* 비밀번호 재설정 폼 제출 시 서버에 재설정 확인 요청을 보내는 메서드 */
document.getElementById('reset-form').addEventListener('submit', async function (event) {
    // 로그인 전 비밀번호 재설정은 JWT 없이 호출하는 API이므로 기본 form 제출 방지
    event.preventDefault();

    // 이전 성공/실패 메시지 제거
    hideMessage();

    try {
        // 서버는 employeeNo로 Redis key를 찾고 tempPassword를 BCrypt matches로 검증
        const response = await fetch('/api/auth/password-reset/confirm', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                employeeNo: employeeNoInput.value.trim(),
                tempPassword: document.getElementById('temp-password').value,
                newPassword: newPasswordInput.value
            })
        });

        // 서버 응답을 JSON으로 변환
        const data = await response.json();

        // 임시 비밀번호 만료/불일치/새 비밀번호 조건 실패 등의 오류 표시
        if (!response.ok) {
            showMessage(data.message || '비밀번호 재설정에 실패했습니다.', 'error');
            return;
        }

        // 재설정 성공 후 기존 로그인 토큰이 남아 있을 수 있으므로 정리
        clearTokens();
        sessionStorage.removeItem('passwordResetEmployeeNo');

        // 성공 메시지 표시 후 로그인 화면으로 이동
        showMessage('비밀번호가 재설정되었습니다. 새 비밀번호로 로그인해주세요.', 'success');
        setTimeout(function () {
            window.location.href = '/login';
        }, 1200);
    } catch (error) {
        // 네트워크 오류나 서버 미기동 상태를 사용자에게 안내
        showMessage('서버와 통신하지 못했습니다. 애플리케이션 실행 상태를 확인해주세요.', 'error');
    }
});

/* 새 비밀번호 정책과 확인값 일치 여부를 검사해 제출 버튼 상태를 갱신하는 메서드 */
function validateForm() {
    // 새 비밀번호 입력값 조회
    const value = newPasswordInput.value;

    // 비밀번호 정책별 통과 여부 계산
    const policies = {
        length: value.length >= 8,
        alpha: /[a-zA-Z]/.test(value),
        number: /[0-9]/.test(value),
        special: /[!@#$%^&*(),.?":{}|<>]/.test(value)
    };

    // 각 정책 항목의 성공/실패 UI 갱신
    updatePolicyUI('p-length', policies.length);
    updatePolicyUI('p-alpha', policies.alpha);
    updatePolicyUI('p-number', policies.number);
    updatePolicyUI('p-special', policies.special);

    // 새 비밀번호와 확인값이 모두 입력되고 서로 일치하는지 확인
    const isMatch = newPasswordInput.value === confirmPasswordInput.value && newPasswordInput.value !== '';

    // 모든 비밀번호 정책을 만족하는지 확인
    const allPolicyOk = policies.length && policies.alpha && policies.number && policies.special;

    // 비밀번호 확인 메시지 DOM 조회
    const matchMsg = document.getElementById('match-msg');

    // 확인값이 비어 있으면 일치 메시지 제거
    if (confirmPasswordInput.value === '') {
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
    submitButton.disabled = !(allPolicyOk && isMatch);
}

/* 비밀번호 정책 항목의 성공/실패 클래스를 갱신하는 메서드 */
function updatePolicyUI(id, isOk) {
    // 대상 정책 항목에 ok 또는 fail 클래스 적용
    document.getElementById(id).className = 'policy-item ' + (isOk ? 'ok' : 'fail');
}

/* 성공/실패 메시지를 공통 메시지 영역에 표시하는 메서드 */
function showMessage(message, type) {
    // 메시지 내용을 화면에 반영
    messageArea.innerText = message;

    // 메시지 타입에 따라 success 또는 error 클래스 적용
    messageArea.className = 'message-area ' + type;
}

/* 이전 메시지를 지우고 메시지 영역을 기본 상태로 되돌리는 메서드 */
function hideMessage() {
    // 메시지 텍스트 제거
    messageArea.innerText = '';

    // 메시지 영역 클래스를 기본 상태로 복원
    messageArea.className = 'message-area';
}

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
