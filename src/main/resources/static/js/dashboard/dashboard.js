/* 전역 변수 설정 */
let currentEmpNo = ''; // 현재 로그인한 사원 번호
let allTasks = []; // 배정된 모든 업무 리스트 (페이징용)
let currentPage = 1; // 업무 리스트 현재 페이지
const pageSize = 5; // 한 페이지당 노출 업무 수

/* 초기 로드 함수 */
document.addEventListener('DOMContentLoaded', () => {
    loadCurrentUser(); // 사용자 정보 확인 및 데이터 호출
    lucide.createIcons(); // 아이콘 렌더링

    // 30초 간격으로 서버에 읽지 않은 새 알림이 있는지 확인 (Polling)
    setInterval(() => {
        if (currentEmpNo) {
            checkNewNotifications();
        }
    }, 30000);
});

/* 현재 로그인 사용자 정보 조회 및 세션 유효성 검사 */
async function loadCurrentUser() {
    const accessToken = localStorage.getItem('accessToken');
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        const response = await fetch('/api/auth/me', {
            headers: {'Authorization': 'Bearer ' + accessToken}
        });
        const data = await response.json();

        if (!response.ok) {
            clearTokens();
            window.location.href = '/login';
            return;
        }
        if (data.tempPasswordRequired) {
            window.location.href = '/password-change';
            return;
        }

        currentEmpNo = data.employeeNo;
        document.getElementById('welcome-name').innerText = data.name;
        fetchDashboardData(); // 사용자 확인 후 대시보드 데이터 호출
    } catch (error) {
        clearTokens();
        window.location.href = '/login';
    }
}

/* 대시보드 전체 데이터 호출 */
async function fetchDashboardData() {
    fetchStats(); // 상단 숫자 통계
    fetchMyTasks(); // 내 업무 리스트
    fetchNotices(); // 공지 사항 5개 로드
    checkNewNotifications(); // 최초 로드 시 알림 확인
}

/*
공지사항 최신 5개 조회 - API 호출 후 렌더링 함수에 데이터를 전달함.
서버가 최신순으로 반환하므로 새 공지 등록 시 가장 오래된 항목은 자동으로 밀려남
*/
const NOTICE_MAX = 5;

async function fetchNotices() {
    const tbody = document.getElementById('notice-list');
    try {
        const res = await axios.get('/api/board/notices/recent', {
            params: {size: NOTICE_MAX},
            headers: {'Authorization': 'Bearer ' + localStorage.getItem('accessToken')}
        });

        // 서버 응답이 NOTICE_MAX를 초과하더라도 최대 5개만 렌더링
        const notices = (res.data || []).slice(0, NOTICE_MAX);
        renderNotices(notices);
    } catch (e) {
        console.error("공지사항 로드 실패", e);
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#DE350B; padding:20px 0;">공지사항을 불러오지 못했습니다.</td></tr>';
    }
}

/*
공지사항 목록 렌더링 - 데이터를 테이블 행으로 변환해 DOM에 삽입함.
첨부파일 여부는 paperclip 아이콘으로, 댓글 수는 [n] 형태로 표시하며,
렌더링 후 lucide.createIcons()를 호출해 동적으로 추가된 아이콘을 활성화함
*/
function renderNotices(notices) {
    const tbody = document.getElementById('notice-list');

    // 공지사항이 없으면 안내 메시지 표시 후 종료
    if (!notices || notices.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; color:#6B778C; padding:20px 0;">등록된 공지사항이 없습니다.</td></tr>';
        return;
    }

    tbody.innerHTML = notices.map(n => {
        const date = n.postedAt ? n.postedAt.substring(0, 10) : '-';

        // 첨부파일이 있는 경우 paperclip 아이콘 노출
        const attachment = n.attachmentCount > 0
            ? `<i data-lucide="paperclip" style="width:13px; height:13px; color:#999; flex-shrink:0;"></i>`
            : '';

        // 댓글이 있는 경우 댓글 수를 [n] 형태로 노출
        const comment = n.commentCount > 0
            ? `<span style="font-size:12px; color:#0052CC; font-weight:600; flex-shrink:0;">[${n.commentCount}]</span>`
            : '';

        return `<tr>
                    <td style="padding-left:10px; padding-right:4px;">
                        <span style="display:inline-block; font-size:12px; font-weight:600; color:#be123c; background:#fff1f2; border:1px solid #fecdd3; border-radius:5px; padding:3px 10px; white-space:nowrap;">공지</span>
                    </td>
                    <td>
                        <div style="display:flex; align-items:center; gap:6px; min-width:0;">
                            <a href="/board/notice/${n.id}"
                               style="color:#172B4D; text-decoration:none; font-weight:500; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;"
                               onmouseover="this.style.color='#0052CC'"
                               onmouseout="this.style.color='#172B4D'">
                                ${n.title}
                            </a>
                            ${attachment}
                            ${comment}
                        </div>
                    </td>
                    <td style="color:#6B778C; white-space:nowrap;">${n.authorName || '-'}</td>
                    <td style="color:#6B778C; white-space:nowrap;">${date}</td>
                </tr>`;
    }).join('');

    // 동적으로 삽입된 lucide 아이콘 활성화
    lucide.createIcons();
}


/* 미확인 알림 확인 및 모달 노출 */
async function checkNewNotifications() {
    if (!currentEmpNo) return;

    try {
        const res = await axios.get(`/api/notifications/unread?empNo=${currentEmpNo}`, {
            headers: {'Authorization': 'Bearer ' + localStorage.getItem('accessToken')}
        });

        // 데이터가 있을 경우 알림 모달 구성
        if (res.status === 200 && res.data && res.data.id) {
            const noti = res.data;
            const modalElement = document.getElementById('notificationModal');
            const titleEl = document.getElementById('noti-title');
            const msgEl = document.getElementById('noti-message');

            const modalHeader = modalElement.querySelector('.modal-header');
            const modalIconContainer = modalElement.querySelector('.modal-body .bg-light');

            // 알림 유형(프로젝트/업무)에 따라 아이콘 및 제목 동적 변경
            if (noti.refType === 'PROJECT') {
                modalHeader.innerHTML = `<h5 class="modal-title fw-bold"><i data-lucide="folder-plus" class="me-2"></i>새 프로젝트 초대</h5><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>`;
                modalIconContainer.innerHTML = `<i data-lucide="briefcase" class="text-primary" style="width: 40px; height: 40px;"></i>`;
            } else {
                modalHeader.innerHTML = `<h5 class="modal-title fw-bold"><i data-lucide="megaphone" class="me-2"></i>새 업무 배정 알림</h5><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>`;
                modalIconContainer.innerHTML = `<i data-lucide="clipboard-check" class="text-primary" style="width: 40px; height: 40px;"></i>`;
            }

            titleEl.innerText = noti.title;
            msgEl.innerText = noti.message;

            const notiModal = new bootstrap.Modal(modalElement);
            lucide.createIcons();
            notiModal.show();

            // 모달 내 확인 버튼 클릭 시 읽음 처리 수행
            document.getElementById('confirmNotiBtn').onclick = async () => {
                try {
                    // 1. 서버에 읽음 상태 전송 (PATCH)
                    await axios.patch(`/api/notifications/${noti.id}/read`, {}, {
                        headers: {'Authorization': 'Bearer ' + localStorage.getItem('accessToken')}
                    });

                    // 2. 모달 닫기
                    notiModal.hide();

                    // 3. 업무 배정일 경우 리스트 즉시 갱신
                    if (noti.refType === 'PROJECT') {
                        // 프로젝트 초대 알림은 단순히 닫기만 수행
                        console.log("프로젝트 초대 확인 완료");
                    } else {
                        // PROJECT_TASK(업무 배정) 알림일 때
                        // 질문 없이 바로 내 업무 리스트를 갱신합니다.
                        fetchMyTasks();
                    }

                    // 4. 통계 수치 갱신
                    fetchStats();

                } catch (e) {
                    console.error("알림 읽음 처리 실패", e);
                }
            };
        }
    } catch (e) {
        if (e.response && e.response.status !== 204) {
            console.error("알림 확인 중 오류 발생", e);
        }
    }
}

/* 대시보드 상단 통계 수치(진행중, 마감임박, 지연) 조회 */
async function fetchStats() {
    try {
        const res = await axios.get(`/api/dashboard/stats?empNo=${currentEmpNo}`, {
            headers: {'Authorization': 'Bearer ' + localStorage.getItem('accessToken')}
        });
        document.getElementById('stat-in-progress').innerText = res.data.inProgress || 0;
        document.getElementById('stat-due-today').innerText = res.data.dueToday || 0;
        document.getElementById('stat-overdue').innerText = res.data.overdue || 0;
    } catch (e) {
        console.error("통계 로드 실패", e);
    }
}

/* 로그인한 사용자에게 배정된 프로젝트 업무 목록 조회 */
async function fetchMyTasks() {
    try {
        const res = await axios.get(`/api/projects/tasks/assigned`, {
            params: {empNo: currentEmpNo},
            headers: {'Authorization': 'Bearer ' + localStorage.getItem('accessToken')}
        });
        allTasks = res.data;
        currentPage = 1;
        renderTasks(); // 조회 성공 시 화면 렌더링
    } catch (e) {
        console.error("업무 로드 실패", e);
        document.getElementById('assigned-task-list').innerHTML = '<p style="color:#DE350B; font-size:14px;">업무를 불러오지 못했습니다.</p>';
    }
}

/* 업무 목록 HTML 렌더링 (페이징 포함) */
function renderTasks() {
    const container = document.getElementById('assigned-task-list');
    if (!allTasks || allTasks.length === 0) {
        container.innerHTML = '<p style="color:#6B778C; font-size:14px;">배당된 업무가 없습니다.</p>';
        return;
    }
    const now = new Date();
    const offset = now.getTimezoneOffset() * 60000; // 분 단위를 밀리초로 변환
    const today = new Date(now.getTime() - offset).toISOString().split('T')[0];

    const totalPages = Math.ceil(allTasks.length / pageSize);
    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    const pagedTasks = allTasks.slice(start, end);

    let html = pagedTasks.map(task => {
        let statusClass = '';
        // 완료되지 않은 업무 중 마감 기한에 따른 스타일 클래스 지정
        if (task.status !== 'DONE') {
            // 날짜 비교 로직
            if (task.dueOn === today) {
                statusClass = 'due-today'; // 오늘 마감 (노란색)
            } else if (task.dueOn && task.dueOn < today) {
                statusClass = 'overdue';   // 지연됨 (빨간색)
            }
        }

        return `
                <div class="task-item ${statusClass}">
                    <div style="display:flex; justify-content:space-between; align-items:start; margin-bottom: 8px;">
                        <span style="font-weight:600; font-size:14px;">
                            <span style="color: #0052CC; margin-right: 4px;">[${task.projectName || '미지정'}]</span>
                            ${task.title}
                        </span>
                        <span class="badge ${task.status === 'DONE' ? 'bg-success' : 'bg-primary'}" style="font-size:10px;">${task.status}</span>
                    </div>
                    <div class="progress-wrapper">
                        <div style="display:flex; justify-content:space-between; align-items: center; font-size:13px; color:#6B778C;">
                            <div style="display: flex; align-items: center; gap: 5px;">
                                <span>진척도:</span>
                                <input type="number" id="input-${task.id}" class="form-control form-control-sm"
                                       style="width: 50px; height: 26px; font-size: 11px;"
                                       min="0" max="100" value="${task.progressRate}"
                                       onkeydown="if(event.key==='Enter') updateProgress(${task.id}, this.value)">
                                <span>%</span>
                            </div>
                            <button class="btn btn-outline-secondary btn-sm" style="font-size: 10px; padding: 1px 6px;"
                                    onclick="updateProgress(${task.id}, document.getElementById('input-${task.id}').value)">수정</button>
                        </div>
                        <div style="font-size:11px; font-weight: ${statusClass ? 'bold' : 'normal'}; color:${statusClass === 'overdue' ? '#DE350B' : '#6B778C'}; margin-top: 5px;">
                            마감: ${task.dueOn || '미정'}
                        </div>
                    </div>
                </div>`;
    }).join('');

    // 페이지 버튼 추가
    if (totalPages > 1) {
        html += `<div class="pagination-container">
                    <button class="page-btn" onclick="changePage(-1)" ${currentPage === 1 ? 'disabled' : ''}>이전</button>
                    <span class="page-info">${currentPage} / ${totalPages}</span>
                    <button class="page-btn" onclick="changePage(1)" ${currentPage === totalPages ? 'disabled' : ''}>다음</button>
                </div>`;
    }
    container.innerHTML = html;
}

/*
 * 업무 목록 페이지 이동
 */
function changePage(direction) {
    currentPage += direction;
    renderTasks();
}

/*
 * 업무 진척도 업데이트 (입력 필드 또는 엔터키)
 */
async function updateProgress(taskId, progress) {
    try {
        const progressRate = parseInt(progress);
        let newStatus = 'IN_PROGRESS'; // 기본 상태는 진행 중

        // 진척도에 따른 상태값 세분화
        if (progressRate === 100) {
            newStatus = 'DONE'; // 완료
        } else if (progressRate === 0) {
            newStatus = 'TODO'; // 시작 전 (※ 백엔드에 설정된 '대기/시작 전' 상태 코드로 변경해주세요. 예: 'TODO', 'WAITING', 'NOT_STARTED' 등)
        }

        await axios.patch(`/api/dashboard/tasks/${taskId}`,
            {
                progressRate: progressRate,
                status: newStatus
            },
            { headers: {'Authorization': 'Bearer ' + localStorage.getItem('accessToken')} }
        );

        // 상태와 진척도가 변경되었으므로 통계와 리스트를 모두 새로고침하는 것이 좋습니다.
        fetchStats();
        fetchMyTasks();
    } catch (e) {
        alert("수정에 실패했습니다.");
        fetchMyTasks();
    }
}

/*
 * 로그아웃 시 로컬 스토리지 데이터 삭제
 */
function clearTokens() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('employeeNo');
    localStorage.removeItem('employeeName');
    localStorage.removeItem('role');
}