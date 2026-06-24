// 서버 부서 enum 코드의 화면 표시명
const departmentLabels = {
    FRONTEND: '프론트엔드팀',
    BACKEND: '백엔드팀',
    MANAGEMENT_SUPPORT: '경영지원팀',
    MARKETING: '마케팅팀',
    DB_MANAGEMENT: 'DB관리팀'
};

const positionLabels = {
    STAFF: '사원',
    ASSISTANT_MANAGER: '대리',
    MANAGER: '과장',
    GENERAL_MANAGER: '부장',
    DEPUTY_GENERAL_MANAGER: '차장',
    CEO: '사장'
};

const roleLabels = {
    USER: '일반',
    ADMIN: '관리자',
    SUPER: '최고관리자'
};

const statusLabels = {
    ACTIVE: '재직',
    ON_LEAVE: '휴직',
    RESIGNED: '퇴사'
};

// 조직도 API에서 조회한 직원 목록
let employees = [];
// 현재 선택된 부서 필터
let selectedDepartment = 'ALL';
// 부서별 보기에서 사용하는 재직 상태 필터
let selectedStatus = 'ACTIVE';
// 현재 로그인 사용자의 시스템 역할
let currentUserRole = null;
// 오른쪽 직원 목록에서 펼쳐진 부서 섹션
const expandedDepartments = new Set(Object.keys(departmentLabels));
// 왼쪽 조직도 트리의 전체 노드 펼침 여부
let organizationTreeExpanded = true;
// 왼쪽 조직도 트리에서 펼쳐진 부서 노드
const expandedTreeDepartments = new Set(Object.keys(departmentLabels));
// 부서별 직원 카드 목록의 현재 페이지
const departmentPageMap = {};
// 부서별 직원 카드 목록은 한 화면에 4명씩 표시
const DEPARTMENT_PAGE_SIZE = 4;

/* 조직도 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', async function () {
    // Lucide 아이콘 초기화
    lucide.createIcons();

    // 검색어 변경 시 페이지 초기화 후 조직도 재렌더링
    document.getElementById('employee-search').addEventListener('input', function () {
        resetDepartmentPages();
        renderOrganization();
    });

    await loadLoginUser();
    await loadOrganization();
});

/* 현재 로그인 사용자 정보 조회 처리 */
async function loadLoginUser() {
    /*
     * layout.html의 사용자 이름은 기본값이 "방문자"임
     * 현재 프로젝트는 JWT를 localStorage에 저장하므로 HTML 렌더링 시점에는 서버가 로그인 사용자를 알 수 없음
     * 그래서 화면이 열린 뒤 /api/auth/me를 호출해서 레이아웃의 이름 영역을 직접 갱신
     */
    const accessToken = localStorage.getItem('accessToken');

    // 토큰이 없으면 로그인 화면 이동 처리
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 현재 로그인 사용자 정보 조회 요청
        const response = await fetch('/api/auth/me', {
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        // 사용자 조회 실패 시 토큰 정리 후 로그인 화면 이동
        if (!response.ok) {
            clearTokens();
            window.location.href = '/login';
            return;
        }

        // 현재 사용자 역할과 레이아웃 이름 반영
        const data = await response.json();
        currentUserRole = data.role;
        const layoutUserName = document.getElementById('layout-user-name');
        if (layoutUserName) {
            layoutUserName.innerText = data.name;
        }
    } catch (error) {
        // 통신 실패 시 인증 상태 초기화
        clearTokens();
        window.location.href = '/login';
    }
}

/* 조직도 직원 목록 조회 처리 */
async function loadOrganization() {
    const accessToken = localStorage.getItem('accessToken');
    try {
        // 조직도 직원 목록 조회 요청
        const response = await fetch('/api/management/employees/organization', {
            headers: {'Authorization': 'Bearer ' + accessToken}
        });
        if (!response.ok) throw new Error();

        employees = await response.json();

        // 이전 조회 실패 메시지 초기화
        const errBox = document.getElementById('organization-error');
        errBox.innerText = '';
        errBox.classList.remove('open');

        // 조회 데이터 기준으로 전체 조직도 렌더링
        renderOrganization();
    } catch (error) {
        // 조회 실패 메시지 표시
        const errBox = document.getElementById('organization-error');
        errBox.innerText = '데이터를 불러올 수 없습니다.';
        errBox.classList.add('open');
    }
}

/* 조직도 주요 영역 렌더링 처리 */
function renderOrganization() {
    renderDepartmentTabs();
    renderOrganizationTree();
    renderStatusFilter();
    renderMemberGrid();

    // 동적 HTML 삽입 후 Lucide 아이콘 렌더링
    lucide.createIcons();
}

/* 부서 탭 렌더링 처리 */
function renderDepartmentTabs() {
    const deptContainer = document.getElementById('dept-list');

    // 부서별 재직자 수와 전체 재직자 수 계산
    const counts = countByDepartment();
    const activeEmployeeCount = employees.filter(emp => emp.status === 'ACTIVE').length;

    // 전체 탭과 부서별 탭 HTML 생성
    let html = createTabHtml('ALL', '조직도 전체', activeEmployeeCount);
    Object.keys(departmentLabels).forEach(key => {
        html += createTabHtml(key, departmentLabels[key], counts[key] || 0);
    });

    deptContainer.innerHTML = html;
}

/* 부서 탭 버튼 HTML 생성 처리 */
function createTabHtml(dept, label, count) {
    // 선택된 부서는 active 클래스로 표시
    const active = selectedDepartment === dept ? ' active' : '';
    return `
        <button type="button" class="dept-tab${active}" onclick="selectDepartment('${dept}')">
            ${label}
            <span class="dept-count">${count}</span>
        </button>
    `;
}

/* 부서 선택 필터 변경 처리 */
function selectDepartment(dept) {
    selectedDepartment = dept;

    // 전체 조직도는 항상 재직자만 보여주는 정책이므로 ACTIVE로 고정
    // 특정 부서를 선택하면 재직/휴직을 함께 살펴볼 수 있도록 전체 상태 필터로 시작
    selectedStatus = dept === 'ALL' ? 'ACTIVE' : 'ALL';

    // 이전에 직원 검색 또는 트리 직원 클릭으로 검색어가 남아 있으면
    // 새로 선택한 부서의 직원 목록도 그 검색어로 계속 좁혀짐
    // 부서 전환은 "새 부서 전체를 보겠다"는 동작이므로 검색어 초기화
    document.getElementById('employee-search').value = '';

    // 특정 부서 선택 시 해당 부서 섹션 펼침 처리
    if (dept !== 'ALL') {
        expandedDepartments.add(dept);
        organizationTreeExpanded = true;
        expandedTreeDepartments.add(dept);
    }
    resetDepartmentPages();
    renderOrganization();
}

/* 조직도 트리 렌더링 처리 */
function renderOrganizationTree() {
    const treeContainer = document.getElementById('organization-tree');
    const counts = countByDepartment();
    const activeEmployeeCount = employees.filter(emp => emp.status === 'ACTIVE').length;

    // 전체 조직 아래에 부서와 직원을 계층형으로 렌더링
    treeContainer.innerHTML = createTreeRootHtml(activeEmployeeCount, counts);
}

/* 조직도 트리 최상단 HTML 생성 처리 */
function createTreeRootHtml(count, counts) {
    // 전체 조직도 선택 상태면 active 클래스 적용
    const active = selectedDepartment === 'ALL' ? ' active' : '';
    const iconName = organizationTreeExpanded ? 'chevron-down' : 'chevron-right';
    const departmentHtml = organizationTreeExpanded
        ? Object.keys(departmentLabels).map(function (dept) {
            const deptEmployees = employees.filter(emp => emp.department === dept && emp.status === 'ACTIVE');
            return createTreeDepartmentHtml(dept, departmentLabels[dept], counts[dept] || 0, deptEmployees);
        }).join('')
        : '';

    return `
        <div class="tree-root-group">
            <button type="button" class="tree-root${active}" onclick="selectDepartment('ALL')">
                <i class="tree-icon" data-lucide="building-2"></i>
                <span>OfficeMate 전체</span>
                <span class="tree-spacer"></span>
                <span class="tree-count">${count}</span>
            </button>
            <button type="button" class="tree-toggle-button" onclick="toggleOrganizationTree(event)" aria-label="전체 조직 펼침 전환">
                <i data-lucide="${iconName}"></i>
            </button>
        </div>
        <div class="tree-root-children${organizationTreeExpanded ? ' open' : ''}">
            ${departmentHtml}
        </div>
    `;
}

/* 조직도 트리 부서 노드 HTML 생성 처리 */
function createTreeDepartmentHtml(dept, label, count, deptEmployees) {
    // 선택된 부서면 active 클래스 적용
    const active = selectedDepartment === dept ? ' active' : '';
    const expanded = expandedTreeDepartments.has(dept);
    const iconName = expanded ? 'chevron-down' : 'chevron-right';

    // 부서에 속한 직원 노드 HTML 생성
    const employeeHtml = deptEmployees.map(createTreeEmployeeHtml).join('');

    return `
        <div class="tree-group">
            <div class="tree-dept-row">
                <button type="button" class="tree-dept${active}" onclick="selectDepartment('${dept}')">
                    <i class="tree-icon" data-lucide="folder"></i>
                    <span>${label}</span>
                    <span class="tree-spacer"></span>
                    <span class="tree-count">${count}</span>
                </button>
                <button type="button" class="tree-toggle-button" onclick="toggleTreeDepartment(event, '${dept}')" aria-label="${label} 펼침 전환">
                    <i data-lucide="${iconName}"></i>
                </button>
            </div>
            <div class="tree-children${expanded ? ' open' : ''}">
                ${employeeHtml || '<div class="tree-employee">등록된 직원 없음</div>'}
            </div>
        </div>
    `;
}

/* 전체 조직 트리 펼침 전환 처리 */
function toggleOrganizationTree(event) {
    // 토글 클릭이 상위 선택 이벤트로 번지지 않도록 차단
    event.stopPropagation();

    organizationTreeExpanded = !organizationTreeExpanded;
    renderOrganization();
}

/* 부서 트리 펼침 전환 처리 */
function toggleTreeDepartment(event, department) {
    // 토글 클릭이 부서 선택 이벤트로 번지지 않도록 차단
    event.stopPropagation();

    // 펼쳐져 있으면 접고, 접혀 있으면 펼침 처리
    if (expandedTreeDepartments.has(department)) {
        expandedTreeDepartments.delete(department);
    } else {
        expandedTreeDepartments.add(department);
    }

    renderOrganization();
}

/* 트리 직원 버튼 HTML 생성 처리 */
function createTreeEmployeeHtml(emp) {
    // 직원 직급 코드를 화면 표시명으로 변환
    const posName = positionLabels[emp.position] || emp.position;

    return `
        <button type="button" class="tree-employee" onclick="focusEmployee('${emp.employeeNo}')">
            <i class="tree-icon" data-lucide="user"></i>
            <span>${emp.name}</span>
            <span class="tree-spacer"></span>
            <span>${posName}</span>
        </button>
    `;
}

/* 트리 직원 선택 시 카드 포커스 처리 */
function focusEmployee(employeeNo) {
    // 트리에서 직원을 누르면 해당 직원이 속한 부서로 이동하고,
    // 검색어를 사번으로 채워 직원 카드가 바로 좁혀지게 함
    const employee = employees.find(emp => emp.employeeNo === employeeNo);
    if (!employee) {
        return;
    }

    selectedDepartment = employee.department;
    expandedDepartments.add(employee.department);
    organizationTreeExpanded = true;
    expandedTreeDepartments.add(employee.department);
    document.getElementById('employee-search').value = employee.employeeNo;
    resetDepartmentPages();
    renderOrganization();
}

/* 직원 카드 목록 렌더링 처리 */
function renderMemberGrid() {
    // 직원 카드 영역과 필터링된 직원 목록 조회
    const grid = document.getElementById('member-grid');
    const filtered = getFilteredEmployees();

    // 선택된 부서 제목과 직원 수 요약 표시
    document.getElementById('selected-dept-title').innerText =
        selectedDepartment === 'ALL' ? '전체 직원' : departmentLabels[selectedDepartment];
    document.getElementById('member-summary').innerText = filtered.length + '명';

    // 검색 결과가 없으면 빈 상태 메시지 표시
    if (filtered.length === 0) {
        grid.innerHTML = '<div class="member-empty-message">검색 결과가 없습니다.</div>';
        return;
    }

    // 필터링된 직원을 부서별 섹션으로 렌더링
    grid.innerHTML = createDepartmentSectionsHtml(filtered);
}

/* 부서별 직원 섹션 HTML 변환 처리 */
function createDepartmentSectionsHtml(filteredEmployees) {
    // 직원 목록을 부서 코드 기준으로 그룹핑
    const groupedEmployees = groupEmployeesByDepartment(filteredEmployees);

    // 전체 조직도는 모든 부서, 특정 부서 선택 시 해당 부서만 표시
    const departments = selectedDepartment === 'ALL'
        ? Object.keys(departmentLabels)
        : [selectedDepartment];

    // 검색 중이면 결과가 있는 부서 자동 펼침 처리
    const keyword = getSearchKeyword();

    return departments
        .map(function (department) {
            // 현재 부서 직원 목록 조회
            const departmentEmployees = groupedEmployees[department] || [];

            // 직원이 없는 부서는 표시하지 않음
            if (departmentEmployees.length === 0) {
                return '';
            }

            // 검색어가 있으면 펼침 상태를 강제 적용
            const expanded = keyword ? true : expandedDepartments.has(department);
            return createDepartmentSectionHtml(department, departmentEmployees, expanded);
        })
        .join('');
}

/* 직원 목록 부서별 그룹핑 처리 */
function groupEmployeesByDepartment(items) {
    // 부서 코드를 key로 직원 배열 저장
    return items.reduce(function (groups, employee) {
        const department = employee.department;
        groups[department] = groups[department] || [];
        groups[department].push(employee);
        return groups;
    }, {});
}

/* 부서별 직원 목록 섹션 HTML 생성 처리 */
function createDepartmentSectionHtml(department, departmentEmployees, expanded) {
    // 부서명과 상태별 인원 수 계산
    const departmentName = departmentLabels[department] || department;
    const activeCount = departmentEmployees.filter(emp => emp.status === 'ACTIVE').length;
    const leaveCount = departmentEmployees.filter(emp => emp.status === 'ON_LEAVE').length;
    const totalPages = Math.ceil(departmentEmployees.length / DEPARTMENT_PAGE_SIZE);
    const currentPage = getValidDepartmentPage(department, totalPages);
    const startIndex = currentPage * DEPARTMENT_PAGE_SIZE;
    const pagedEmployees = departmentEmployees.slice(startIndex, startIndex + DEPARTMENT_PAGE_SIZE);

    // 펼침 상태에 따라 아이콘과 본문 노출 결정
    const iconName = expanded ? 'chevron-down' : 'chevron-right';
    const bodyHtml = expanded
        ? `
            <div class="member-section-grid">${pagedEmployees.map(createMemberCard).join('')}</div>
            ${createDepartmentPaginationHtml(department, currentPage, totalPages)}
        `
        : '';

    return `
        <section class="member-department-section">
            <button type="button" class="member-section-header" onclick="toggleDepartmentSection('${department}')">
                <span class="member-section-title">
                    <i data-lucide="${iconName}"></i>
                    ${departmentName}
                </span>
                <span class="member-section-summary">
                    전체 ${departmentEmployees.length}명 · 재직 ${activeCount}명 · 휴직 ${leaveCount}명
                </span>
            </button>
            ${bodyHtml}
        </section>
    `;
}

/* 부서별 직원 목록 페이지 보정 처리 */
function getValidDepartmentPage(department, totalPages) {
    // 페이지가 없거나 전체 페이지가 0이면 첫 페이지 처리
    if (!departmentPageMap[department] || totalPages === 0) {
        departmentPageMap[department] = 0;
        return 0;
    }

    // 필터 변경으로 현재 페이지가 범위를 넘으면 마지막 페이지로 보정
    if (departmentPageMap[department] >= totalPages) {
        departmentPageMap[department] = totalPages - 1;
    }

    return departmentPageMap[department];
}

/* 부서별 직원 목록 페이지 UI 생성 처리 */
function createDepartmentPaginationHtml(department, currentPage, totalPages) {
    // 직원 수가 적어도 1 / 1 페이지 정보 표시
    const displayTotalPages = Math.max(totalPages, 1);

    // 첫 페이지와 마지막 페이지에서 이전/다음 버튼 비활성화
    const prevDisabled = currentPage <= 0 ? ' disabled' : '';
    const nextDisabled = currentPage >= displayTotalPages - 1 ? ' disabled' : '';

    return `
        <div class="member-section-pagination">
            <button type="button" class="member-page-button" onclick="moveDepartmentPage('${department}', -1)"${prevDisabled}>
                이전
            </button>
            <span class="member-page-info">${currentPage + 1} / ${displayTotalPages}</span>
            <button type="button" class="member-page-button" onclick="moveDepartmentPage('${department}', 1)"${nextDisabled}>
                다음
            </button>
        </div>
    `;
}

/* 부서별 직원 카드 페이지 이동 처리 */
function moveDepartmentPage(department, delta) {
    // 현재 부서의 필터링된 직원 수 기준으로 전체 페이지 계산
    const departmentEmployees = getFilteredEmployees().filter(emp => emp.department === department);
    const totalPages = Math.ceil(departmentEmployees.length / DEPARTMENT_PAGE_SIZE);
    const currentPage = getValidDepartmentPage(department, totalPages);
    const nextPage = currentPage + delta;

    // 조회 가능 범위를 벗어나면 이동하지 않음
    if (nextPage < 0 || nextPage >= totalPages) {
        return;
    }

    // 부서별 현재 페이지 저장 후 화면 갱신
    departmentPageMap[department] = nextPage;
    renderOrganization();
}

/* 부서별 직원 카드 페이지 초기화 처리 */
function resetDepartmentPages() {
    // 검색어, 부서, 상태 필터 변경 시 기존 페이지 위치 초기화
    Object.keys(departmentPageMap).forEach(function (department) {
        departmentPageMap[department] = 0;
    });
}

/* 부서별 직원 목록 섹션 펼침 전환 처리 */
function toggleDepartmentSection(department) {
    // 펼쳐져 있으면 접고, 접혀 있으면 펼침 처리
    if (expandedDepartments.has(department)) {
        expandedDepartments.delete(department);
    } else {
        expandedDepartments.add(department);
    }

    // 변경된 펼침 상태 화면 반영
    renderOrganization();
}

/* 재직 상태 필터 렌더링 처리 */
function renderStatusFilter() {
    /*
     * 전체 조직도는 현재 재직 중인 직원만 보여주는 화면임
     * 그래서 상태 필터를 숨기고 ACTIVE 필터를 강제
     *
     * 부서별 화면에서는 휴직자도 조직 구성원으로 함께 관리해야 하므로
     * 전체/재직/휴직 상태 탭을 노출
     */
    const panel = document.getElementById('member-status-panel');
    const tabs = document.getElementById('status-tabs');
    const summary = document.getElementById('status-summary');

    // 전체 조직도에서는 상태 필터를 숨기고 ACTIVE 강제 처리
    if (selectedDepartment === 'ALL') {
        selectedStatus = 'ACTIVE';
        panel.classList.remove('open');
        tabs.innerHTML = '';
        return;
    }

    // 선택된 부서 기준 재직/휴직 인원 계산
    const departmentEmployees = employees.filter(emp => emp.department === selectedDepartment);
    const activeCount = departmentEmployees.filter(emp => emp.status === 'ACTIVE').length;
    const leaveCount = departmentEmployees.filter(emp => emp.status === 'ON_LEAVE').length;

    // 상태 요약과 상태 필터 탭 반영
    summary.innerText = '재직 ' + activeCount + '명 · 휴직 ' + leaveCount + '명';
    tabs.innerHTML = [
        createStatusTabHtml('ALL', '전체', departmentEmployees.length),
        createStatusTabHtml('ACTIVE', '재직', activeCount),
        createStatusTabHtml('ON_LEAVE', '휴직', leaveCount)
    ].join('');
    panel.classList.add('open');
}

/* 재직 상태 필터 버튼 HTML 생성 처리 */
function createStatusTabHtml(status, label, count) {
    // 선택된 상태는 active 클래스로 강조
    const active = selectedStatus === status ? ' active' : '';
    return `
        <button type="button" class="status-tab${active}" onclick="selectStatus('${status}')">
            ${label} ${count}
        </button>
    `;
}

/* 재직 상태 필터 변경 처리 */
function selectStatus(status) {
    selectedStatus = status;

    resetDepartmentPages();
    renderOrganization();
}

/* 직원 카드 HTML 생성 처리 */
function createMemberCard(emp) {
    // 이름 첫 글자와 코드 값의 화면 표시명 계산
    const initial = emp.name ? emp.name.substring(0, 1) : '?';
    const deptName = departmentLabels[emp.department] || emp.department;
    const posName = positionLabels[emp.position] || emp.position;
    const statusName = statusLabels[emp.status] || emp.status;

    // SUPER 사용자에게만 직원 수정 버튼 표시
    const managementAction = currentUserRole === 'SUPER'
        ? `
            <div class="member-card-actions">
                <button type="button" class="edit-member-button" onclick="openManagementModal('${emp.employeeNo}')">
                    <i data-lucide="edit-3"></i>
                    수정
                </button>
            </div>
        `
        : '';

    return `
        <article class="member-card">
            <div class="member-info-top">
                <div class="member-avatar">${initial}</div>
                <div class="member-name-area">
                    <div class="name-row">
                        <span class="member-name">${emp.name}</span>
                        <span class="role-badge">${roleLabels[emp.role]}</span>
                        <span class="status-badge ${emp.status}">${statusName}</span>
                    </div>
                    <div class="pos-dept">${posName} · ${deptName}</div>
                </div>
            </div>
            <div class="member-contact">
                <div class="contact-item">
                    <i data-lucide="hash"></i>
                    <span>사번: ${emp.employeeNo}</span>
                </div>
                <a href="mailto:${emp.email}" class="contact-item email">
                    <i data-lucide="mail"></i>
                    <span>${emp.email}</span>
                </a>
            </div>
            ${managementAction}
        </article>
    `;
}

/* 직원 관리 정보 수정 모달 열기 처리 */
function openManagementModal(employeeNo) {
    // 선택된 직원 정보 조회
    const employee = employees.find(emp => emp.employeeNo === employeeNo);

    // 직원이 없거나 SUPER 권한이 아니면 모달 열기 중단
    if (!employee || currentUserRole !== 'SUPER') {
        return;
    }

    // 직원 현재 정보를 모달 입력값에 반영
    document.getElementById('management-employee-no').value = employee.employeeNo;
    document.getElementById('management-name').value = employee.name || '';
    document.getElementById('management-department').value = employee.department;
    document.getElementById('management-position').value = employee.position;
    document.getElementById('management-role').value = employee.role;
    document.getElementById('management-status').value = employee.status;
    document.getElementById('management-resigned-on').value = employee.resignedOn || '';
    hideManagementMessage();
    toggleResignedOnField();

    // 모달 열림 상태 처리
    const modal = document.getElementById('employee-management-modal');
    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');

    // 모달 내부 Lucide 아이콘 렌더링
    lucide.createIcons();
}

/* 직원 관리 정보 수정 모달 닫기 처리 */
function closeManagementModal() {
    // 모달 닫힘 상태 처리
    const modal = document.getElementById('employee-management-modal');
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');

    // 모달 메시지 초기화
    hideManagementMessage();
}

/* 퇴사일 입력 필드 표시 전환 처리 */
function toggleResignedOnField() {
    // 현재 선택된 재직 상태와 퇴사일 필드 조회
    const status = document.getElementById('management-status').value;
    const field = document.getElementById('resigned-on-field');

    // 퇴사 상태일 때만 퇴사일 입력 필드 표시
    field.classList.toggle('open', status === 'RESIGNED');
}

/* 직원 관리 정보 저장 요청 처리 */
async function saveManagedEmployee(event) {
    // 기본 form 제출 대신 fetch 저장 요청 사용
    event.preventDefault();

    // 저장 API 호출에 필요한 값 조회
    const accessToken = localStorage.getItem('accessToken');
    const employeeNo = document.getElementById('management-employee-no').value;
    const status = document.getElementById('management-status').value;
    const saveButton = document.getElementById('management-save-button');

    // 중복 제출 방지를 위한 저장 버튼 비활성화
    saveButton.disabled = true;
    hideManagementMessage();

    // 직원 관리 정보 수정 요청 body 생성
    const requestBody = {
        name: document.getElementById('management-name').value.trim(),
        department: document.getElementById('management-department').value,
        position: document.getElementById('management-position').value,
        role: document.getElementById('management-role').value,
        status: status,
        resignedOn: status === 'RESIGNED'
            ? (document.getElementById('management-resigned-on').value || null)
            : null
    };

    try {
        // 직원 관리 정보 수정 요청
        const response = await fetch('/api/management/employees/' + employeeNo + '/management', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + accessToken
            },
            body: JSON.stringify(requestBody)
        });

        const data = await response.json();

        // 검증 실패나 권한 오류 메시지 표시
        if (!response.ok) {
            showManagementMessage(data.message || '직원 정보를 수정하지 못했습니다.', 'error');
            return;
        }

        // 저장 성공 메시지 표시 후 조직도 데이터 새로고침
        showManagementMessage('직원 정보가 수정되었습니다.', 'success');
        await loadOrganization();

        // 짧은 성공 메시지 노출 후 모달 닫기
        setTimeout(closeManagementModal, 500);
    } catch (error) {
        // 서버 통신 실패 메시지 표시
        showManagementMessage('서버와 통신하지 못했습니다.', 'error');
    } finally {
        // 저장 요청 종료 후 버튼 다시 활성화
        saveButton.disabled = false;
    }
}

/* 직원 관리 모달 메시지 표시 처리 */
function showManagementMessage(message, type) {
    // 메시지 문구와 상태 클래스 반영
    const messageBox = document.getElementById('management-message');
    messageBox.innerText = message;
    messageBox.className = 'modal-message ' + type;
}

/* 직원 관리 모달 메시지 초기화 처리 */
function hideManagementMessage() {
    // 메시지 문구와 상태 클래스 초기화
    const messageBox = document.getElementById('management-message');
    messageBox.innerText = '';
    messageBox.className = 'modal-message';
}

/* 현재 필터 조건에 맞는 직원 목록 반환 처리 */
function getFilteredEmployees() {
    // 검색어를 소문자로 변환해 비교 준비
    const keyword = getSearchKeyword();

    // 검색어와 선택 부서/상태를 모두 만족하는 직원만 반환
    return employees.filter(emp => {
        /*
         * 전체 조직도는 현재 근무 중인 ACTIVE 직원만 보여줌
         * 부서별 화면은 해당 부서 안에서 ACTIVE/ON_LEAVE를 상태 탭으로 검색 가능
         */
        const matchDept = selectedDepartment === 'ALL' || emp.department === selectedDepartment;
        const matchStatus = selectedDepartment === 'ALL'
            ? emp.status === 'ACTIVE'
            : (selectedStatus === 'ALL' || emp.status === selectedStatus);

        // 이름, 이메일, 사번, 부서명, 직급명, 상태명 검색 처리
        const matchText = [
            emp.name,
            emp.email,
            emp.employeeNo,
            departmentLabels[emp.department],
            positionLabels[emp.position],
            statusLabels[emp.status]
        ].some(t => (t || '').toLowerCase().includes(keyword));
        return matchDept && matchStatus && matchText;
    });
}

/* 직원 검색어 정규화 처리 */
function getSearchKeyword() {
    // 공백과 대소문자 차이 제거
    return document.getElementById('employee-search').value.trim().toLowerCase();
}

/* 부서별 재직자 수 계산 처리 */
function countByDepartment() {
    // 부서별 재직자 카운트 저장 객체
    const counts = {};

    // 조직도 전체 기준과 맞추기 위해 ACTIVE 직원만 카운트
    employees.filter(emp => emp.status === 'ACTIVE').forEach(emp => {
        counts[emp.department] = (counts[emp.department] || 0) + 1;
    });
    return counts;
}

/* 브라우저 로그인 정보 제거 처리 */
function clearTokens() {
    // 인증 토큰 제거
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');

    // 로그인 사용자 정보 제거
    localStorage.removeItem('employeeNo');
    localStorage.removeItem('employeeName');
    localStorage.removeItem('role');
}
