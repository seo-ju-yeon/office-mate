// Thymeleaf inline script에서 서버 데이터 주입
// const postId   = /*[[${post.id}]]*/ 0;
// const boardId  = /*[[${post.boardId}]]*/ 0;
// const authorNo = /*[[${post.authorNo}]]*/ '';

// 현재 로그인 사용자 정보
const currentEmployeeNo = localStorage.getItem('employeeNo') || '';
const currentRole = localStorage.getItem('role') || '';
const isAdmin = currentRole === 'ADMIN' || currentRole === 'SUPER';

/* 게시글 상세 버튼 노출 초기화 처리 */
(function initButtons() {
    const isOwner = currentEmployeeNo !== '' && currentEmployeeNo === authorNo;
    const canEdit = isOwner || isAdmin;

    if (canEdit) {
        let editPath;
        if (boardId === 1) editPath = `/board/notice/${postId}/edit`;
        else if (boardId === 2) editPath = `/board/general/${postId}/edit`;
        else editPath = `/board/project/notice/${postId}/edit`;

        const btnEdit = document.getElementById('btnEdit');
        btnEdit.href = editPath;
        btnEdit.style.display = '';
        document.getElementById('btnDelete').style.display = '';
    }

    const btnWrite = document.getElementById('btnWrite');
    if (btnWrite) {
        if (boardId === 1) {
            // 공지사항은 ADMIN/SUPER만 작성 가능
            if (isAdmin) {
                btnWrite.href = '/board/notice/write';
                btnWrite.style.display = '';
            }
        } else if (boardId === 2) {
            // 자유게시판은 로그인 사용자 작성 가능
            if (currentEmployeeNo) {
                btnWrite.href = '/board/general/write';
                btnWrite.style.display = '';
            }
        } else {
            // 프로젝트 공지는 ADMIN/SUPER만 작성 가능
            if (isAdmin) {
                btnWrite.href = '/board/project/notice/write';
                btnWrite.style.display = '';
            }
        }
    }
})();

/* 댓글 목록 초기 조회 처리 */
document.addEventListener('DOMContentLoaded', function () {
    loadComments();
});

/* 댓글 목록 조회 및 렌더링 처리 */
function loadComments() {
    const accessToken = localStorage.getItem('accessToken');

    fetch(`/api/board/${postId}/comments`, {
        headers: {'Authorization': 'Bearer ' + accessToken}
    })
        .then(res => res.json())
        .then(data => {
            const listElem = document.getElementById("commentList");
            // 기존 목록 초기화 후 재렌더링
            listElem.innerHTML = "";

            data.forEach(comment => {
                // 댓글 작성자 본인 또는 관리자만 삭제 버튼 노출
                const isAuthor = (comment.authorNo === currentEmployeeNo);
                const html = `
                <div style="padding: 16px 20px; border-bottom: 1px solid #eee; display: flex; flex-direction: column; gap: 6px;">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <div style="font-size: 13px;">
                            <strong style="color: #333;">${comment.authorName}</strong>
                            <span style="color: #999; margin-left: 8px;">${formatDate(comment.postedAt)}</span>
                        </div>
                        ${isAuthor || isAdmin ? `<button onclick="deleteComment(${comment.id})" style="background: none; border: none; color: #999; cursor: pointer; font-size: 12px; text-decoration: underline;">삭제</button>` : ''}
                    </div>
                    <div style="font-size: 14px; color: #555; line-height: 1.5; white-space: pre-wrap;">${comment.content}</div>
                </div>
            `;
                listElem.insertAdjacentHTML('beforeend', html);
            });
        })
        .catch(err => console.error("댓글 로드 실패:", err));
}

/* 댓글 작성 요청 처리 */
function saveComment() {
    const content = document.getElementById("commentContent").value;
    const accessToken = localStorage.getItem('accessToken');

    // 공백만 입력된 경우도 빈 내용으로 처리
    if (!content.trim()) {
        alert("내용을 입력해주세요.");
        return;
    }

    fetch(`/api/board/${postId}/comments`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + accessToken
        },
        body: JSON.stringify({content: content})
    })
        .then(res => {
            if (res.ok) {
                // 등록 성공 시 입력창 초기화 후 목록 새로고침
                document.getElementById("commentContent").value = "";
                loadComments();
            } else {
                // 토큰 만료 또는 인증 실패 안내
                alert("댓글 등록에 실패했습니다. 세션을 확인해주세요.");
            }
        });
}

/* 댓글 삭제 요청 처리 */
function deleteComment(commentId) {
    if (!confirm("댓글을 삭제하시겠습니까?")) return;
    const accessToken = localStorage.getItem('accessToken');

    fetch(`/api/board/comments/${commentId}`, {
        method: 'DELETE',
        headers: {'Authorization': 'Bearer ' + accessToken}
    })
        .then(res => {
            if (res.ok) loadComments(); // 삭제 성공 시 목록 새로고침
            else alert("본인이 작성한 댓글만 삭제할 수 있습니다.");
        });
}

/* 날짜/시간 표시 형식 변환 처리 */
function formatDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.getFullYear() + '.' +
        String(date.getMonth() + 1).padStart(2, '0') + '.' +
        String(date.getDate()).padStart(2, '0') + ' ' +
        String(date.getHours()).padStart(2, '0') + ':' +
        String(date.getMinutes()).padStart(2, '0');
}

/* 게시글 삭제 요청 처리 */
function deletePost(btn) {
    if (!confirm('게시글을 삭제하시겠습니까?')) return;
    const postId = btn.dataset.postId;
    const boardId = parseInt(btn.dataset.boardId);

    let listUrl;
    if (boardId === 1) listUrl = '/board/notice/list';
    else if (boardId === 2) listUrl = '/board/general/list';
    else listUrl = '/board/project/notice/list';

    const accessToken = localStorage.getItem('accessToken');
    fetch(`/api/board/${postId}`, {
        method: 'DELETE',
        headers: {'Authorization': 'Bearer ' + accessToken}
    }).then(res => {
        if (res.ok) {
            alert('삭제되었습니다.');
            window.location.href = listUrl;
        } else alert('삭제 권한이 없습니다.');
    });
}