/* DOMContentLoaded 시 lucide 아이콘 초기화 */
document.addEventListener('DOMContentLoaded', () => {
    lucide.createIcons();

    /* 검색어 없이 검색 시 알림 후 1페이지로 이동 */
    document.querySelector('form').addEventListener('submit', function (e) {
        const keyword = this.querySelector('input[name="keyword"]').value.trim();
        if (!keyword) {
            e.preventDefault();
            alert('검색어를 입력해주세요.');
            location.href = '/board/general/list?page=1';
        }
    });
});