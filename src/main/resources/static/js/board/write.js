// Thymeleaf inline script에서 서버가 주입하는 게시판 타입
// const boardType = /*[[${boardType}]]*/ 'general';

/* 게시글 작성 Quill 에디터 초기화 처리 */
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

// input.files가 읽기 전용이라 DataTransfer로 선택 파일 목록 관리
const dt = new DataTransfer();

/* 프로젝트 공지 작성용 게시판 드롭다운 초기화 처리 */
async function loadProjectBoards() {
    const accessToken = localStorage.getItem('accessToken');
    try {
        const res = await fetch('/api/board/project/boards/writable', {
            headers: {'Authorization': 'Bearer ' + accessToken}
        });
        if (!res.ok) throw new Error('프로젝트 목록 조회 실패');

        const boards = await res.json();
        const select = document.getElementById('projectBoardSelect');

        if (boards.length === 0) {
            select.innerHTML = '<option value="">맡은 프로젝트가 없습니다.</option>';
            return;
        }

        boards.forEach(board => {
            const option = document.createElement('option');
            option.value = board.boardid;
            option.textContent = board.projectname;
            select.appendChild(option);
        });

        // 목록에서 넘어온 boardId가 있으면 해당 프로젝트 자동 선택
        const urlBoardId = new URLSearchParams(location.search).get('boardId');
        if (urlBoardId && select.querySelector(`option[value="${urlBoardId}"]`)) {
            select.value = urlBoardId;
        } else {
            select.selectedIndex = 1;
        }

        document.getElementById('boardId').value = select.value;

        select.addEventListener('change', function () {
            document.getElementById('boardId').value = this.value;
        });

    } catch (err) {
        // 오류 추적용 콘솔 기록 유지
        console.error('프로젝트 게시판 로드 실패:', err);
    }
}

// 프로젝트 공지 작성 화면에서만 드롭다운 로드
if (boardType === 'projectNotice') {
    document.addEventListener('DOMContentLoaded', loadProjectBoards);
}

/* 파일 선택 목록 누적 처리 */
function handleFileSelect(input) {
    Array.from(input.files).forEach(file => {
        // 동일 파일명 중복 추가 방지
        const exists = Array.from(dt.files).some(f => f.name === file.name);
        if (!exists) dt.items.add(file);
    });
    // FormData 제출 시 누적 파일이 전송되도록 input.files 동기화
    input.files = dt.files;
    renderFileList();
}

/* 선택 파일 제거 처리 */
function removeFile(fileName) {
    const newDt = new DataTransfer();
    // DataTransfer 직접 삭제가 어려워 삭제 대상 제외 후 새 목록 생성
    Array.from(dt.files)
        .filter(f => f.name !== fileName)
        .forEach(f => newDt.items.add(f));

    // 기존 DataTransfer를 새 목록으로 교체
    while (dt.items.length > 0) dt.items.remove(0);
    Array.from(newDt.files).forEach(f => dt.items.add(f));

    // FormData 제출에 반영되도록 input.files 동기화
    document.getElementById('fileInput').files = dt.files;
    renderFileList();
}

/* 선택 파일 목록 렌더링 처리 */
function renderFileList() {
    const list = document.getElementById('fileList');
    // 기존 목록 초기화 후 재렌더링
    list.innerHTML = '';
    Array.from(dt.files).forEach(file => {
        const size = (file.size / 1024).toFixed(1) + ' KB';
        const item = document.createElement('div');
        item.className = 'file-item';
        item.innerHTML = `
            <span>${file.name} <span style="color:#aaa">(${size})</span></span>
            <button type="button" class="file-remove"
                    onclick="removeFile('${file.name}')" title="삭제">×</button>
        `;
        list.appendChild(item);
    });
}

/* 게시글 작성 폼 제출 처리 */
document.getElementById('writeForm').addEventListener('submit', async function (e) {
    e.preventDefault();

    // Quill 기본 빈 상태까지 고려해 본문 검증
    if (quill.getText().trim().length === 0) {
        alert('내용을 입력해주세요.');
        quill.focus();
        return;
    }

    // 프로젝트 공지는 프로젝트 선택 여부 검증
    if (boardType === 'projectNotice') {
        const boardId = document.getElementById('boardId').value;
        if (!boardId) {
            alert('프로젝트를 선택해주세요.');
            document.getElementById('projectBoardSelect').focus();
            return;
        }
    }

    // 에디터 HTML을 hidden input에 주입
    document.getElementById('content').value = quill.root.innerHTML;

    const accessToken = localStorage.getItem('accessToken');

    // 토큰 없으면 로그인 페이지 이동 처리
    if (!accessToken) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login';
        return;
    }

    const formData = new FormData(this);

    // 미체크 체크박스가 FormData에 포함되지 않는 문제 방지
    formData.delete('pinned');
    const pinnedCheckbox = document.getElementById('pinned');
    if (pinnedCheckbox && pinnedCheckbox.checked) {
        formData.append('pinned', 'true');
    }

    try {
        const response = await fetch('/api/board/write', {
            method: 'POST',
            headers: {'Authorization': 'Bearer ' + accessToken},
            body: formData
        });

        if (response.ok) {
            // 등록 성공 시 boardType에 맞는 목록으로 이동
            if (boardType === 'notice') window.location.href = '/board/notice/list';
            else if (boardType === 'projectNotice') window.location.href = '/board/project/notice/list';
            else window.location.href = '/board/general/list';
        } else if (response.status === 401) {
            // 토큰 만료 또는 인증 실패 시 로그인 페이지 이동
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
        } else {
            alert('게시글 등록에 실패했습니다.');
        }
    } catch (err) {
        // 오류 추적용 콘솔 기록 유지
        console.error('게시글 등록 오류:', err);
        alert('오류가 발생했습니다.');
    }
});