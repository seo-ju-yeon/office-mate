/* DOMContentLoaded 시 lucide 아이콘 초기화 - 페이지 로드 후 아이콘이 렌더링되도록 실행함 */
document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();

    /* 검색어 없이 검색 시 알림 후 1페이지로 이동 */
    document.querySelector('form').addEventListener('submit', function (e) {
        const keyword = this.querySelector('input[name="keyword"]').value.trim();
        if (!keyword) {
            e.preventDefault();
            alert('검색어를 입력해주세요.');
            location.href = '/board/notice/list?page=1';
        }
    });
});

/* 공지사항 작성 페이지 이동 - 클라이언트 토큰으로 권한 체크 API를 먼저 호출해
   ADMIN·SUPER 권한 확인 후 작성 페이지로 이동하거나 권한 없음 모달을 표시함 */
function goToNoticeWrite() {
    // localStorage에서 액세스 토큰 로드
    const accessToken = localStorage.getItem('accessToken');

    // Authorization 헤더에 토큰을 담아 서버에서 권한 검증 요청
    // (토큰 없거나 권한 부족 시 서버가 401 또는 403 반환)
    fetch('/api/board/notice/write-check', {
        headers: { 'Authorization': 'Bearer ' + accessToken }
    })
        .then(res => {
            if (res.ok) {
                // 권한 확인됨 → 공지사항 작성 페이지로 이동
                window.location.href = '/board/notice/write';
            } else {
                // 권한 없음 (401·403) → 페이지 이동 대신 모달로 안내
                document.getElementById('noAuthModal').style.display = 'flex';
            }
        });
}