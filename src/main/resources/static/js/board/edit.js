/* 게시글 수정 Quill 에디터 초기화 처리 */
const quill = new Quill('#quillEditor', {
    theme: 'snow',
    modules: {
        toolbar: [
            [{font: []}, {size: ['small', false, 'large', 'huge']}],
            ['bold', 'italic', 'underline', 'strike'],
            [{color: []}, {background: []}],
            [{align: []}],
            [{list: 'ordered'}, {list: 'bullet'}],
        ]
    },
    placeholder: '내용을 입력해주세요'
});

// Thymeleaf가 hidden input에 주입한 기존 본문 HTML
const existingContent = document.getElementById('content').value;
if (existingContent) {
    quill.root.innerHTML = existingContent;
}

/* 첨부파일 삭제 표시 처리 */
function markDelete(attachId, btn) {
    // 삭제 예정 파일에 줄긋기 표시
    const item = document.getElementById('existing-' + attachId);
    item.classList.add('deleted');

    // 삭제 취소가 가능하도록 되돌리기 버튼으로 교체
    btn.outerHTML = `<button type="button" class="file-restore-btn"
                               onclick="restoreAttachment(${attachId}, this)">되돌리기</button>`;

    // 폼 제출 시 서버로 삭제 대상 attachId 전달
    const container = document.getElementById('deleteAttachmentInputs');
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = 'deleteAttachmentIds';
    input.value = attachId;
    input.id = 'del-input-' + attachId; // 되돌리기 시 제거하기 위한 id
    container.appendChild(input);
}

/* 첨부파일 삭제 복원 처리 */
function restoreAttachment(attachId, btn) {
    // 줄긋기 표시 해제
    const item = document.getElementById('existing-' + attachId);
    item.classList.remove('deleted');

    // 되돌리기 버튼을 삭제 버튼으로 복원
    btn.outerHTML = `<button type="button" class="file-delete-btn"
                               onclick="markDelete(${attachId}, this)" title="삭제">×</button>`;

    // 삭제 요청용 hidden input 제거
    const delInput = document.getElementById('del-input-' + attachId);
    if (delInput) delInput.remove();
}

/* 게시글 수정 폼 제출 처리 */
document.getElementById('editForm').addEventListener('submit', function (e) {

    // Quill 본문 빈 값 검증
    if (quill.getText().trim().length === 0) {
        e.preventDefault();
        alert('내용을 입력해주세요.');
        quill.focus();
        return;
    }

    // 에디터 HTML을 hidden input에 주입
    document.getElementById('content').value = quill.root.innerHTML;

    // TokenCheckFilter가 /board/**를 검사하지 않아 SecurityContext에서 사번을 꺼낼 수 없던 이력 있음
    // 로그인 시 localStorage에 저장된 employeeNo를 editorNo hidden input으로 전달
    document.getElementById('editorNo').value = localStorage.getItem('employeeNo');
});