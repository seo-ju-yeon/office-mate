// lucide 아이콘을 실제 SVG로 변환
lucide.createIcons();

// 사번 입력값을 실시간으로 대문자로 변환하기 위한 입력창 조회
const empInput = document.getElementById('employeeId');

// 사번 입력 시 화면 표시값도 대문자로 유지
empInput.addEventListener('input', function () {
    this.value = this.value.toUpperCase();
});

/* 비밀번호 입력값의 표시/숨김 상태와 아이콘을 전환하는 메서드 */
function togglePassword() {
    // 비밀번호 입력칸과 토글 아이콘 DOM 조회
    const pwdInput = document.getElementById('password');
    const toggleIcon = document.getElementById('toggleIcon');

    // 숨김 상태면 텍스트로 바꾸고 아이콘을 eye-off로 변경
    if (pwdInput.type === 'password') {
        pwdInput.type = 'text';
        toggleIcon.setAttribute('data-lucide', 'eye-off');
    } else {
        // 보이는 상태면 다시 password 타입으로 바꾸고 아이콘을 eye로 변경
        pwdInput.type = 'password';
        toggleIcon.setAttribute('data-lucide', 'eye');
    }

    // data-lucide 속성 변경 후 아이콘을 다시 렌더링
    lucide.createIcons();
}

/* 로그인 폼 제출 시 JWT 로그인 API를 호출하고 결과에 따라 화면을 이동하는 메서드 */
document.getElementById('loginForm').addEventListener('submit', async function (e) {
    // 기본 form 제출로 페이지가 새로고침되지 않도록 차단
    e.preventDefault();

    // 이전 오류/성공/복직 신청 상태 초기화
    const errorArea = document.getElementById('error-msg');
    errorArea.style.display = 'none';
    hideSuccess();
    hideReturnRequestPanel();

    // 로그인 처리 중 로딩 표시
    showLoading();

    // 사번은 대문자로 정규화하고 비밀번호는 입력값 그대로 사용
    const employeeNo = document.getElementById('employeeId').value.toUpperCase().trim();
    const password = document.getElementById('password').value;

    try {
        // 로그인 API에 사번과 비밀번호를 JSON으로 전달
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({employeeNo, password})
        });

        // 서버 응답 본문을 JSON으로 변환하고 로딩 종료
        const data = await response.json();
        hideLoading();

        // 로그인 실패 응답이면 오류 유형에 맞는 메시지 표시
        if (!response.ok) {
            // 휴직 계정이면 복직 신청 패널을 함께 노출
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

        // 이후 API 호출과 화면 표시에서 사용할 로그인 정보를 저장
        localStorage.setItem('accessToken', data.accessToken);
        localStorage.setItem('employeeNo', data.employeeNo);
        localStorage.setItem('employeeName', data.name);
        localStorage.setItem('role', data.role);

        // 임시 비밀번호 사용자는 비밀번호 변경 화면으로 이동
        if (data.tempPasswordRequired) {
            window.location.href = '/password-change';
        } else {
            // 정상 로그인 사용자는 대시보드로 이동
            window.location.href = '/dashboard';
        }
    } catch (error) {
        // 네트워크 오류 또는 서버 미응답 시 공통 오류 메시지 표시
        hideLoading();
        document.getElementById('error-text').innerText = '서버와 연결할 수 없습니다.';
        errorArea.style.display = 'flex';
    }
});

/* 로그인 실패 응답 정보를 사용자에게 보여줄 안내 문구로 변환하는 메서드 */
function buildLoginFailureMessage(data) {
    // 계정 잠금 상태면 서버 메시지 또는 기본 잠금 안내 반환
    if (data.error === 'ACCOUNT_LOCKED') {
        return data.message || '로그인 실패 5회 이상으로 계정이 잠겼습니다. 관리자에게 문의해주세요.';
    }

    // 서버 기본 메시지와 실패 횟수 정보를 준비
    const baseMessage = data.message || '사번 또는 비밀번호가 올바르지 않습니다.';
    const failCount = Number(data.loginFailCount);
    const lockThreshold = Number(data.lockThreshold || 5);

    // 실패 횟수가 없으면 기본 메시지만 반환
    if (!Number.isFinite(failCount) || failCount <= 0) {
        return baseMessage;
    }

    // 실패 횟수와 잠금 기준을 함께 안내
    return baseMessage + ' 현재 로그인 실패 ' + failCount + '회입니다. '
        + lockThreshold + '회 이상 실패하면 계정이 잠깁니다.';
}

/* 휴직 계정의 복직 신청 API를 호출하는 메서드 */
async function submitReturnFromLeaveRequest() {
    // 복직 신청에 필요한 사번, 비밀번호, 사유 입력값 조회
    const employeeNo = document.getElementById('employeeId').value.toUpperCase().trim();
    const password = document.getElementById('password').value;
    const reason = document.getElementById('return-reason').value;
    const errorArea = document.getElementById('error-msg');
    const button = document.getElementById('return-request-btn');

    // 이전 오류/성공 메시지 초기화
    errorArea.style.display = 'none';
    hideSuccess();

    // 사번과 비밀번호가 없으면 서버 호출 전에 안내
    if (!employeeNo || !password) {
        document.getElementById('error-text').innerText = '사번과 비밀번호를 입력한 뒤 복직 신청을 진행해주세요.';
        errorArea.style.display = 'flex';
        return;
    }

    // 중복 제출을 막고 로딩 표시
    button.disabled = true;
    showLoading();

    try {
        // 복직 신청 API에 인증용 사번/비밀번호와 신청 사유 전달
        const response = await fetch('/api/auth/return-from-leave/request', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({employeeNo, password, reason})
        });

        // 서버 응답을 JSON으로 변환하고 로딩 종료
        const data = await response.json();
        hideLoading();

        // 업무 오류가 있으면 서버 메시지를 우선 표시
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
        // 네트워크 오류 또는 서버 미응답 시 공통 오류 메시지 표시
        hideLoading();
        document.getElementById('error-text').innerText = '서버와 연결할 수 없습니다.';
        errorArea.style.display = 'flex';
    } finally {
        // 요청 완료 후 제출 버튼 재활성화
        button.disabled = false;
    }
}

/* 복직 신청 패널을 화면에 표시하는 메서드 */
function showReturnRequestPanel() {
    // 복직 신청 패널에 open 클래스를 추가해 표시
    document.getElementById('return-request-panel').classList.add('open');
}

/* 복직 신청 패널을 화면에서 숨기는 메서드 */
function hideReturnRequestPanel() {
    // 복직 신청 패널의 open 클래스를 제거해 숨김
    document.getElementById('return-request-panel').classList.remove('open');
}

/* 성공 메시지 영역에 안내 문구를 표시하는 메서드 */
function showSuccess(message) {
    // 성공 메시지 문구를 넣고 영역을 표시
    document.getElementById('success-text').innerText = message;
    document.getElementById('success-msg').style.display = 'flex';
}

/* 성공 메시지 영역을 숨기는 메서드 */
function hideSuccess() {
    // 성공 메시지 영역을 숨김
    document.getElementById('success-msg').style.display = 'none';
}

/* 화면 전체 로딩 오버레이를 표시하는 메서드 */
function showLoading() {
    // 로그인 또는 복직 신청 요청 처리 중임을 표시
    document.getElementById('loading-overlay').style.display = 'flex';
}

/* 화면 전체 로딩 오버레이를 숨기는 메서드 */
function hideLoading() {
    // 요청 처리가 끝났으므로 로딩 오버레이 숨김
    document.getElementById('loading-overlay').style.display = 'none';
}
