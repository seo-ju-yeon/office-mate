// boardType은 HTML의 <script th:inline="javascript"> 블록에서 Thymeleaf가 서버 데이터를 주입함
// const boardType = /*[[${boardType}]]*/ 'general';

//  Quill 초기화
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

// DataTransfer를 활용해 파일 목록을 직접 관리 (input.files는 읽기 전용이므로 우회)
const dt = new DataTransfer();

/*
 * 프로젝트 공지 작성 시 드롭다운 초기화
 * - /api/board/project/boards 호출 → 내가 속한 프로젝트 게시판 목록 조회
 * - 드롭다운 선택 시 boardId hidden input에 값 주입
 */
async function loadProjectBoards() {
    const accessToken = localStorage.getItem('accessToken');
    try {
        const res = await fetch('/api/board/project/boards/writable', {
            headers: { 'Authorization': 'Bearer ' + accessToken }
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

        // URL에 boardId 파라미터가 있으면 해당 옵션 자동 선택, 없으면 첫 번째 프로젝트 선택
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
        console.error('프로젝트 게시판 로드 실패:', err);
    }
}

// 프로젝트 공지 작성 페이지일 때만 드롭다운 로드
if (boardType === 'projectNotice') {
    document.addEventListener('DOMContentLoaded', loadProjectBoards);
}

/* 파일 선택 처리 - 동일한 파일명이 이미 존재하면 중복 추가를 방지하고,
   DataTransfer에 파일을 누적한 후 파일 목록을 다시 렌더링함 */
function handleFileSelect(input) {
    Array.from(input.files).forEach(file => {
        // 동일 파일명 중복 추가 방지
        const exists = Array.from(dt.files).some(f => f.name === file.name);
        if (!exists) dt.items.add(file);
    });
    // input.files를 DataTransfer 기준으로 갱신 (폼 제출 시 누적된 파일이 전송되도록)
    input.files = dt.files;
    renderFileList();
}

/* 파일 제거 - 선택된 파일명을 DataTransfer에서 제거하고 input.files와 목록을 동기화함.
   DataTransfer는 직접 삭제가 불가능해 새 DataTransfer를 생성해 교체하는 방식으로 처리함 */
function removeFile(fileName) {
    const newDt = new DataTransfer();
    // 삭제 대상을 제외한 나머지 파일만 새 DataTransfer에 추가
    Array.from(dt.files)
        .filter(f => f.name !== fileName)
        .forEach(f => newDt.items.add(f));

    // 기존 DataTransfer 초기화 후 새 목록으로 교체
    while (dt.items.length > 0) dt.items.remove(0);
    Array.from(newDt.files).forEach(f => dt.items.add(f));

    // input.files도 동기화 (폼 제출 시 반영되도록)
    document.getElementById('fileInput').files = dt.files;
    renderFileList();
}

/* 파일 목록 렌더링 - DataTransfer에 담긴 파일 목록을 파일명·크기·삭제 버튼으로 화면에 표시함 */
function renderFileList() {
    const list = document.getElementById('fileList');
    // 기존 목록 초기화 후 재렌더링 (중복 방지)
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

/* 게시글 작성 폼 제출 - 토큰 유효성 확인 후 multipart/form-data로 API를 호출하고,
   pinned 체크박스를 수동으로 처리해 미체크 시 서버에 false가 전달되지 않는 문제를 방지함 */
document.getElementById('writeForm').addEventListener('submit', async function (e) {
    e.preventDefault();

    // 빈 내용 검증 (Quill 기본 빈 상태는 '<p><br></p>')
    if (quill.getText().trim().length === 0) {
        alert('내용을 입력해주세요.');
        quill.focus();
        return;
    }

    // 프로젝트 공지: 프로젝트 선택 여부 검증
    if (boardType === 'projectNotice') {
        const boardId = document.getElementById('boardId').value;
        if (!boardId) {
            alert('프로젝트를 선택해주세요.');
            document.getElementById('projectBoardSelect').focus();
            return;
        }
    }

    // 에디터 HTML → hidden input 주입
    document.getElementById('content').value = quill.root.innerHTML;

    const accessToken = localStorage.getItem('accessToken');

    // 토큰 없으면 로그인 페이지로 이동
    if (!accessToken) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login';
        return;
    }

    const formData = new FormData(this);

    // 체크박스 미체크 시 FormData에 포함되지 않는 문제 방지를 위해 수동으로 처리
    formData.delete('pinned');
    const pinnedCheckbox = document.getElementById('pinned');
    if (pinnedCheckbox && pinnedCheckbox.checked) {
        formData.append('pinned', 'true');
    }

    try {
        const response = await fetch('/api/board/write', {
            method: 'POST',
            headers: { 'Authorization': 'Bearer ' + accessToken },
            body: formData
        });

        if (response.ok) {
            // 등록 성공 시 boardType에 따라 해당 게시판 목록으로 이동
            if (boardType === 'notice')             window.location.href = '/board/notice/list';
            else if (boardType === 'projectNotice') window.location.href = '/board/project/notice/list';
            else                                    window.location.href = '/board/general/list';
        } else if (response.status === 401) {
            // 토큰 만료 또는 인증 실패 시 로그인 페이지로 이동
            alert('로그인이 필요합니다.');
            window.location.href = '/login';
        } else {
            alert('게시글 등록에 실패했습니다.');
        }
    } catch (err) {
        // 네트워크 오류 등 예외 처리
        console.error('게시글 등록 오류:', err);
        alert('오류가 발생했습니다.');
    }
});