// 사번 중복확인을 완료했는지 기억하는 화면 상태값
let employeeNoChecked = false;

// 중복확인을 완료한 사번 저장. 입력값이 바뀌면 이 값과 비교해 다시 확인
let checkedEmployeeNo = '';

// 이메일 중복확인을 완료했는지 기억하는 화면 상태값
let emailChecked = false;

// 중복확인을 완료한 이메일 저장. 입력값이 바뀌면 이 값과 비교해 다시 확인
let checkedEmail = '';

// 현재 로그인 사용자가 직원 등록 권한을 가지고 있는지 저장
let registerPermissionAllowed = false;

/* 직원 등록 화면 최초 진입 시 권한 확인과 입력 이벤트를 연결하는 초기화 메서드 */
document.addEventListener('DOMContentLoaded', function () {
    // 화면이 열리면 먼저 현재 사용자의 직원 등록 권한 확인
    checkRegisterPermission();

    // 등록 form 제출 이벤트 연결
    document.getElementById('employee-register-form').addEventListener('submit', createEmployee);

    // 사번 입력 시 화면 표시값도 대문자로 유지하고 기존 중복확인 상태 초기화
    document.getElementById('employeeNo').addEventListener('input', function () {
        this.value = this.value.toUpperCase();
        resetEmployeeNoCheck();
    });

    // 이메일 입력값 변경 시 기존 중복확인 상태 초기화
    document.getElementById('email').addEventListener('input', resetEmailCheck);
});

/* 현재 로그인 사용자가 직원 등록 가능한 ADMIN 또는 SUPER 권한인지 확인하는 메서드 */
async function checkRegisterPermission() {
    // 직원 등록은 ADMIN 또는 SUPER만 가능하므로 현재 로그인 사용자 먼저 조회
    const accessToken = localStorage.getItem('accessToken');

    // 토큰이 없으면 로그인 화면으로 이동
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // accessToken을 검증하고 현재 로그인 사용자 정보 조회
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        // 응답 본문을 JSON으로 변환
        const data = await response.json();

        // 토큰이 만료되었거나 잘못된 경우 다시 로그인하도록 이동
        if (!response.ok) {
            clearTokens();
            window.location.href = '/login';
            return;
        }

        // 레이아웃 사용자 이름 영역이 있으면 현재 로그인 사용자 이름으로 갱신
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

        // 권한 확인 완료 후 버튼 활성화 조건 재계산
        registerPermissionAllowed = true;
        updateSubmitButtonState();
    } catch (error) {
        // 사용자 정보 조회 실패 시 토큰 정리 후 로그인 화면 이동
        clearTokens();
        window.location.href = '/login';
    }
}

/* 직원 등록 전 입력한 사번이 이미 사용 중인지 서버에 확인하는 메서드 */
async function checkEmployeeNoDuplicate() {
    // API 인증에 사용할 accessToken과 입력한 사번 조회
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

    // 토큰이 없으면 로그인 화면으로 이동
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 사번에 특수문자가 있어도 쿼리스트링이 깨지지 않도록 인코딩 후 API 호출
        const response = await fetch('/api/management/employees/exists/employee-no?employeeNo=' + encodeURIComponent(employeeNo), {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        // 서버 응답을 JSON으로 변환
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

        // 중복확인을 통과한 사번을 저장하고 성공 메시지 표시
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

/* 직원 등록 전 입력한 이메일이 이미 사용 중인지 서버에 확인하는 메서드 */
async function checkEmailDuplicate() {
    // API 인증에 사용할 accessToken과 입력한 이메일 조회
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

    // 브라우저의 email 타입 검증 결과를 사용해 형식 오류를 먼저 안내
    if (!emailInput.checkValidity()) {
        fieldMessage.innerText = '이메일 형식이 올바르지 않습니다.';
        fieldMessage.className = 'field-message error';
        return;
    }

    // 토큰이 없으면 로그인 화면으로 이동
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 이메일에 특수문자가 있어도 쿼리스트링이 깨지지 않도록 인코딩 후 API 호출
        const response = await fetch('/api/management/employees/exists/email?email=' + encodeURIComponent(email), {
            method: 'GET',
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        // 서버 응답을 JSON으로 변환
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

        // 중복확인을 통과한 이메일을 저장하고 성공 메시지 표시
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

/* 사번 입력값이 변경되면 이전 중복확인 결과를 초기화하는 메서드 */
function resetEmployeeNoCheck() {
    // 이전 중복확인 결과는 더 이상 신뢰할 수 없으므로 초기화
    employeeNoChecked = false;
    checkedEmployeeNo = '';

    // 중복확인이 다시 필요하다는 안내 메시지 표시
    const fieldMessage = document.getElementById('employee-no-message');
    fieldMessage.innerText = '사번 입력 또는 변경 후 중복확인을 진행해주세요.';
    fieldMessage.className = 'field-message';

    // 직원 등록 버튼 활성화 조건 재계산
    updateSubmitButtonState();
}

/* 이메일 입력값이 변경되면 이전 중복확인 결과를 초기화하는 메서드 */
function resetEmailCheck() {
    // 이전 중복확인 결과는 더 이상 신뢰할 수 없으므로 초기화
    emailChecked = false;
    checkedEmail = '';

    // 중복확인이 다시 필요하다는 안내 메시지 표시
    const fieldMessage = document.getElementById('email-message');
    fieldMessage.innerText = '이메일 입력 또는 변경 후 중복확인을 진행해주세요.';
    fieldMessage.className = 'field-message';

    // 직원 등록 버튼 활성화 조건 재계산
    updateSubmitButtonState();
}

/* 직원 등록 form 제출 시 서버에 직원 생성 요청을 보내는 메서드 */
async function createEmployee(event) {
    // 직원 등록은 JSON API로 처리하므로 form 기본 제출 방지
    event.preventDefault();

    // API 호출에 필요한 accessToken과 현재 사번 조회
    const accessToken = localStorage.getItem('accessToken');
    const submitButton = document.getElementById('submit-button');
    const employeeNo = document.getElementById('employeeNo').value.toUpperCase().trim();
    const email = document.getElementById('email').value.trim().toLowerCase();

    // 토큰이 없으면 로그인 화면으로 이동
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    // 중복확인을 완료하지 않았거나 확인한 사번과 현재 사번이 다르면 등록 중단
    if (!employeeNoChecked || checkedEmployeeNo !== employeeNo) {
        showMessage('직원 등록 전 사번 중복확인을 완료해주세요.', 'error');
        return;
    }

    // 중복확인을 완료하지 않았거나 확인한 이메일과 현재 이메일이 다르면 등록 중단
    if (!emailChecked || checkedEmail !== email) {
        showMessage('직원 등록 전 이메일 중복확인을 완료해주세요.', 'error');
        return;
    }

    // Controller의 EmployeeCreateRequest 필드명과 맞춰 JSON 요청 body 생성
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

    // 이전 메시지 숨김 처리 후 중복 제출 방지를 위해 버튼 비활성화
    hideMessage();
    submitButton.disabled = true;

    try {
        // 직원 생성 API 호출. 서버에서 초기 비밀번호 암호화, 중복검사, 권한검사 처리
        const response = await fetch('/api/management/employees', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + accessToken
            },
            body: JSON.stringify(requestBody)
        });

        // 서버 응답을 JSON으로 변환
        const data = await response.json();

        // 중복 사번/이메일, 권한 부족 같은 업무 오류 표시
        if (!response.ok) {
            showMessage(data.message || '직원 등록에 실패했습니다.', 'error');
            return;
        }

        // 등록 성공 메시지 표시
        showMessage(
            data.employeeNo + ' 직원이 등록되었습니다. ' + data.initialPasswordGuide,
            'success'
        );

        // 입력 폼 초기화 후 사번 중복확인 상태도 초기화
        document.getElementById('employee-register-form').reset();
        resetEmployeeNoCheckAfterSuccess();
        resetEmailCheckAfterSuccess();
    } catch (error) {
        // 서버 통신 실패 메시지 표시
        showMessage('서버와 통신하지 못했습니다.', 'error');
    } finally {
        // 요청 종료 후 현재 중복확인 조건 기준으로 버튼 활성화 상태 재계산
        updateSubmitButtonState();
    }
}

/* 등록 성공 후 새 직원을 계속 등록할 수 있도록 사번 중복확인 상태를 초기화하는 메서드 */
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

/* 등록 성공 후 새 직원을 계속 등록할 수 있도록 이메일 중복확인 상태를 초기화하는 메서드 */
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

/* 직원 등록 버튼 활성화 조건을 계산해 버튼 상태를 갱신하는 메서드 */
function updateSubmitButtonState() {
    // 버튼과 현재 사번 입력값 조회
    const submitButton = document.getElementById('submit-button');
    const currentEmployeeNo = document.getElementById('employeeNo').value.toUpperCase().trim();
    const currentEmail = document.getElementById('email').value.trim().toLowerCase();

    // 권한 보유, 사번/이메일 중복확인 완료, 확인한 값과 현재 값 일치 여부를 모두 검사
    const canSubmit = registerPermissionAllowed
        && employeeNoChecked
        && checkedEmployeeNo === currentEmployeeNo
        && emailChecked
        && checkedEmail === currentEmail;

    // 모든 조건을 만족할 때만 직원 등록 버튼 활성화
    submitButton.disabled = !canSubmit;
}

/* 직원 등록 화면 상단의 공통 성공/오류 메시지 영역을 갱신하는 메서드 */
function showMessage(message, type) {
    // 메시지 문구와 상태 클래스를 화면에 반영
    const messageArea = document.getElementById('message-area');
    messageArea.innerText = message;
    messageArea.className = 'message-area ' + type;
}

/* 직원 등록 화면의 이전 성공/오류 메시지를 지우는 메서드 */
function hideMessage() {
    // 메시지 문구와 상태 클래스를 기본값으로 초기화
    const messageArea = document.getElementById('message-area');
    messageArea.innerText = '';
    messageArea.className = 'message-area';
}

/* 인증 실패 시 브라우저에 저장된 JWT와 사용자 정보를 정리하는 메서드 */
function clearTokens() {
    // 인증 토큰 제거
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');

    // 로그인 사용자 정보 제거
    localStorage.removeItem('employeeNo');
    localStorage.removeItem('employeeName');
    localStorage.removeItem('role');
}
