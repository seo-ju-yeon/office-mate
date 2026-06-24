/**
 * 프로젝트 관리 시스템 (작성자: 강수현)
 * 프로젝트 생성/삭제/조회, 팀원 초대, 업무 할당, 상태 관리 처리
 */

/* Axios JWT 인증 헤더 자동 삽입 처리 */
axios.interceptors.request.use(function (config) {
    // Spring Security 적용 후 API 인증 누락 방지를 위해 모든 Axios 요청에 Bearer 토큰 추가
    const accessToken = localStorage.getItem('accessToken') || localStorage.getItem('token');
    if (accessToken) {
        config.headers.Authorization = accessToken.startsWith('Bearer ') ? accessToken : 'Bearer ' + accessToken;
    }
    return config;
});

/* 프로젝트 관리 페이지 접근 권한 확인 처리 */
const currentRole = localStorage.getItem('role');
if (currentRole !== 'SUPER' && currentRole !== 'ADMIN') {
    alert("해당 페이지에 접근할 수 있는 권한이 없습니다.");
    window.location.href = '/dashboard'; // 권한이 없으면 대시보드로 이동
}

/* 프로젝트 화면 전역 상태 */
let allProjects = []; // 서버에서 조회한 전체 프로젝트 목록
let currentTab = 'ACTIVE'; // 현재 선택된 프로젝트 탭
let currentProjectId = null; // 상세 모달에서 관리 중인 프로젝트 ID
let selectedEmployeeNo = null; // 상세 모달에서 선택된 팀원 사번
let detailModal = null; // 상세 관리 모달 인스턴스
let createProjectModal = null; // 프로젝트 생성 모달 인스턴스
let memberScheduleModal = null; // 팀원 일정 확인 모달 인스턴스
let eventDetailModal = null;    // 일정 상세 확인 모달 인스턴스

// 프로젝트 전체 일정 모달과 달력 상태
let projectScheduleModal = null;
let projectFullCalendar = null;

let memberCalendar = null; // 팀원 일정 FullCalendar 인스턴스
let currentMemberTasks = []; // 선택 팀원 업무 목록
let currentTaskTab = 'ALL'; // 상세 모달 업무 필터 탭

/* 프로젝트 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', () => {
    fetchDashboardData(); // 프로젝트 목록 조회
    filterEmployees(); // 초대용 직원 목록 초기 조회

    // Bootstrap 모달 객체 초기화
    detailModal = new bootstrap.Modal(document.getElementById('projectDetailModal'));
    createProjectModal = new bootstrap.Modal(document.getElementById('createProjectModal'));
    memberScheduleModal = new bootstrap.Modal(document.getElementById('memberScheduleModal'));

    // 일정 상세 모달 인스턴스 연결
    const eventDetailModalEl = document.getElementById('calendarEventDetailModal');
    if (eventDetailModalEl) {
        eventDetailModal = new bootstrap.Modal(eventDetailModalEl);
    }

    // 프로젝트 전체 일정 모달 인스턴스 연결
    const projectScheduleModalEl = document.getElementById('projectScheduleModal');
    if (projectScheduleModalEl) {
        projectScheduleModal = new bootstrap.Modal(projectScheduleModalEl);
    }

    // Lucide 아이콘 렌더링
    if (typeof lucide !== 'undefined' && typeof lucide.createIcons === 'function') {
        lucide.createIcons();
    }

    // 시작일/마감일은 오늘 이전 날짜 선택 방지
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('newProjectStart').setAttribute('min', today);
    document.getElementById('newProjectEnd').setAttribute('min', today);
    document.getElementById('taskDueDateInput').setAttribute('min', today);

    // 프로젝트 시작일 변경 시 종료일 최소 날짜 동기화
    document.getElementById('newProjectStart').addEventListener('change', function() {
        document.getElementById('newProjectEnd').setAttribute('min', this.value);
    });

    // 일정 모달이 열린 뒤 FullCalendar 크기 보정 처리
    document.getElementById('memberScheduleModal').addEventListener('shown.bs.modal', function () {
        setTimeout(() => {
            if (memberCalendar) {
                memberCalendar.updateSize();
            }
        }, 50);
    });

    // 프로젝트 전체 일정 모달이 열린 뒤 FullCalendar 크기 보정 처리
    document.getElementById('projectScheduleModal').addEventListener('shown.bs.modal', function () {
        setTimeout(() => {
            if (projectFullCalendar) {
                projectFullCalendar.updateSize();
            }
        }, 50);
    });
});

/* 프로젝트 전체 목록 조회 처리 */
async function fetchDashboardData() {
    const container = document.getElementById('projectContainer');
    try {
        const response = await axios.get('/api/projects/dashboard');
        allProjects = response.data;

        const updated = await checkAndAutoCloseProjects();
        if (updated) return;

        updateTabCounts();
        filterByTab(currentTab);
    } catch (e) {
        if (container) {
            container.innerHTML = '<div class="p-5 text-center text-danger fw-bold">데이터 로드 실패 (인증 권한 및 서버 상태를 확인하세요)</div>';
        }
        console.error(e);
    }
}

/* 진척도 100% 활성 프로젝트 자동 완료 처리 */
async function checkAndAutoCloseProjects() {
    const targetProjects = allProjects.filter(p => {
        const statusUpper = p.status ? p.status.trim().toUpperCase() : 'READY';
        const isActive = ['READY', 'IN_PROGRESS', 'DELAYED'].includes(statusUpper);
        const isProgressFull = parseInt(p.progressRate || 0) === 100;
        return isActive && isProgressFull;
    });

    if (targetProjects.length === 0) return false;

    for (const project of targetProjects) {
        try {
            await axios.patch(`/api/projects/${project.id}/status`, { status: 'DONE' });
            console.log(`[시스템] 프로젝트 '${project.name}'의 모든 업무가 완료되어 자동으로 완료 처리되었습니다.`);
        } catch (error) {
            console.error(`프로젝트 자동 완료 처리 중 오류 발생 (ID: ${project.id})`, error);
        }
    }

    if (detailModal && document.getElementById('projectDetailModal').classList.contains('show')) {
        if (targetProjects.some(p => p.id === currentProjectId)) {
            alert("해당 프로젝트의 모든 업무가 완료되어 '완료 탭'으로 자동 이동합니다.");
            detailModal.hide();
        }
    } else {
        alert("모든 업무가 완료된 프로젝트가 있어 '완료 탭'으로 자동으로 이동 처리되었습니다.");
    }

    currentTab = 'FINISHED';
    fetchDashboardData();
    return true;
}

/* 프로젝트 상태 탭 개수 배지 갱신 처리 */
function updateTabCounts() {
    const counts = {
        active: allProjects.filter(p => p.status && ['READY', 'IN_PROGRESS', 'DELAYED'].includes(p.status.trim().toUpperCase())).length,
        hold: allProjects.filter(p => p.status && p.status.trim().toUpperCase() === 'ON_HOLD').length,
        finished: allProjects.filter(p => p.status && p.status.trim().toUpperCase() === 'DONE').length
    };

    const activeEl = document.getElementById('count-active');
    const holdEl = document.getElementById('count-hold');
    const finishedEl = document.getElementById('count-finished');

    if (activeEl) activeEl.innerText = counts.active;
    if (holdEl) holdEl.innerText = counts.hold;
    if (finishedEl) finishedEl.innerText = counts.finished;
}

/* 프로젝트 상태 탭 필터링 처리 */
function filterByTab(tabType) {
    currentTab = tabType;

    document.querySelectorAll('#projectStatusTabs .nav-link').forEach((btn, idx) => {
        btn.classList.remove('active');
        if((tabType === 'ACTIVE' && idx === 0) ||
            (tabType === 'ON_HOLD' && idx === 1) ||
            (tabType === 'FINISHED' && idx === 2)) {
            btn.classList.add('active');
        }
    });

    const filtered = allProjects.filter(p => {
        if (!p.status) return false;
        const statusUpper = p.status.trim().toUpperCase();

        if (tabType === 'ACTIVE') return ['READY', 'IN_PROGRESS', 'DELAYED'].includes(statusUpper);
        if (tabType === 'ON_HOLD') return statusUpper === 'ON_HOLD';
        if (tabType === 'FINISHED') return statusUpper === 'DONE';
        return false;
    });

    renderProjectCards(filtered);
}

/* 프로젝트 카드 렌더링 처리 */
function renderProjectCards(projects) {
    const container = document.getElementById('projectContainer');
    if (!container) return;

    container.innerHTML = '';

    if (!projects || projects.length === 0) {
        container.innerHTML = '<div class="text-center py-5 text-muted w-100">해당하는 프로젝트가 없습니다.</div>';
        return;
    }

    projects.forEach(p => {
        const statusUpper = p.status ? p.status.trim().toUpperCase() : 'READY';
        const displayStart = p.startOn || p.startsOn || '';
        const displayEnd = p.endOn || p.endsOn || '';

        container.insertAdjacentHTML('beforeend', `
            <div class="project-card" style="position: relative;">
                <button class="btn btn-link text-danger p-0 position-absolute"
                        style="top: 15px; right: 15px; border: none; background: none;"
                        onclick="deleteProject(${p.id})">
                    <i data-lucide="trash-2" style="width:18px;"></i>
                </button>
                <div>
                    <div class="d-flex justify-content-between align-items-start mb-2 pe-4">
                        <h5 class="fw-bold mb-0">${p.name}</h5>
                    </div>
                    <span class="status-badge ${getStatusBadgeClass(statusUpper)}">${statusUpper}</span>

                    <div class="info-item mt-3 mb-1">
                        <i data-lucide="user" style="width:14px;"></i>
                        <span>담당자: ${p.managerName || '미지정'}</span>
                    </div>

                    <div class="text-muted mt-1 mb-2" style="font-size: 0.75rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; text-overflow: ellipsis; line-height: 1.4; min-height: 2.1em;">
                        <i data-lucide="align-left" style="width:12px; margin-right: 4px; margin-bottom: 2px;"></i>
                        ${p.description || '프로젝트 설명이 없습니다.'}
                    </div>

                    <div class="mt-1">
                        <div class="d-flex justify-content-between small">
                            <span class="text-muted">진행 정도</span>
                            <span class="fw-bold">${p.progressRate || 0}%</span>
                        </div>
                        <div class="progress-bar-bg"><div class="progress-fill" style="width: ${p.progressRate || 0}%"></div></div>
                    </div>
                    <div class="info-item mt-3 small">
                        <i data-lucide="calendar" style="width:14px;"></i>
                        <span>${displayStart} ~ ${displayEnd}</span>
                    </div>
                </div>
                <button class="btn-detail" onclick="openProjectDetail(${p.id}, '${p.name}', '${statusUpper}')">인원 및 상세 관리</button>
            </div>
        `);
    });

    if (typeof lucide !== 'undefined' && typeof lucide.createIcons === 'function') {
        lucide.createIcons();
    }
}

/* 프로젝트 상태 배지 클래스 변환 처리 */
function getStatusBadgeClass(status) {
    switch(status) {
        case 'IN_PROGRESS': return 'bg-primary-subtle text-primary';
        case 'DELAYED': return 'bg-danger-subtle text-danger';
        case 'DONE': return 'bg-success-subtle text-success';
        case 'ON_HOLD': return 'bg-warning-subtle text-warning';
        default: return 'bg-secondary-subtle text-secondary';
    }
}

/* 부서별 관리자 후보 조회 처리 */
async function filterManagerEmployees() {
    const dept = document.getElementById('newProjectDeptFilter').value;
    const select = document.getElementById('newProjectManagerSelect');
    if (!select) return;
    try {
        const response = await axios.get('/api/projects/employees', {params: {department: dept}});
        select.innerHTML = '<option value="">관리자를 선택하세요</option>';
        response.data.forEach(emp => {
            select.insertAdjacentHTML('beforeend',
                `<option value="${emp.employeeNo}">${emp.employeeName} (${emp.employeeNo})</option>`
            );
        });
    } catch (e) {
        console.error("관리자 목록 로드 실패");
    }
}

/* 프로젝트 생성 모달 표시 처리 */
function openCreateProjectModal() {
    document.getElementById('newProjectDeptFilter').value = '';
    filterManagerEmployees();
    createProjectModal.show();
}

/* 새 프로젝트 생성 요청 처리 */
async function createProject() {
    const name = document.getElementById('newProjectName').value;
    const startsOn = document.getElementById('newProjectStart').value;
    const endsOn = document.getElementById('newProjectEnd').value;
    const description = document.getElementById('newProjectDescription').value;
    const managerNo = document.getElementById('newProjectManagerSelect').value;
    const ownerDepartment = document.getElementById('newProjectDeptFilter').value;

    const today = new Date().toISOString().split('T')[0];

    if (!startsOn || !endsOn) return alert("시작일과 종료일을 입력해주세요.");
    if (startsOn < today) return alert("시작일은 오늘 또는 오늘 이후 날짜여야 합니다.");
    if (endsOn < startsOn) return alert("종료일은 시작일보다 빠를 수 없습니다.");
    if (!name.trim() || !managerNo || !ownerDepartment) return alert("프로젝트명, 부서, 관리자는 필수 입력 사항입니다.");

    try {
        const sendData = {
            name: name,
            description: description,
            startsOn: startsOn,
            endsOn: endsOn,
            managerNo: managerNo,
            ownerDepartment: ownerDepartment
        };

        await axios.post('/api/projects', sendData);
        createProjectModal.hide();

        document.getElementById('newProjectName').value = '';
        document.getElementById('newProjectDescription').value = '';
        document.getElementById('newProjectStart').value = '';
        document.getElementById('newProjectEnd').value = '';

        fetchDashboardData();
        alert("프로젝트가 생성되었습니다.");
    } catch (e) {
        alert("생성 실패: 입력 데이터를 확인하세요.");
    }
}

/* 프로젝트 취소 요청 처리 */
async function deleteProject(projectId) {
    if (!confirm("프로젝트를 취소하시겠습니까? 취소된 프로젝트는 대시보드에서 보이지 않게 됩니다.")) return;
    try {
        await axios.delete(`/api/projects/${projectId}`);
        fetchDashboardData();
        alert("프로젝트가 취소되었습니다.");
    } catch (e) {
        alert("프로젝트 취소 실패");
    }
}

/* 팀원 초대용 직원 목록 필터링 처리 */
async function filterEmployees() {
    const dept = document.getElementById('deptFilter').value;
    const select = document.getElementById('employeeSelect');
    if (!select) return;
    try {
        const response = await axios.get('/api/projects/employees', {params: {department: dept}});
        select.innerHTML = '<option value="">직원을 선택하세요</option>';
        response.data.forEach(emp => {
            select.insertAdjacentHTML('beforeend', `<option value="${emp.employeeNo}">${emp.employeeName} (${emp.employeeNo})</option>`);
        });
    } catch (e) {
        console.error("직원 로드 실패");
    }
}

/* 프로젝트 상세 관리 모달 초기화 처리 */
async function openProjectDetail(id, name, currentStatus) {
    currentProjectId = id;
    selectedEmployeeNo = null;
    document.getElementById('modalProjectName').innerText = name;

    if (currentStatus) {
        document.getElementById('modalProjectStatus').value = currentStatus.trim().toUpperCase();
    }

    const currentProject = allProjects.find(p => p.id === id);
    const dateInput = document.getElementById('taskDueDateInput');

    if (currentProject) {
        const projectStart = currentProject.startOn || currentProject.startsOn;
        if (projectStart) {
            const today = new Date().toISOString().split('T')[0];
            const minDate = projectStart > today ? projectStart : today;
            dateInput.setAttribute('min', minDate);
        }

        const projectEnd = currentProject.endOn || currentProject.endsOn;
        if (projectEnd) {
            dateInput.setAttribute('max', projectEnd);
        } else {
            dateInput.removeAttribute('max');
        }
    }

    document.getElementById('taskManagementSection').style.display = 'none';
    document.getElementById('taskPlaceholder').style.display = 'flex';

    await fetchMemberList();
    detailModal.show();
}

/* 프로젝트 진행 상태 변경 요청 처리 */
async function updateProjectStatus() {
    const newStatus = document.getElementById('modalProjectStatus').value;

    try {
        if (newStatus === 'DONE') {
            const taskResponse = await axios.get(`/api/projects/tasks/${currentProjectId}`);
            const allTasks = taskResponse.data;

            const hasIncompleteTask = allTasks.some(task => {
                const progress = parseInt(task.progressRate || 0);
                const status = task.status ? task.status.trim().toUpperCase() : 'TODO';
                return progress < 100 || status !== 'DONE';
            });

            if (hasIncompleteTask) {
                alert("아직 완료되지 않은 업무가 남아있어 프로젝트를 완료 상태로 변경할 수 없습니다.\n모든 업무의 진행률을 100%로 완료해 주세요.");
                const currentProject = allProjects.find(p => p.id === currentProjectId);
                if (currentProject && currentProject.status) {
                    document.getElementById('modalProjectStatus').value = currentProject.status.trim().toUpperCase();
                } else {
                    document.getElementById('modalProjectStatus').value = 'IN_PROGRESS';
                }
                return;
            }
        }

        await axios.patch(`/api/projects/${currentProjectId}/status`, { status: newStatus });

        if (newStatus === 'DONE') {
            alert("프로젝트가 완료 처리되어 완료 탭으로 이동합니다.");
            detailModal.hide();
            currentTab = 'FINISHED';
        } else {
            alert("상태가 변경되었습니다.");
        }

        fetchDashboardData();
    } catch (e) {
        alert("상태 변경에 실패했습니다.");
    }
}

/* 프로젝트 참여 멤버 목록 조회 처리 */
async function fetchMemberList() {
    const container = document.getElementById('memberListContainer');
    const countBadge = document.getElementById('memberCount');

    try {
        const response = await axios.get(`/api/projects/${currentProjectId}/members`);
        const members = response.data;

        if (countBadge) countBadge.innerText = members.length;
        container.innerHTML = members.length ? '' : '<div class="p-4 text-center text-muted small">참여 팀원이 없습니다.</div>';

        members.forEach(m => {
            container.insertAdjacentHTML('beforeend', `
            <div class="member-item" id="mem_${m.employeeNo}" onclick="selectMember('${m.employeeNo}', '${m.employeeName}')">
                <div class="d-flex align-items-center gap-3">
                    <div class="member-avatar">${m.employeeName.substring(0, 1)}</div>
                    <div>
                        <div class="fw-bold small">${m.employeeName}</div>
                        <div class="text-muted small">${m.departmentName}</div>
                    </div>
                </div>
                <button class="btn-remove-member" onclick="event.stopPropagation(); removeProjectMember('${m.employeeNo}')">
                    <i data-lucide="user-minus" style="width:18px;"></i>
                </button>
            </div>
          `);
        });
        if (typeof lucide !== 'undefined' && typeof lucide.createIcons === 'function') {
            lucide.createIcons();
        }
    } catch (e) {
        container.innerHTML = '<div class="p-3 text-center text-danger">실패</div>';
    }
}

/* 팀원 선택 및 업무 영역 표시 처리 */
async function selectMember(empNo, empName) {
    selectedEmployeeNo = empNo;

    document.querySelectorAll('.member-item').forEach(el => el.classList.remove('active'));
    const activeItem = document.getElementById(`mem_${empNo}`);
    if (activeItem) activeItem.classList.add('active');

    document.getElementById('selectedMemberName').innerText = empName;
    document.getElementById('taskPlaceholder').style.display = 'none';
    document.getElementById('taskManagementSection').style.display = 'flex';

    fetchMemberTasks();
}

/* 선택 팀원 업무 목록 조회 처리 */
async function fetchMemberTasks() {
    const listContainer = document.getElementById('memberTaskList');
    listContainer.innerHTML = '<div class="text-center py-3"><div class="spinner-border spinner-border-sm text-secondary"></div></div>';

    try {
        const response = await axios.get(`/api/projects/tasks/${currentProjectId}`);
        const currentMemberName = document.getElementById('selectedMemberName').innerText;
        currentMemberTasks = response.data.filter(t => t.assigneeName === currentMemberName);

        filterTasksByStatus(currentTaskTab);
    } catch (e) {
        listContainer.innerHTML = '<div class="text-center py-3 text-danger small">실패</div>';
    }
}

/* 선택 팀원 업무 상태 필터링 처리 */
function filterTasksByStatus(status) {
    currentTaskTab = status;
    const tabButtons = document.querySelectorAll('#taskStatusTabs .nav-link');
    const statusMap = { 'ALL': 0, 'TODO': 1, 'IN_PROGRESS': 2, 'DONE': 3 };

    tabButtons.forEach((btn, idx) => {
        btn.classList.remove('active');
        if (idx === statusMap[status]) btn.classList.add('active');
    });

    const filtered = status === 'ALL' ? currentMemberTasks : currentMemberTasks.filter(t => t.status === status);
    renderMemberTaskCards(filtered);
}

/* 선택 팀원 업무 카드 렌더링 처리 */
function renderMemberTaskCards(tasks) {
    const listContainer = document.getElementById('memberTaskList');
    listContainer.innerHTML = tasks.length ? '' : '<div class="text-center py-4 text-muted small">해당 상태의 업무가 없습니다.</div>';

    tasks.forEach(t => {
        const priorityBadge = t.priority === 'HIGH' ? 'bg-danger' : 'bg-secondary';
        const safeDesc = t.description ? t.description.replace(/'/g, "\\'") : "";
        const barColor = t.status === 'DONE' ? 'bg-success' : 'bg-primary';
        const targetDate = t.dueOn || t.dueDate || '';

        listContainer.insertAdjacentHTML('beforeend', `
            <div class="task-item shadow-sm border-start border-4 ${t.status === 'DONE' ? 'border-success' : 'border-primary'}">
                <div class="d-flex justify-content-between align-items-start mb-1">
                    <span class="fw-bold small" title="${t.description || '설명 없음'}">${t.title}</span>
                    <div class="d-flex gap-1 align-items-center">
                        <span class="badge ${priorityBadge}" style="font-size: 0.6rem;">${t.priority}</span>
                        <button class="btn btn-link text-danger p-0 ms-2" onclick="deleteTask(${t.id})" style="font-size: 0.7rem; text-decoration: none; font-weight: bold;">삭제</button>
                    </div>
                </div>
                ${t.description ? `<div class="text-muted mb-2" style="font-size: 0.65rem;">${t.description}</div>` : ''}
                <div class="progress-bar-bg" style="height: 5px;">
                    <div class="progress-fill ${barColor}" style="width: ${t.progressRate || 0}%"></div>
                </div>
                <div class="d-flex justify-content-between mt-2 align-items-center" style="font-size: 0.7rem;">
                    <div class="d-flex align-items-center gap-2">
                        <span class="text-muted">진행률: ${t.progressRate || 0}%</span>
                        <span id="date-container-${t.id}">
                            <span class="text-danger task-date-display" onclick="editTaskDate(${t.id}, '${targetDate}', '${t.title}', '${t.priority}', '${safeDesc}')">
                                <i data-lucide="clock" style="width:10px; margin-bottom:2px;"></i>
                                <span id="date-text-${t.id}">${targetDate || '기한설정'}</span>
                            </span>
                        </span>
                    </div>
                    <span class="fw-bold ${t.status === 'DONE' ? 'text-success' : 'text-primary'}">${t.status}</span>
                </div>
            </div>
        `);
    });
    if (typeof lucide !== 'undefined' && typeof lucide.createIcons === 'function') {
        lucide.createIcons();
    }
}

/* 업무 기한 인라인 수정 UI 표시 처리 */
function editTaskDate(taskId, currentDate, title, priority, description) {
    const container = document.getElementById(`date-container-${taskId}`);
    if (!container || container.querySelector('input')) return;

    const input = document.createElement('input');
    input.type = 'date';
    input.value = currentDate;
    input.className = 'task-date-input me-1';

    const today = new Date().toISOString().split('T')[0];
    const currentProject = allProjects.find(p => p.id === currentProjectId);
    const projectStart = currentProject ? (currentProject.startOn || currentProject.startsOn) : today;
    const projectEnd = currentProject ? (currentProject.endOn || currentProject.endsOn) : null;
    const minDate = projectStart > today ? projectStart : today;

    input.setAttribute('min', minDate);
    if (projectEnd) input.setAttribute('max', projectEnd);

    const saveBtn = document.createElement('button');
    saveBtn.className = 'btn btn-sm btn-success p-0 px-1 me-1';
    saveBtn.innerHTML = '<i data-lucide="check" style="width:14px; height:14px;"></i>';
    saveBtn.onclick = () => submitDateUpdate(taskId, input.value, currentDate, title, priority, description);

    const cancelBtn = document.createElement('button');
    cancelBtn.className = 'btn btn-sm btn-secondary p-0 px-1';
    cancelBtn.innerHTML = '<i data-lucide="x" style="width:14px; height:14px;"></i>';
    cancelBtn.onclick = () => fetchMemberTasks();

    container.innerHTML = '';
    container.appendChild(input);
    container.appendChild(saveBtn);
    container.appendChild(cancelBtn);

    if (typeof lucide !== 'undefined' && typeof lucide.createIcons === 'function') {
        lucide.createIcons();
    }
    setTimeout(() => input.focus(), 0);
}

/* 업무 기한 수정 요청 처리 */
async function submitDateUpdate(taskId, newDate, oldDate, title, priority, description) {
    if (!newDate || newDate === oldDate) {
        fetchMemberTasks();
        return;
    }

    const today = new Date().toISOString().split('T')[0];
    const currentProject = allProjects.find(p => p.id === currentProjectId);
    const projectStart = currentProject ? (currentProject.startOn || currentProject.startsOn) : today;
    const projectEnd = currentProject ? (currentProject.endOn || currentProject.endsOn) : null;
    const minDate = projectStart > today ? projectStart : today;

    if (newDate < minDate) {
        alert(`마감일은 프로젝트 시작일(${projectStart}) 또는 오늘(${today}) 이후여야 합니다.`);
        fetchMemberTasks();
        return;
    }
    if (projectEnd && newDate > projectEnd) {
        alert(`업무 마감일은 프로젝트 마감일(${projectEnd})을 초과할 수 없습니다.`);
        fetchMemberTasks();
        return;
    }

    try {
        await axios.put(`/api/projects/tasks/${taskId}`, {title, priority, description, dueOn: newDate});
        fetchMemberTasks();
        fetchDashboardData();
    } catch (e) {
        alert("일정 수정 실패");
        fetchMemberTasks();
    }
}

/* 업무 삭제 요청 처리 */
async function deleteTask(taskId) {
    if (!confirm("이 업무를 삭제하시겠습니까?")) return;
    try {
        await axios.delete(`/api/projects/tasks/${taskId}`);
        fetchMemberTasks();
        fetchDashboardData();
    } catch (e) {
        alert("삭제 실패");
    }
}

/* 선택 팀원 업무 할당 요청 처리 */
async function assignTask() {
    const title = document.getElementById('taskTitleInput').value;
    const description = document.getElementById('taskDescInput').value;
    const priority = document.getElementById('taskPriorityInput').value;
    const dueOn = document.getElementById('taskDueDateInput').value;
    const loginEmpNo = localStorage.getItem('employeeNo');

    if (!title.trim()) return alert("업무 제목을 입력하세요.");

    const today = new Date().toISOString().split('T')[0];
    const currentProject = allProjects.find(p => p.id === currentProjectId);
    const projectStart = currentProject ? (currentProject.startOn || currentProject.startsOn) : today;
    const projectEnd = currentProject ? (currentProject.endOn || currentProject.endsOn) : null;
    const minDate = projectStart > today ? projectStart : today;

    if (dueOn && dueOn < minDate) return alert(`마감일은 프로젝트 시작일(${projectStart}) 또는 오늘(${today}) 이후여야 합니다.`);
    if (dueOn && projectEnd && dueOn > projectEnd) return alert(`업무 마감일은 프로젝트 마감일(${projectEnd})을 초과할 수 없습니다.`);

    const currentStatusElement = document.getElementById('modalProjectStatus');
    const isAlreadyInProgress = currentStatusElement && currentStatusElement.value === 'IN_PROGRESS';

    try {
        await axios.post('/api/projects/tasks', {
            projectId: currentProjectId,
            assigneeNo: selectedEmployeeNo,
            title: title,
            description: description,
            priority: priority,
            dueOn: dueOn,
            status: 'TODO',
            assignedBy: loginEmpNo
        });

        try {
            await axios.patch(`/api/projects/${currentProjectId}/status`, { status: "IN_PROGRESS" });
        } catch (statusError) {
            console.error("프로젝트 상태 자동 전환 실패:", statusError);
        }

        document.getElementById('taskTitleInput').value = '';
        document.getElementById('taskDescInput').value = '';
        document.getElementById('taskDueDateInput').value = '';

        fetchMemberTasks();
        fetchDashboardData();

        if (isAlreadyInProgress) {
            alert("업무가 정상적으로 할당되었습니다.");
        } else {
            alert("업무가 할당되었으며 프로젝트가 '진행 중' 상태로 전환되었습니다.");
        }
    } catch (e) {
        alert("할당 실패: 입력 데이터를 확인해주세요.");
    }
}

/* 프로젝트 팀원 초대 요청 처리 */
async function addProjectMember() {
    const empNo = document.getElementById('employeeSelect').value;
    if (!empNo) return alert("직원을 선택하세요.");

    try {
        await axios.post(`/api/projects/${currentProjectId}/members`, {employeeNo: empNo});
        alert("멤버가 프로젝트에 초대되었으며 알림이 발송되었습니다.");
        fetchMemberList();
    } catch (e) {
        const errorMsg = e.response?.data?.error || "이미 참여 중이거나 추가에 실패했습니다.";
        alert(errorMsg);
    }
}

/* 프로젝트 팀원 제외 및 미완료 업무 검증 처리 */
async function removeProjectMember(en) {
    try {
        const taskResponse = await axios.get(`/api/projects/tasks/${currentProjectId}`);
        const allTasks = taskResponse.data;

        const memberItem = document.getElementById(`mem_${en}`);
        const employeeName = memberItem ? memberItem.querySelector('.fw-bold.small').innerText : '';

        const hasIncompleteTask = allTasks.some(task => {
            const isHisTask = task.assigneeNo === en || task.assigneeName === employeeName;
            const progress = parseInt(task.progressRate || 0);
            const status = task.status ? task.status.trim().toUpperCase() : 'TODO';
            return isHisTask && (progress < 100 || status !== 'DONE');
        });

        if (hasIncompleteTask) {
            alert(`해당 팀원은 아직 완료되지 않은 업무가 남아있어 프로젝트에서 제외할 수 없습니다.\n할당된 모든 업무를 완료 처리하거나 업무를 삭제한 후 시도해 주세요.`);
            return;
        }

        if (!confirm("해당 팀원을 프로젝트에서 제외하시겠습니까?")) return;

        await axios.delete(`/api/projects/${currentProjectId}/members/${en}`);

        if (selectedEmployeeNo === en) {
            document.getElementById('taskManagementSection').style.display = 'none';
            document.getElementById('taskPlaceholder').style.display = 'flex';
        }

        fetchMemberList();
        alert("팀원이 프로젝트에서 정상적으로 제외되었습니다.");
    } catch (e) {
        alert("팀원 제외 처리 중 오류가 발생했습니다.");
    }
}

// 팀원 개인 일정 모달 및 FullCalendar 처리

/* 팀원 일정 확인 모달 표시 처리 */
async function openMemberScheduleModal() {
    const currentMemberName = document.getElementById('selectedMemberName').innerText;
    if (!selectedEmployeeNo || !currentMemberName) {
        return alert("선택된 팀원 정보가 없습니다.");
    }

    document.getElementById('scheduleModalTitle').innerText = `${currentMemberName} 님의 일정 확인`;

    const calendarEl = document.getElementById('memberCalendar');
    calendarEl.innerHTML = `
        <div class="d-flex flex-column align-items-center justify-content-center py-5" style="min-height: 400px;">
            <div class="spinner-border text-primary mb-2" role="status"></div>
            <div class="text-muted small">팀원의 일정을 불러오는 중입니다...</div>
        </div>
    `;

    if (memberScheduleModal) {
        memberScheduleModal.show();
    }

    setTimeout(async () => {
        try {
            calendarEl.innerHTML = '';

            memberCalendar = new FullCalendar.Calendar(calendarEl, {
                initialView: 'dayGridMonth',
                locale: 'ko',
                headerToolbar: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'dayGridMonth,timeGridWeek'
                },
                height: 500,
                navLinks: true,
                editable: false,
                selectable: false,
                events: async function(info, successCallback, failureCallback) {
                    try {
                        const response = await axios.get(`/api/calendar/events`, {
                            params: {
                                empNo: selectedEmployeeNo,
                                start: info.startStr,
                                end: info.endStr
                            }
                        });

                        if (!response.data || response.data.length === 0) {
                            successCallback([]);
                            return;
                        }

                        const events = response.data.map(evt => ({
                            id: evt.id,
                            title: evt.title || '제목 없음',
                            start: evt.startAt || evt.start || evt.startsAt,
                            end: evt.endAt || evt.end || evt.endsAt,
                            description: evt.description,
                            color: evt.color || '#4f46e5',
                            allDay: evt.allDay ?? true
                        }));

                        successCallback(events);
                    } catch (error) {
                        console.error("일정 API 로드 실패:", error);
                        failureCallback(error);
                    }
                },

                eventClick: function(info) {
                    const eventObj = info.event;
                    document.getElementById('eventDetailTitle').innerText = eventObj.title;

                    const desc = eventObj.extendedProps.description;
                    if (desc !== undefined && desc !== null && String(desc).trim() !== '') {
                        document.getElementById('eventDetailDesc').innerText = desc;
                    } else {
                        document.getElementById('eventDetailDesc').innerText = '등록된 상세 설명이 없습니다.';
                    }

                    let dateText = '';
                    const startStr = eventObj.startStr;
                    const endStr = eventObj.endStr;

                    if (!endStr || startStr === endStr) {
                        dateText = startStr;
                    } else {
                        dateText = `${startStr} ~ ${endStr}`;
                    }
                    document.getElementById('eventDetailDate').innerText = dateText;

                    const badgeEl = document.getElementById('eventDetailBadge');
                    if (eventObj.id && eventObj.id.startsWith('CAL_')) {
                        badgeEl.innerText = '개인 일정';
                        badgeEl.style.backgroundColor = '#4f46e5';
                    } else {
                        badgeEl.innerText = '프로젝트 업무';
                        badgeEl.style.backgroundColor = eventObj.backgroundColor || '#0ca5e9';
                    }

                    if (eventDetailModal) {
                        eventDetailModal.show();
                        if (typeof lucide !== 'undefined' && typeof lucide.createIcons === 'function') {
                            lucide.createIcons();
                        }
                    }
                }
            });

            memberCalendar.render();
            memberCalendar.updateSize();

        } catch (err) {
            console.error("캘린더 생성 중 예외 발생:", err);
            calendarEl.innerHTML = '<div class="text-center py-5 text-danger">캘린더를 로드하는 중 오류가 발생했습니다.</div>';
        }
    }, 300);
}


// 프로젝트 전체 일정 모달 및 FullCalendar 처리

/* 프로젝트 전체 일정 모달 표시 처리 */
async function openProjectAllSchedule() {
    if (!currentProjectId) {
        return alert("현재 선택된 프로젝트 정보가 없습니다.");
    }

    const projectName = document.getElementById('modalProjectName').innerText;
    document.getElementById('projectScheduleModalTitle').innerHTML = `<i data-lucide="calendar-days" class="me-2"></i> [${projectName}] 전체 업무 및 일정 타임라인`;

    // 스피너 로딩 화면 설정
    const calendarEl = document.getElementById('projectCalendar');
    calendarEl.innerHTML = `
        <div class="d-flex flex-column align-items-center justify-content-center py-5" style="min-height: 500px;">
            <div class="spinner-border text-primary mb-2" role="status"></div>
            <div class="text-muted small">모든 팀원의 업무와 일정을 모아오는 중입니다...</div>
        </div>
    `;

    // 모달 먼저 표시
    if (projectScheduleModal) {
        projectScheduleModal.show();
    }

    // 모달이 완전히 열린 뒤 캘린더 렌더링
    setTimeout(async () => {
        try {
            calendarEl.innerHTML = ''; // 스피너 제거

            projectFullCalendar = new FullCalendar.Calendar(calendarEl, {
                initialView: 'dayGridMonth',
                locale: 'ko',
                headerToolbar: {
                    left: 'prev,next today',
                    center: 'title',
                    right: 'dayGridMonth,timeGridWeek'
                },
                height: 600, // 전체 일정 모달용 높이
                navLinks: true,
                editable: false,
                selectable: false,
                events: async function(info, successCallback, failureCallback) {
                    try {
                        const loginEmpNo = localStorage.getItem('employeeNo'); // 현재 로그인 사번

                        // 프로젝트 전체 일정 조회 요청
                        const response = await axios.get('/api/calendar/project-events', {
                            params: {
                                projectId: currentProjectId,
                                empNo: loginEmpNo,
                                start: info.startStr,
                                end: info.endStr
                            }
                        });

                        if (!response.data || response.data.length === 0) {
                            successCallback([]);
                            return;
                        }

                        // 서버 응답을 FullCalendar 이벤트 형식으로 변환
                        const events = response.data.map(evt => ({
                            id: evt.id,
                            title: evt.title || '제목 없음',
                            start: evt.startAt || evt.start || evt.startsAt,
                            end: evt.endAt || evt.end || evt.endsAt,
                            description: evt.description,
                            color: evt.color || '#0ca5e9', // 기본 색상
                            allDay: evt.allDay ?? true
                        }));

                        successCallback(events);
                    } catch (error) {
                        console.error("프로젝트 전체 일정 로드 실패:", error);
                        failureCallback(error);
                    }
                },
                eventClick: function(info) {
                    const eventObj = info.event;

                    // 캘린더 일정 클릭 시 상세 모달 내용 설정
                    document.getElementById('eventDetailTitle').innerText = eventObj.title;

                    const desc = eventObj.extendedProps.description;
                    if (desc !== undefined && desc !== null && String(desc).trim() !== '') {
                        document.getElementById('eventDetailDesc').innerText = desc;
                    } else {
                        document.getElementById('eventDetailDesc').innerText = '등록된 상세 설명이 없습니다.';
                    }

                    let dateText = '';
                    const startStr = eventObj.startStr;
                    const endStr = eventObj.endStr;

                    if (!endStr || startStr === endStr) {
                        dateText = startStr;
                    } else {
                        dateText = `${startStr} ~ ${endStr}`;
                    }
                    document.getElementById('eventDetailDate').innerText = dateText;

                    // 일정 유형별 배지 색상 설정
                    const badgeEl = document.getElementById('eventDetailBadge');
                    if (eventObj.id && eventObj.id.startsWith('CAL_')) {
                        badgeEl.innerText = '공유 일정';
                        badgeEl.style.backgroundColor = '#10b981'; // 공유 일정 색상
                    } else {
                        badgeEl.innerText = '팀원 업무';
                        badgeEl.style.backgroundColor = eventObj.backgroundColor || '#0ca5e9'; // 팀원 업무 색상
                    }

                    if (eventDetailModal) {
                        eventDetailModal.show();
                        if (typeof lucide !== 'undefined' && typeof lucide.createIcons === 'function') {
                            lucide.createIcons();
                        }
                    }
                }
            });

            projectFullCalendar.render();
            projectFullCalendar.updateSize();

            if (typeof lucide !== 'undefined' && typeof lucide.createIcons === 'function') {
                lucide.createIcons();
            }

        } catch (err) {
            console.error("캘린더 생성 중 예외 발생:", err);
            calendarEl.innerHTML = '<div class="text-center py-5 text-danger">캘린더를 로드하는 중 오류가 발생했습니다.</div>';
        }
    }, 300); // 모달 표시 후 캘린더 렌더링 대기
}