/* 공지사항 목록 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();

    /* 빈 검색어 제출 시 첫 페이지로 이동 처리 */
    document.querySelector('form').addEventListener('submit', function (e) {
        const keyword = this.querySelector('input[name="keyword"]').value.trim();
        if (!keyword) {
            e.preventDefault();
            alert('검색어를 입력해주세요.');
            location.href = '/board/notice/list?page=1';
        }
    });
});

/* 공지사항 작성 권한 확인 후 페이지 이동 처리 */
function goToNoticeWrite() {
    // 클라이언트 토큰으로 서버 권한 체크 API를 먼저 호출함
    const accessToken = localStorage.getItem('accessToken');

    // 토큰이 없거나 권한 부족이면 서버가 401 또는 403 반환
    fetch('/api/board/notice/write-check', {
        headers: {'Authorization': 'Bearer ' + accessToken}
    })
        .then(res => {
            if (res.ok) {
                // ADMIN/SUPER 권한 확인 후 작성 화면 이동
                window.location.href = '/board/notice/write';
            } else {
                // 권한 없음 응답이면 이동 대신 모달 안내
                document.getElementById('noAuthModal').style.display = 'flex';
            }
        });
}