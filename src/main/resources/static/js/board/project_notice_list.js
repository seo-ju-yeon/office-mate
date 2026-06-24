let currentPage = 1;
let selectedBoardId = null;   // 드롭다운에서 선택된 board.id
let selectedProjectId = null; // 글쓰기 버튼에 전달할 project.id
const PAGE_SIZE = 10;

// 프로젝트 상태 탭 그룹
const STATUS_GROUPS = {
    active: ['READY', 'IN_PROGRESS', 'DELAYED'],
    hold: ['ON_HOLD'],
    done: ['DONE', 'CANCELED']
};

const TAB_LABELS = {
    active: '진행 중',
    hold: '보류',
    done: '완료'
};

let currentTabKey = 'active'; // 현재 선택된 상태 탭
let allBoards = [];           // API에서 조회한 전체 게시판 목록

document.addEventListener('DOMContentLoaded', () => {
    loadBoards();

    document.getElementById('searchKeyword').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') loadList(1);
    });
});

/* 프로젝트 게시판 목록 조회 처리 */
async function loadBoards() {
    const accessToken = localStorage.getItem('accessToken');
    try {
        const res = await fetch('/api/board/project/boards', {
            headers: {'Authorization': `Bearer ${accessToken}`}
        });
        if (!res.ok) return;

        allBoards = await res.json();
        renderStatusTabs();
        applyTab('active');
    } catch (e) {
        // 오류 추적용 콘솔 기록 유지
        console.error('게시판 목록 조회 실패:', e);
    }
}

/* 프로젝트 상태 탭 렌더링 처리 */
function renderStatusTabs() {
    const container = document.getElementById('statusTabs');
    const counts = {};
    Object.keys(STATUS_GROUPS).forEach(key => {
        counts[key] = getBoardsByTab(key).length;
    });

    container.innerHTML = Object.keys(STATUS_GROUPS).map(key => `
        <button class="tab-btn ${key === 'active' ? 'active' : ''}"
                id="tab-${key}"
                onclick="applyTab('${key}')">
            ${TAB_LABELS[key]}
            <span class="tab-count">${counts[key]}</span>
        </button>
    `).join('');
}

/* 상태 탭 기준 게시판 목록 반환 처리 */
function getBoardsByTab(tabKey) {
    const statuses = STATUS_GROUPS[tabKey];
    return allBoards.filter(b => statuses.includes(b.projectstatus));
}

/* 프로젝트 상태 탭 선택 처리 */
function applyTab(tabKey) {
    currentTabKey = tabKey;

    // 탭 active 클래스 갱신
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    const tabEl = document.getElementById('tab-' + tabKey);
    if (tabEl) tabEl.classList.add('active');

    // 완료 탭에서는 글쓰기 버튼 숨김 처리
    const writeBtn = document.querySelector('.wb');
    if (writeBtn) writeBtn.style.display = tabKey === 'done' ? 'none' : '';

    const filtered = getBoardsByTab(tabKey);
    renderDropdown(filtered);

    if (filtered.length > 0) {
        // 첫 번째 프로젝트 자동 선택 처리
        selectProject(filtered[0].boardid, filtered[0].projectid);
        document.getElementById('projectDropdown').value = String(filtered[0].boardid);
    } else {
        selectedBoardId = null;
        selectedProjectId = null;
        document.getElementById('projectDropdown').value = '';
        renderEmpty('해당 상태의 프로젝트가 없습니다.');
        renderPagination({prev: false, next: false, start: 1, end: 1, page: 1});
    }
}

/* 프로젝트 드롭다운 렌더링 처리 */
function renderDropdown(boards) {
    const select = document.getElementById('projectDropdown');
    if (boards.length === 0) {
        select.innerHTML = '<option value="">프로젝트가 없습니다.</option>';
        select.disabled = true;
        return;
    }
    select.disabled = false;
    select.innerHTML = boards.map(b =>
        `<option value="${b.boardid}">${escapeHtml(b.projectname)}</option>`
    ).join('');
}

/* 프로젝트 드롭다운 변경 처리 */
function onDropdownChange(select) {
    const boardId = select.value ? Number(select.value) : null;
    const board = allBoards.find(b => String(b.boardid) === String(select.value));
    const projectId = board ? board.projectid : null;
    selectProject(boardId, projectId);
}

/* 프로젝트 선택 후 목록 조회 처리 */
function selectProject(boardId, projectId) {
    selectedBoardId = boardId;
    selectedProjectId = projectId;
    loadList(1);
}

/* 프로젝트 공지 목록 조회 처리 */
async function loadList(page) {
    currentPage = page;

    const type = document.getElementById('searchType').value;
    const keyword = document.getElementById('searchKeyword').value.trim();

    const params = new URLSearchParams({
        page,
        size: PAGE_SIZE,
        type,
        ...(keyword && {keyword}),
        ...(selectedProjectId && {projectId: selectedProjectId})
    });

    const accessToken = localStorage.getItem('accessToken');
    try {
        const res = await fetch(`/api/board/project/notices?${params}`, {
            headers: {'Authorization': `Bearer ${accessToken}`}
        });
        if (res.status === 401) {
            location.href = '/login';
            return;
        }
        if (!res.ok) throw new Error('서버 오류');

        const data = await res.json();
        renderTable(data.dtoList);
        renderPagination(data);
    } catch (e) {
        // 오류 추적용 콘솔 기록 유지
        console.error('프로젝트 공지 목록 조회 실패:', e);
        renderEmpty('목록을 불러오지 못했습니다.');
    }
}

/* 프로젝트 공지 테이블 렌더링 처리 */
function renderTable(list) {
    const tbody = document.getElementById('projectNoticeBody');
    if (!list || list.length === 0) {
        renderEmpty('게시글이 없습니다.');
        return;
    }
    tbody.innerHTML = list.map(dto => {
        const date = dto.postedAt ? dto.postedAt.substring(0, 10).replaceAll('-', '.') : '';
        const pinBadge = dto.pinned ? `<span class="lbl lp" style="margin-right:4px;">공지</span>` : '';
        const clipIcon = dto.attachmentCount > 0 ? `<i data-lucide="paperclip" style="width:14px;height:14px;color:#999;"></i>` : '';
        const commentBadge = dto.commentCount > 0 ? `<span class="tc">[${dto.commentCount}]</span>` : '';
        return `
            <tr onclick="location.href='/board/project/notice/${dto.id}'">
                <td style="max-width:160px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;"
                    title="${escapeHtml(dto.projectName ?? '')}">
                    ${pinBadge}${escapeHtml(dto.projectName ?? '')}
                </td>
                <td>
                    <div class="ti">
                        <span class="tt">${escapeHtml(dto.title)}</span>
                        ${clipIcon}${commentBadge}
                    </div>
                </td>
                <td class="ac">${escapeHtml(dto.authorName)}</td>
                <td class="mu">${date}</td>
                <td class="nr">${dto.viewCount}</td>
            </tr>`;
    }).join('');
    lucide.createIcons();
}

/* 프로젝트 공지 빈 상태 표시 처리 */
function renderEmpty(msg) {
    document.getElementById('projectNoticeBody').innerHTML =
        `<tr><td colspan="5" class="empty">${msg}</td></tr>`;
}

/* 프로젝트 공지 페이지네이션 렌더링 처리 */
function renderPagination(data) {
    const pgr = document.getElementById('pagination');
    let html = '';
    // total=0이면 end=0이 오므로 최소 1페이지 표시
    const end = data.end > 0 ? data.end : 1;
    const start = data.start > 0 ? data.start : 1;
    if (data.prev) html += `<a class="pg" onclick="loadList(${start - 1})">‹</a>`;
    for (let i = start; i <= end; i++) {
        html += `<a class="pg${i === data.page ? ' on' : ''}" onclick="loadList(${i})">${i}</a>`;
    }
    if (data.next) html += `<a class="pg" onclick="loadList(${end + 1})">›</a>`;
    pgr.innerHTML = html;
}

/* 프로젝트 공지 글쓰기 권한 확인 후 이동 처리 */
function goToProjectNoticeWrite() {
    const accessToken = localStorage.getItem('accessToken');
    fetch('/api/board/notice/write-check', {
        headers: {'Authorization': 'Bearer ' + accessToken}
    }).then(res => {
        if (res.ok) {
            // 선택된 boardId를 쿼리 파라미터로 전달해 작성 화면에서 드롭다운 자동 선택
            const url = selectedBoardId
                ? `/board/project/notice/write?boardId=${selectedBoardId}`
                : '/board/project/notice/write';
            window.location.href = url;
        } else {
            document.getElementById('noAuthModal').style.display = 'flex';
        }
    });
}

/* HTML 특수 문자 이스케이프 처리 */
function escapeHtml(str) {
    return str.replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}