// 사번 중복확인 완료 여부
let employeeNoChecked = false;

// 중복확인을 완료한 사번
let checkedEmployeeNo = '';

// 이메일 중복확인 완료 여부
let emailChecked = false;

// 중복확인을 완료한 이메일
let checkedEmail = '';

// 현재 로그인 사용자의 직원 등록 권한 여부
let registerPermissionAllowed = false;

/* 직원 등록 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', function () {
    // 현재 사용자의 직원 등록 권한 확인
    checkRegisterPermission();

    // 등록 form 제출 이벤트 연결
    document.getElementById('employee-register-form').addEventListener('submit', createEmployee);

    // 사번 입력 시 대문자 변환과 중복확인 상태 초기화
    document.getElementById('employeeNo').addEventListener('input', function () {
        this.value = this.value.toUpperCase();
        resetEmployeeNoCheck();
    });

    // 이메일 입력값 변경 시 중복확인 상태 초기화
    document.getElementById('email').addEventListener('input', resetEmailCheck);
});

/* 직원 등록 권한 검증 처리 */
async function checkRegisterPermission() {
    // 직원 등록은 ADMIN 또는 SUPER만 가능하므로 현재 사용자 조회
    const accessToken = localStorage.getItem('accessToken');

    // 토큰이 없으면 로그인 화면 이동 처리
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 현재 로그인 사용자 정보 조회 요청
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        const data = await response.json();

        // 인증 실패 시 토큰 정리 후 로그인 화면 이동
        if (!response.ok) {
            clearTokens();
            window.location.href = '/login';
            return;
        }

        // 레이아웃 사용자 이름 갱신
        const layoutUserName = document.getElementById('layout-user-name');
        if (layoutUserName) {
            layoutUserName.innerText = data.name;
        }

        // 권한이 없는 사용자는 등록 버튼 비활성화와 안내 메시지 표시
        if (data.role !== 'ADMIN' && data.role !== 'SUPER') {
            showMessage('직원 등록은 ADMIN 또는 SUPER 권한만 사용할 수 있습니다.', 'error');
            document.getElementById('submit-button').disabled = true;
            registerPermissionAllowed = false;
            return;
        }

        // 권한 확인 후 버튼 활성화 조건 재계산
        registerPermissionAllowed = true;
        updateSubmitButtonState();
    } catch (error) {
        // 사용자 정보 조회 실패 시 토큰 정리 후 로그인 화면 이동
        clearTokens();
        window.location.href = '/login';
    }
}

/* 사번 중복확인 요청 처리 */
async function checkEmployeeNoDuplicate() {
    // 인증 토큰과 입력 사번 준비
    const accessToken = localStorage.getItem('accessToken');
    const employeeNo = document.getElementById('employeeNo').value.toUpperCase().trim();
    const fieldMessage = document.getElementById('employee-no-message');

    // 이전 중복확인 상태와 메시지 초기화
    employeeNoChecked = false;
    checkedEmployeeNo = '';
    fieldMessage.innerText = '';
    fieldMessage.className = 'field-message';

    // 사번이 비어 있으면 중복확인 요청 중단
    if (!employeeNo) {
        fieldMessage.innerText = '사번을 입력한 뒤 중복확인을 눌러주세요.';
        fieldMessage.className = 'field-message error';
        return;
    }

    // 토큰이 없으면 로그인 화면 이동 처리
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 사번 인코딩 후 중복확인 요청
        const response = await fetch('/api/management/employees/exists/employee-no?employeeNo=' + encodeURIComponent(employeeNo), {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        const data = await response.json();

        // 중복확인 API 오류 메시지 표시
        if (!response.ok) {
            fieldMessage.innerText = data.message || '사번 중복확인에 실패했습니다.';
            fieldMessage.className = 'field-message error';
            return;
        }

        // 이미 사용 중인 사번이면 오류 메시지 표시
        if (data.exists) {
            fieldMessage.innerText = '이미 사용 중인 사번입니다.';
            fieldMessage.className = 'field-message error';
            return;
        }

        // 중복확인 통과 사번 저장과 성공 메시지 표시
        employeeNoChecked = true;
        checkedEmployeeNo = employeeNo;
        fieldMessage.innerText = '사용 가능한 사번입니다.';
        fieldMessage.className = 'field-message success';

        // 직원 등록 버튼 활성화 조건 재계산
        updateSubmitButtonState();
    } catch (error) {
        // 서버 통신 실패 메시지 표시
        fieldMessage.innerText = '서버와 통신하지 못했습니다.';
        fieldMessage.className = 'field-message error';
    }
}

/* 이메일 중복확인 요청 처리 */
async function checkEmailDuplicate() {
    // 인증 토큰과 입력 이메일 준비
    const accessToken = localStorage.getItem('accessToken');
    const emailInput = document.getElementById('email');
    const email = emailInput.value.trim().toLowerCase();
    const fieldMessage = document.getElementById('email-message');

    // 이전 중복확인 상태와 메시지 초기화
    emailChecked = false;
    checkedEmail = '';
    fieldMessage.innerText = '';
    fieldMessage.className = 'field-message';

    // 이메일이 비어 있으면 중복확인 요청 중단
    if (!email) {
        fieldMessage.innerText = '이메일을 입력한 뒤 중복확인을 눌러주세요.';
        fieldMessage.className = 'field-message error';
        return;
    }

    emailInput.value = email;

    // 브라우저 email 타입 검증으로 형식 오류 안내
    if (!emailInput.checkValidity()) {
        fieldMessage.innerText = '이메일 형식이 올바르지 않습니다.';
        fieldMessage.className = 'field-message error';
        return;
    }

    // 토큰이 없으면 로그인 화면 이동 처리
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 이메일 인코딩 후 중복확인 요청
        const response = await fetch('/api/management/employees/exists/email?email=' + encodeURIComponent(email), {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        const data = await response.json();

        // 중복확인 API 오류 메시지 표시
        if (!response.ok) {
            fieldMessage.innerText = data.message || '이메일 중복확인에 실패했습니다.';
            fieldMessage.className = 'field-message error';
            return;
        }

        // 이미 사용 중인 이메일이면 오류 메시지 표시
        if (data.exists) {
            fieldMessage.innerText = '이미 사용 중인 이메일입니다.';
            fieldMessage.className = 'field-message error';
            return;
        }

        // 중복확인 통과 이메일 저장과 성공 메시지 표시
        emailChecked = true;
        checkedEmail = email;
        fieldMessage.innerText = '사용 가능한 이메일입니다.';
        fieldMessage.className = 'field-message success';

        // 직원 등록 버튼 활성화 조건 재계산
        updateSubmitButtonState();
    } catch (error) {
        // 서버 통신 실패 메시지 표시
        fieldMessage.innerText = '서버와 통신하지 못했습니다.';
        fieldMessage.className = 'field-message error';
    }
}

/* 사번 중복확인 상태 초기화 처리 */
function resetEmployeeNoCheck() {
    // 입력값 변경으로 이전 중복확인 결과 초기화
    employeeNoChecked = false;
    checkedEmployeeNo = '';

    // 중복확인 재진행 안내 메시지 표시
    const fieldMessage = document.getElementById('employee-no-message');
    fieldMessage.innerText = '사번 입력 또는 변경 후 중복확인을 진행해주세요.';
    fieldMessage.className = 'field-message';

    // 직원 등록 버튼 활성화 조건 재계산
    updateSubmitButtonState();
}

/* 이메일 중복확인 상태 초기화 처리 */
function resetEmailCheck() {
    // 입력값 변경으로 이전 중복확인 결과 초기화
    emailChecked = false;
    checkedEmail = '';

    // 중복확인 재진행 안내 메시지 표시
    const fieldMessage = document.getElementById('email-message');
    fieldMessage.innerText = '이메일 입력 또는 변경 후 중복확인을 진행해주세요.';
    fieldMessage.className = 'field-message';

    // 직원 등록 버튼 활성화 조건 재계산
    updateSubmitButtonState();
}

/* 직원 생성 요청 처리 */
async function createEmployee(event) {
    // JSON API로 처리하기 위해 기본 form 제출 방지
    event.preventDefault();

    // 인증 토큰과 현재 입력값 준비
    const accessToken = localStorage.getItem('accessToken');
    const submitButton = document.getElementById('submit-button');
    const employeeNo = document.getElementById('employeeNo').value.toUpperCase().trim();
    const email = document.getElementById('email').value.trim().toLowerCase();

    // 토큰이 없으면 로그인 화면 이동 처리
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    // 확인한 사번과 현재 사번이 다르면 등록 중단
    if (!employeeNoChecked || checkedEmployeeNo !== employeeNo) {
        showMessage('직원 등록 전 사번 중복확인을 완료해주세요.', 'error');
        return;
    }

    // 확인한 이메일과 현재 이메일이 다르면 등록 중단
    if (!emailChecked || checkedEmail !== email) {
        showMessage('직원 등록 전 이메일 중복확인을 완료해주세요.', 'error');
        return;
    }

    // EmployeeCreateRequest 필드명에 맞춰 요청 body 생성
    const requestBody = {
        employeeNo: employeeNo,
        name: document.getElementById('name').value.trim(),
        email: email,
        department: document.getElementById('department').value,
        position: document.getElementById('position').value,
        role: document.getElementById('role').value,
        joinedOn: document.getElementById('joinedOn').value || null,
        employeeNoDuplicateChecked: true,
        emailDuplicateChecked: true
    };

    // 이전 메시지 초기화 후 중복 제출 방지 처리
    hideMessage();
    submitButton.disabled = true;

    try {
        // 직원 생성 요청
        const response = await fetch('/api/management/employees', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + accessToken
            },
            body: JSON.stringify(requestBody)
        });

        const data = await response.json();

        // 중복 사번/이메일, 권한 부족 등 업무 오류 표시
        if (!response.ok) {
            showMessage(data.message || '직원 등록에 실패했습니다.', 'error');
            return;
        }

        // 등록 성공 메시지 표시
        showMessage(
            data.employeeNo + ' 직원이 등록되었습니다. ' + data.initialPasswordGuide,
            'success'
        );

        // 입력 폼과 중복확인 상태 초기화
        document.getElementById('employee-register-form').reset();
        resetEmployeeNoCheckAfterSuccess();
        resetEmailCheckAfterSuccess();
    } catch (error) {
        // 서버 통신 실패 메시지 표시
        showMessage('서버와 통신하지 못했습니다.', 'error');
    } finally {
        // 요청 종료 후 현재 조건 기준으로 버튼 상태 재계산
        updateSubmitButtonState();
    }
}

/* 등록 성공 후 사번 중복확인 상태 초기화 처리 */
function resetEmployeeNoCheckAfterSuccess() {
    // 사번 중복확인 상태 초기화
    employeeNoChecked = false;
    checkedEmployeeNo = '';

    // 사번 필드 메시지 초기화
    const fieldMessage = document.getElementById('employee-no-message');
    fieldMessage.innerText = '';
    fieldMessage.className = 'field-message';

    // 직원 등록 버튼 활성화 조건 재계산
    updateSubmitButtonState();
}

/* 등록 성공 후 이메일 중복확인 상태 초기화 처리 */
function resetEmailCheckAfterSuccess() {
    // 이메일 중복확인 상태 초기화
    emailChecked = false;
    checkedEmail = '';

    // 이메일 필드 메시지 초기화
    const fieldMessage = document.getElementById('email-message');
    fieldMessage.innerText = '';
    fieldMessage.className = 'field-message';

    // 직원 등록 버튼 활성화 조건 재계산
    updateSubmitButtonState();
}

/* 직원 등록 버튼 상태 갱신 처리 */
function updateSubmitButtonState() {
    // 현재 입력값과 버튼 조회
    const submitButton = document.getElementById('submit-button');
    const currentEmployeeNo = document.getElementById('employeeNo').value.toUpperCase().trim();
    const currentEmail = document.getElementById('email').value.trim().toLowerCase();

    // 권한과 중복확인 완료 여부 검사
    const canSubmit = registerPermissionAllowed
        && employeeNoChecked
        && checkedEmployeeNo === currentEmployeeNo
        && emailChecked
        && checkedEmail === currentEmail;

    // 모든 조건을 만족할 때만 등록 버튼 활성화
    submitButton.disabled = !canSubmit;
}

/* 직원 등록 메시지 표시 처리 */
function showMessage(message, type) {
    // 메시지 문구와 상태 클래스 반영
    const messageArea = document.getElementById('message-area');
    messageArea.innerText = message;
    messageArea.className = 'message-area ' + type;
}

/* 직원 등록 메시지 초기화 처리 */
function hideMessage() {
    // 메시지 문구와 상태 클래스 초기화
    const messageArea = document.getElementById('message-area');
    messageArea.innerText = '';
    messageArea.className = 'message-area';
}

/* 브라우저 로그인 정보 제거 처리 */
function clearTokens() {
    // 인증 토큰 제거
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');

    // 로그인 사용자 정보 제거
    localStorage.removeItem('employeeNo');
    localStorage.removeItem('employeeName');
    localStorage.removeItem('role');
}
