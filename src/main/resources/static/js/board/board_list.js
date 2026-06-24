/* 전체 게시판 목록 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();

    /* 빈 검색어 제출 시 첫 페이지로 이동 처리 */
    document.querySelector('form').addEventListener('submit', function (e) {
        const keyword = this.querySelector('input[name="keyword"]').value.trim();
        if (!keyword) {
            e.preventDefault();
            alert('검색어를 입력해주세요.');
            location.href = '/board/list?page=1';
        }
    });
});