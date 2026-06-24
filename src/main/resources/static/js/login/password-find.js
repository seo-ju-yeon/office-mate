// 비밀번호 찾기 사번 입력 요소
const findEmployeeNoInput = document.getElementById('find-employee-no');

// 사번 입력 시 대문자 표시 처리
findEmployeeNoInput.addEventListener('input', function () {
    this.value = this.value.toUpperCase();
});

/* 임시 비밀번호 발송 요청 처리 */
async function requestPasswordReset() {
    // 사용자가 입력한 사번과 이메일 값 조회
    const employeeNo = document.getElementById('find-employee-no').value.toUpperCase().trim();
    const email = document.getElementById('find-email').value;

    // 필수 입력값이 비어 있으면 서버 호출 전 안내
    if (!employeeNo || !email) {
        alert('정보를 모두 입력해주세요.');
        return;
    }

    try {
        // 비밀번호 재설정 요청 API 호출
        const response = await fetch('/api/auth/password-reset/request', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                employeeNo: employeeNo,
                email: email
            })
        });

        const data = await response.json();

        // 사번/이메일 불일치 또는 메일 발송 실패 메시지 표시
        if (!response.ok) {
            alert(data.message || '입력한 사번과 이메일을 확인해주세요.');
            return;
        }

        // 재설정 화면에서 사번을 다시 입력하지 않도록 임시 저장
        sessionStorage.setItem('passwordResetEmployeeNo', employeeNo);

        // 발송 완료 단계로 화면 전환
        document.getElementById('target-email').innerText = email;
        document.getElementById('step1').classList.remove('active');
        document.getElementById('step2').classList.add('active');
    } catch (error) {
        // 네트워크 오류 또는 서버 미응답 메시지 표시
        alert('서버와 통신하지 못했습니다. 애플리케이션 실행 상태를 확인해주세요.');
    }
}
