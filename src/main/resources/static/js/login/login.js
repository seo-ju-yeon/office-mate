// 로그인 화면 Lucide 아이콘 렌더링
lucide.createIcons();

// 사번 입력 요소
const empInput = document.getElementById('employeeId');

// 사번 입력 시 대문자 표시 처리
empInput.addEventListener('input', function () {
    this.value = this.value.toUpperCase();
});

/* 비밀번호 표시/숨김 전환 처리 */
function togglePassword() {
    // 비밀번호 입력칸과 토글 아이콘 조회
    const pwdInput = document.getElementById('password');
    const toggleIcon = document.getElementById('toggleIcon');

    // 입력 타입과 아이콘을 현재 상태에 맞게 전환
    if (pwdInput.type === 'password') {
        pwdInput.type = 'text';
        toggleIcon.setAttribute('data-lucide', 'eye-off');
    } else {
        pwdInput.type = 'password';
        toggleIcon.setAttribute('data-lucide', 'eye');
    }

    // data-lucide 변경 후 아이콘 재렌더링
    lucide.createIcons();
}

/* 로그인 제출 요청 처리 */
document.getElementById('loginForm').addEventListener('submit', async function (e) {
    // 기본 form 제출 대신 fetch 요청 사용
    e.preventDefault();

    // 이전 오류/성공/복직 신청 상태 초기화
    const errorArea = document.getElementById('error-msg');
    errorArea.style.display = 'none';
    hideSuccess();
    hideReturnRequestPanel();

    // 로그인 처리 중 로딩 표시
    showLoading();

    // 사번은 대문자로 정규화하고 비밀번호는 원문 사용
    const employeeNo = document.getElementById('employeeId').value.toUpperCase().trim();
    const password = document.getElementById('password').value;

    try {
        // 로그인 API 요청
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({employeeNo, password})
        });

        // 서버 응답 변환 후 로딩 종료
        const data = await response.json();
        hideLoading();

        // 로그인 실패 응답이면 오류 유형별 메시지 표시
        if (!response.ok) {
            // 휴직 계정은 복직 신청 패널 노출
            if (data.error === 'ACCOUNT_ON_LEAVE') {
                document.getElementById('error-text').innerText = data.message;
                errorArea.style.display = 'flex';
                showReturnRequestPanel();
                return;
            }

            // 일반 로그인 실패 또는 계정 잠금 메시지 표시
            document.getElementById('error-text').innerText = buildLoginFailureMessage(data);
            errorArea.style.display = 'flex';
            return;
        }

        // 이후 API 호출과 화면 표시에서 사용할 로그인 정보 저장
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('employeeNo', data.employeeNo);
        localStorage.setItem('employeeName', data.name);
        localStorage.setItem('role', data.role);

        // 임시 비밀번호 사용자는 비밀번호 변경 화면 이동
        if (data.tempPasswordRequired) {
            window.location.href = '/password-change';
        } else {
            // 정상 로그인 사용자는 대시보드 이동
            window.location.href = '/dashboard';
        }
    } catch (error) {
        // 네트워크 오류 또는 서버 미응답 메시지 표시
        hideLoading();
        document.getElementById('error-text').innerText = '서버와 연결할 수 없습니다.';
        errorArea.style.display = 'flex';
    }
});

/* 로그인 실패 메시지 변환 처리 */
function buildLoginFailureMessage(data) {
    // 계정 잠금 상태는 잠금 안내 우선 표시
    if (data.error === 'ACCOUNT_LOCKED') {
        return data.message || '로그인 실패 5회 이상으로 계정이 잠겼습니다. 관리자에게 문의해주세요.';
    }

    // 서버 기본 메시지와 실패 횟수 정보 준비
    const baseMessage = data.message || '사번 또는 비밀번호가 올바르지 않습니다.';
    const failCount = Number(data.loginFailCount);
    const lockThreshold = Number(data.lockThreshold || 5);

    // 실패 횟수가 없으면 기본 메시지만 반환
    if (!Number.isFinite(failCount) || failCount <= 0) {
        return baseMessage;
    }

    // 실패 횟수와 잠금 기준 함께 안내
    return baseMessage + ' 현재 로그인 실패 ' + failCount + '회입니다. '
        + lockThreshold + '회 이상 실패하면 계정이 잠깁니다.';
}

/* 휴직 계정 복직 신청 요청 처리 */
async function submitReturnFromLeaveRequest() {
    // 복직 신청에 필요한 입력값 준비
    const employeeNo = document.getElementById('employeeId').value.toUpperCase().trim();
    const password = document.getElementById('password').value;
    const reason = document.getElementById('return-reason').value;
    const errorArea = document.getElementById('error-msg');
    const button = document.getElementById('return-request-btn');

    // 이전 오류/성공 메시지 초기화
    errorArea.style.display = 'none';
    hideSuccess();

    // 사번과 비밀번호가 없으면 서버 호출 전 안내
    if (!employeeNo || !password) {
        document.getElementById('error-text').innerText = '사번과 비밀번호를 입력한 뒤 복직 신청을 진행해주세요.';
        errorArea.style.display = 'flex';
        return;
    }

    // 중복 제출 방지와 로딩 표시
    button.disabled = true;
    showLoading();

    try {
        // 인증용 사번/비밀번호와 신청 사유 전달
        const response = await fetch('/api/auth/return-from-leave/request', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({employeeNo, password, reason})
        });

        // 서버 응답 변환 후 로딩 종료
        const data = await response.json();
        hideLoading();

        // 업무 오류가 있으면 서버 메시지 우선 표시
        if (!response.ok) {
            document.getElementById('error-text').innerText =
                data.message || '복직 신청을 제출하지 못했습니다.';
            errorArea.style.display = 'flex';
            return;
        }

        // 성공 메시지 표시 후 입력값과 패널 상태 정리
        showSuccess('복직 신청이 제출되었습니다. SUPER 승인 후 다시 로그인할 수 있습니다.');
        document.getElementById('return-reason').value = '';
        hideReturnRequestPanel();
    } catch (error) {
        // 네트워크 오류 또는 서버 미응답 메시지 표시
        hideLoading();
        document.getElementById('error-text').innerText = '서버와 연결할 수 없습니다.';
        errorArea.style.display = 'flex';
    } finally {
        // 요청 완료 후 제출 버튼 재활성화
        button.disabled = false;
    }
}

/* 복직 신청 패널 표시 처리 */
function showReturnRequestPanel() {
    document.getElementById('return-request-panel').classList.add('open');
}

/* 복직 신청 패널 숨김 처리 */
function hideReturnRequestPanel() {
    document.getElementById('return-request-panel').classList.remove('open');
}

/* 성공 메시지 표시 처리 */
function showSuccess(message) {
    document.getElementById('success-text').innerText = message;
    document.getElementById('success-msg').style.display = 'flex';
}

/* 성공 메시지 숨김 처리 */
function hideSuccess() {
    document.getElementById('success-msg').style.display = 'none';
}

/* 로딩 오버레이 표시 처리 */
function showLoading() {
    document.getElementById('loading-overlay').style.display = 'flex';
}

/* 로딩 오버레이 숨김 처리 */
function hideLoading() {
    document.getElementById('loading-overlay').style.display = 'none';
}
