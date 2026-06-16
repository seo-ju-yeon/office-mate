/* Quill 초기화 */
const quill = new Quill('#quillEditor', {
    theme: 'snow',
    modules: {
        toolbar: [
            [{ font: [] }, { size: ['small', false, 'large', 'huge'] }],
            ['bold', 'italic', 'underline', 'strike'],
            [{ color: [] }, { background: [] }],
            [{ align: [] }],
            [{ list: 'ordered' }, { list: 'bullet' }],
        ]
    },
    placeholder: '내용을 입력해주세요'
});

// hidden input(th:value="${post.content}")에 담긴 HTML을 에디터에 주입
const existingContent = document.getElementById('content').value;
if (existingContent) {
    quill.root.innerHTML = existingContent;
}

/* 첨부파일 삭제 표시 - × 버튼 클릭 시 해당 파일 항목에 줄긋기를 표시하고,
   폼 제출 시 서버에 삭제 요청이 전달되도록 deleteAttachmentIds hidden input을 추가함 */
function markDelete(attachId, btn) {
    // 해당 첨부파일 항목에 deleted 클래스 추가 (CSS로 줄긋기 표시)
    const item = document.getElementById('existing-' + attachId);
    item.classList.add('deleted');

    // 삭제 취소가 가능하도록 × 버튼을 되돌리기 버튼으로 교체
    btn.outerHTML = `<button type="button" class="file-restore-btn"
                               onclick="restoreAttachment(${attachId}, this)">되돌리기</button>`;

    // 폼 제출 시 해당 attachId가 서버로 전달되도록 hidden input 생성 후 컨테이너에 추가
    const container = document.getElementById('deleteAttachmentInputs');
    const input = document.createElement('input');
    input.type  = 'hidden';
    input.name  = 'deleteAttachmentIds';
    input.value = attachId;
    input.id    = 'del-input-' + attachId; // 되돌리기 시 제거할 수 있도록 id 부여
    container.appendChild(input);
}

/* 첨부파일 삭제 복원 - 되돌리기 버튼 클릭 시 줄긋기를 제거하고,
   폼 제출 시 삭제 요청이 전달되지 않도록 hidden input을 제거함 */
function restoreAttachment(attachId, btn) {
    // deleted 클래스 제거 (줄긋기 표시 해제)
    const item = document.getElementById('existing-' + attachId);
    item.classList.remove('deleted');

    // 되돌리기 버튼을 다시 × 버튼으로 복원
    btn.outerHTML = `<button type="button" class="file-delete-btn"
                               onclick="markDelete(${attachId}, this)" title="삭제">×</button>`;

    // 앞서 추가했던 hidden input 제거 (서버에 삭제 요청이 가지 않도록)
    const delInput = document.getElementById('del-input-' + attachId);
    if (delInput) delInput.remove();
}

/* 수정 폼 제출 - 폼 제출 직전에 localStorage의 사번을 editorNo hidden input에 주입함.
   TokenCheckFilter가 /board/** 경로를 검사하지 않아 SecurityContext에서 사번을 꺼낼 수 없으므로,
   로그인 시 localStorage에 저장된 employeeNo를 직접 읽어 서버로 전달함 */
document.getElementById('editForm').addEventListener('submit', function (e) {

    // 빈 내용 검증
    if (quill.getText().trim().length === 0) {
        e.preventDefault();
        alert('내용을 입력해주세요.');
        quill.focus();
        return;
    }

    // 에디터 HTML → hidden input 주입
    document.getElementById('content').value = quill.root.innerHTML;

    // localStorage에서 로그인한 직원 사번을 꺼내 hidden input에 세팅
    document.getElementById('editorNo').value = localStorage.getItem('employeeNo');
});