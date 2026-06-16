// 서버는 enum 코드(FRONTEND 등)를 내려주므로 화면에서는 한국어 표시명으로 변환
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

// 조직도 API에서 받아온 직원 목록을 저장하는 화면 전역 상태
let employees = [];
// 현재 선택된 부서 필터. ALL이면 전체 직원 표시
let selectedDepartment = 'ALL';
// 부서별 보기에서 사용할 재직 상태 필터. 전체 조직도에서는 ACTIVE만 강제 표시
let selectedStatus = 'ACTIVE';
// 현재 로그인 사용자의 시스템 역할. SUPER이면 직원 관리 모달 사용 가능
let currentUserRole = null;
// 오른쪽 직원 목록에서 펼쳐진 부서 섹션을 저장하는 화면 전역 상태
const expandedDepartments = new Set(Object.keys(departmentLabels));
// 왼쪽 조직도 트리의 전체 노드 펼침 여부
let organizationTreeExpanded = true;
// 왼쪽 조직도 트리에서 펼쳐진 부서 노드를 저장하는 화면 전역 상태
const expandedTreeDepartments = new Set(Object.keys(departmentLabels));
// 부서별 직원 카드 목록의 현재 페이지를 저장하는 화면 전역 상태
const departmentPageMap = {};
// 부서별 직원 카드 목록은 한 화면에 4명씩 표시
const DEPARTMENT_PAGE_SIZE = 4;

/* 조직도 화면 최초 진입 시 아이콘, 검색 이벤트, 로그인 사용자, 조직도 데이터를 초기화하는 메서드 */
document.addEventListener('DOMContentLoaded', async function () {
    // lucide 아이콘 초기화
    lucide.createIcons();

    // 검색어 입력 시 부서별 페이지를 첫 페이지로 돌린 뒤 조직도 다시 렌더링
    document.getElementById('employee-search').addEventListener('input', function () {
        resetDepartmentPages();
        renderOrganization();
    });

    // 현재 로그인 사용자 정보 조회
    await loadLoginUser();

    // 조직도 직원 목록 조회
    await loadOrganization();
});

/* 현재 로그인 사용자 정보를 조회하고 레이아웃 사용자 이름을 갱신하는 메서드 */
async function loadLoginUser() {
    /*
     * layout.html의 사용자 이름은 기본값이 "방문자"임
     * 현재 프로젝트는 JWT를 localStorage에 저장하므로 HTML 렌더링 시점에는 서버가 로그인 사용자를 알 수 없음
     * 그래서 화면이 열린 뒤 /api/auth/me를 호출해서 레이아웃의 이름 영역을 직접 갱신
     */
    const accessToken = localStorage.getItem('accessToken');

    // 토큰이 없으면 로그인 화면으로 이동
    if (!accessToken) {
        window.location.href = '/login';
        return;
    }

    try {
        // 현재 로그인 사용자 정보 조회 API 호출
        const response = await fetch('/api/auth/me', {
            headers: {
                'Authorization': 'Bearer ' + accessToken
            }
        });

        // 사용자 조회 실패 시 토큰을 정리하고 로그인 화면으로 이동
        if (!response.ok) {
            clearTokens();
            window.location.href = '/login';
            return;
        }

        // 응답에서 현재 사용자 역할과 이름을 화면 상태에 반영
        const data = await response.json();
        currentUserRole = data.role;
        const layoutUserName = document.getElementById('layout-user-name');
        if (layoutUserName) {
            layoutUserName.innerText = data.name;
        }
    } catch (error) {
        // 통신 실패 시 인증 상태를 초기화하고 로그인 화면으로 이동
        clearTokens();
        window.location.href = '/login';
    }
}

/* 조직도에 표시할 직원 목록을 서버에서 조회하는 메서드 */
async function loadOrganization() {
    // 조직도 API 인증에 사용할 accessToken 조회
    const accessToken = localStorage.getItem('accessToken');
    try {
        // 로그인 사용자가 접근 가능한 조직도 직원 목록 API 호출
        const response = await fetch('/api/management/employees/organization', {
            headers: {'Authorization': 'Bearer ' + accessToken}
        });
        if (!response.ok) throw new Error();

        // 서버 직원 목록을 화면 전역 상태에 저장
        employees = await response.json();

        // 이전 조회 실패 메시지가 남아 있으면 숨김 처리
        const errBox = document.getElementById('organization-error');
        errBox.innerText = '';
        errBox.classList.remove('open');

        // 데이터를 받은 뒤 탭, 트리, 카드 영역을 한 번에 다시 렌더링
        renderOrganization();
    } catch (error) {
        // 조회 실패 메시지를 화면에 표시
        const errBox = document.getElementById('organization-error');
        errBox.innerText = '데이터를 불러올 수 없습니다.';
        errBox.classList.add('open');
    }
}

/* 부서 탭, 트리, 상태 필터, 직원 카드 목록을 한 번에 다시 그리는 메서드 */
function renderOrganization() {
    // 조직도 화면의 주요 영역은 같은 employees 데이터를 공유
    renderDepartmentTabs();
    renderOrganizationTree();
    renderStatusFilter();
    renderMemberGrid();

    // 새로 삽입된 아이콘 요소를 lucide SVG로 변환
    lucide.createIcons();
}

/* 부서별 직원 수를 계산해 가로 탭 UI를 렌더링하는 메서드 */
function renderDepartmentTabs() {
    // 부서 탭을 삽입할 DOM 조회
    const deptContainer = document.getElementById('dept-list');

    // 부서별 재직자 수와 전체 재직자 수 계산
    const counts = countByDepartment();
    const activeEmployeeCount = employees.filter(emp => emp.status === 'ACTIVE').length;

    // 조직도 전체 탭과 각 부서 탭 HTML 생성
    let html = createTabHtml('ALL', '조직도 전체', activeEmployeeCount);
    Object.keys(departmentLabels).forEach(key => {
        html += createTabHtml(key, departmentLabels[key], counts[key] || 0);
    });

    // 생성한 탭 HTML을 화면에 반영
    deptContainer.innerHTML = html;
}

/* 부서 탭 버튼 하나의 HTML 문자열을 만드는 메서드 */
function createTabHtml(dept, label, count) {
    // 선택된 부서라면 active 클래스로 현재 필터를 시각적으로 표시
    const active = selectedDepartment === dept ? ' active' : '';
    return `
        <button type="button" class="dept-tab${active}" onclick="selectDepartment('${dept}')">
            ${label}
            <span class="dept-count">${count}</span>
        </button>
    `;
}

/* 가로 탭이나 트리에서 선택한 부서 기준으로 조직도 필터를 바꾸는 메서드 */
function selectDepartment(dept) {
    // 선택된 부서를 화면 상태에 저장
    selectedDepartment = dept;

    // 전체 조직도는 항상 재직자만 보여주는 정책이므로 ACTIVE로 고정
    // 특정 부서를 선택하면 재직/휴직을 함께 살펴볼 수 있도록 전체 상태 필터로 시작
    selectedStatus = dept === 'ALL' ? 'ACTIVE' : 'ALL';

    // 이전에 직원 검색 또는 트리 직원 클릭으로 검색어가 남아 있으면
    // 새로 선택한 부서의 직원 목록도 그 검색어로 계속 좁혀짐
    // 부서 전환은 "새 부서 전체를 보겠다"는 동작이므로 검색어 초기화
    document.getElementById('employee-search').value = '';

    // 특정 부서를 선택하면 해당 부서 섹션이 펼쳐진 상태로 보이게 처리
    if (dept !== 'ALL') {
        expandedDepartments.add(dept);
        organizationTreeExpanded = true;
        expandedTreeDepartments.add(dept);
    }
    resetDepartmentPages();
    renderOrganization();
}

/* 전체 > 부서 > 직원 형태의 조직도 트리 UI를 렌더링하는 메서드 */
function renderOrganizationTree() {
    // 트리를 삽입할 DOM과 부서별 재직자 카운트 조회
    const treeContainer = document.getElementById('organization-tree');
    const counts = countByDepartment();
    const activeEmployeeCount = employees.filter(emp => emp.status === 'ACTIVE').length;

    // 전체 조직 토글 아래에 부서와 직원을 계층형으로 렌더링
    treeContainer.innerHTML = createTreeRootHtml(activeEmployeeCount, counts);
}

/* 트리 최상단의 OfficeMate 전체 버튼 HTML을 만드는 메서드 */
function createTreeRootHtml(count, counts) {
    // 전체 조직도 선택 상태면 active 클래스 추가
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

/* 트리의 부서 노드와 직원 자식 노드 HTML을 함께 만드는 메서드 */
function createTreeDepartmentHtml(dept, label, count, deptEmployees) {
    // 선택된 부서면 active 클래스 추가
    const active = selectedDepartment === dept ? ' active' : '';
    const expanded = expandedTreeDepartments.has(dept);
    const iconName = expanded ? 'chevron-down' : 'chevron-right';

    // 부서에 속한 직원 자식 노드 HTML 생성
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

/* 왼쪽 트리의 전체 조직 노드를 접거나 펼치는 메서드 */
function toggleOrganizationTree(event) {
    // 토글 버튼 클릭이 상위 선택 이벤트로 번지지 않도록 차단
    event.stopPropagation();

    // 전체 조직 노드 펼침 상태 전환
    organizationTreeExpanded = !organizationTreeExpanded;
    renderOrganization();
}

/* 왼쪽 트리의 부서 노드를 접거나 펼치는 메서드 */
function toggleTreeDepartment(event, department) {
    // 토글 버튼 클릭이 부서 선택 이벤트로 번지지 않도록 차단
    event.stopPropagation();

    // 펼쳐져 있으면 접고, 접혀 있으면 펼침
    if (expandedTreeDepartments.has(department)) {
        expandedTreeDepartments.delete(department);
    } else {
        expandedTreeDepartments.add(department);
    }

    renderOrganization();
}

/* 트리 안에서 직원 한 명을 버튼 HTML로 만드는 메서드 */
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

/* 트리에서 선택한 직원이 바로 보이도록 부서와 검색어를 설정하는 메서드 */
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

/* 현재 부서, 상태, 검색어 필터를 반영해 직원 카드 목록을 렌더링하는 메서드 */
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

    // 필터링된 직원을 부서별로 묶어 접이식 섹션 HTML로 렌더링
    grid.innerHTML = createDepartmentSectionsHtml(filtered);
}

/* 필터링된 직원 목록을 부서별 접이식 섹션 HTML로 변환하는 메서드 */
function createDepartmentSectionsHtml(filteredEmployees) {
    // 직원 목록을 department 코드 기준으로 그룹핑
    const groupedEmployees = groupEmployeesByDepartment(filteredEmployees);

    // 전체 조직도에서는 모든 부서를 순서대로 보여주고, 특정 부서 선택 시 해당 부서만 보여줌
    const departments = selectedDepartment === 'ALL'
        ? Object.keys(departmentLabels)
        : [selectedDepartment];

    // 검색 중이면 검색 결과가 있는 부서를 자동으로 펼침
    const keyword = getSearchKeyword();

    return departments
        .map(function (department) {
            // 현재 부서에 해당하는 직원 목록 조회
            const departmentEmployees = groupedEmployees[department] || [];

            // 직원이 없는 부서는 화면에 표시하지 않음
            if (departmentEmployees.length === 0) {
                return '';
            }

            // 검색어가 있으면 결과가 있는 부서는 펼침, 검색어가 없으면 사용자가 토글한 상태 사용
            const expanded = keyword ? true : expandedDepartments.has(department);
            return createDepartmentSectionHtml(department, departmentEmployees, expanded);
        })
        .join('');
}

/* 직원 목록을 부서 코드 기준 객체로 묶는 메서드 */
function groupEmployeesByDepartment(items) {
    // 부서 코드를 key로, 해당 부서 직원 배열을 value로 저장
    return items.reduce(function (groups, employee) {
        const department = employee.department;
        groups[department] = groups[department] || [];
        groups[department].push(employee);
        return groups;
    }, {});
}

/* 부서 하나의 접이식 직원 목록 섹션 HTML을 만드는 메서드 */
function createDepartmentSectionHtml(department, departmentEmployees, expanded) {
    // 부서명과 상태별 인원 수 계산
    const departmentName = departmentLabels[department] || department;
    const activeCount = departmentEmployees.filter(emp => emp.status === 'ACTIVE').length;
    const leaveCount = departmentEmployees.filter(emp => emp.status === 'ON_LEAVE').length;
    const totalPages = Math.ceil(departmentEmployees.length / DEPARTMENT_PAGE_SIZE);
    const currentPage = getValidDepartmentPage(department, totalPages);
    const startIndex = currentPage * DEPARTMENT_PAGE_SIZE;
    const pagedEmployees = departmentEmployees.slice(startIndex, startIndex + DEPARTMENT_PAGE_SIZE);

    // 펼침 상태에 따라 아이콘과 본문 노출 여부 결정
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

/* 부서별 직원 목록의 현재 페이지 값을 유효한 범위로 보정하는 메서드 */
function getValidDepartmentPage(department, totalPages) {
    // 페이지가 없거나 전체 페이지가 0이면 첫 페이지로 보정
    if (!departmentPageMap[department] || totalPages === 0) {
        departmentPageMap[department] = 0;
        return 0;
    }

    // 필터 변경 등으로 현재 페이지가 마지막 페이지를 넘으면 마지막 페이지로 보정
    if (departmentPageMap[department] >= totalPages) {
        departmentPageMap[department] = totalPages - 1;
    }

    return departmentPageMap[department];
}

/* 부서별 직원 목록 하단의 페이지 이동 UI를 만드는 메서드 */
function createDepartmentPaginationHtml(department, currentPage, totalPages) {
    // 4명 이하인 부서도 1 / 1 페이지 정보를 보여주기 위해 최소 1페이지로 표시
    const displayTotalPages = Math.max(totalPages, 1);

    // 첫 페이지와 마지막 페이지에서는 각각 이전/다음 버튼을 비활성화
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

/* 부서별 직원 카드 목록의 페이지를 이전/다음으로 이동하는 메서드 */
function moveDepartmentPage(department, delta) {
    // 현재 부서의 필터링된 직원 수를 기준으로 전체 페이지 수 계산
    const departmentEmployees = getFilteredEmployees().filter(emp => emp.department === department);
    const totalPages = Math.ceil(departmentEmployees.length / DEPARTMENT_PAGE_SIZE);
    const currentPage = getValidDepartmentPage(department, totalPages);
    const nextPage = currentPage + delta;

    // 조회 가능한 페이지 범위를 벗어나면 이동하지 않음
    if (nextPage < 0 || nextPage >= totalPages) {
        return;
    }

    // 부서별 현재 페이지를 저장하고 화면 갱신
    departmentPageMap[department] = nextPage;
    renderOrganization();
}

/* 부서별 직원 카드 페이지를 모두 첫 페이지로 초기화하는 메서드 */
function resetDepartmentPages() {
    // 검색어, 부서, 상태 필터가 바뀌면 기존 페이지 위치가 어긋날 수 있으므로 초기화
    Object.keys(departmentPageMap).forEach(function (department) {
        departmentPageMap[department] = 0;
    });
}

/* 부서별 직원 목록 섹션의 펼침/접힘 상태를 전환하는 메서드 */
function toggleDepartmentSection(department) {
    // 펼쳐져 있으면 접고, 접혀 있으면 펼침
    if (expandedDepartments.has(department)) {
        expandedDepartments.delete(department);
    } else {
        expandedDepartments.add(department);
    }

    // 변경된 펼침 상태를 화면에 반영
    renderOrganization();
}

/* 부서 선택 상태에 따라 재직/휴직 상태 필터를 표시하거나 숨기는 메서드 */
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

    // 전체 조직도에서는 상태 필터를 숨기고 ACTIVE로 강제
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

    // 상태 요약과 상태 필터 탭 HTML 반영
    summary.innerText = '재직 ' + activeCount + '명 · 휴직 ' + leaveCount + '명';
    tabs.innerHTML = [
        createStatusTabHtml('ALL', '전체', departmentEmployees.length),
        createStatusTabHtml('ACTIVE', '재직', activeCount),
        createStatusTabHtml('ON_LEAVE', '휴직', leaveCount)
    ].join('');
    panel.classList.add('open');
}

/* 부서별 재직 상태 필터 버튼 HTML을 만드는 메서드 */
function createStatusTabHtml(status, label, count) {
    // 선택된 상태는 active 클래스로 강조
    const active = selectedStatus === status ? ' active' : '';
    return `
        <button type="button" class="status-tab${active}" onclick="selectStatus('${status}')">
            ${label} ${count}
        </button>
    `;
}

/* 부서별 직원 목록에서 재직/휴직 상태 필터를 변경하는 메서드 */
function selectStatus(status) {
    // 선택된 상태 필터를 화면 상태에 저장
    selectedStatus = status;

    // 변경된 상태 기준으로 조직도 다시 렌더링
    resetDepartmentPages();
    renderOrganization();
}

/* 직원 객체 하나를 카드 HTML 문자열로 변환하는 메서드 */
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

/* SUPER 사용자가 선택한 직원의 관리 정보 수정 모달을 여는 메서드 */
function openManagementModal(employeeNo) {
    // 선택된 직원 정보 조회
    const employee = employees.find(emp => emp.employeeNo === employeeNo);

    // 직원이 없거나 SUPER 권한이 아니면 모달 열기 중단
    if (!employee || currentUserRole !== 'SUPER') {
        return;
    }

    // 직원 현재 정보를 모달 입력값에 채움
    document.getElementById('management-employee-no').value = employee.employeeNo;
    document.getElementById('management-name').value = employee.name || '';
    document.getElementById('management-department').value = employee.department;
    document.getElementById('management-position').value = employee.position;
    document.getElementById('management-role').value = employee.role;
    document.getElementById('management-status').value = employee.status;
    document.getElementById('management-resigned-on').value = employee.resignedOn || '';
    hideManagementMessage();
    toggleResignedOnField();

    // 모달을 열림 상태로 변경
    const modal = document.getElementById('employee-management-modal');
    modal.classList.add('open');
    modal.setAttribute('aria-hidden', 'false');

    // 모달 내부 아이콘을 lucide SVG로 변환
    lucide.createIcons();
}

/* 직원 관리 정보 수정 모달을 닫는 메서드 */
function closeManagementModal() {
    // 모달을 닫힘 상태로 변경
    const modal = document.getElementById('employee-management-modal');
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');

    // 모달 메시지 초기화
    hideManagementMessage();
}

/* 재직 상태가 퇴사일 때만 퇴사일 입력 필드를 표시하는 메서드 */
function toggleResignedOnField() {
    // 현재 선택된 재직 상태와 퇴사일 필드 조회
    const status = document.getElementById('management-status').value;
    const field = document.getElementById('resigned-on-field');

    // 퇴사 상태일 때만 퇴사일 입력 필드 열기
    field.classList.toggle('open', status === 'RESIGNED');
}

/* 직원 관리 정보 수정 내용을 서버에 저장하는 메서드 */
async function saveManagedEmployee(event) {
    // form 기본 제출을 막고 fetch 기반 저장 사용
    event.preventDefault();

    // 저장 API 호출에 필요한 값 조회
    const accessToken = localStorage.getItem('accessToken');
    const employeeNo = document.getElementById('management-employee-no').value;
    const status = document.getElementById('management-status').value;
    const saveButton = document.getElementById('management-save-button');

    // 중복 제출 방지를 위해 저장 버튼 비활성화
    saveButton.disabled = true;
    hideManagementMessage();

    // 서버가 받는 직원 관리 정보 수정 요청 body 생성
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
        // 직원 관리 정보 수정 API 호출
        const response = await fetch('/api/management/employees/' + employeeNo + '/management', {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + accessToken
            },
            body: JSON.stringify(requestBody)
        });

        // 서버 응답 JSON 변환
        const data = await response.json();

        // 검증 실패나 권한 오류가 있으면 모달 메시지에 표시
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

/* 직원 관리 모달에 성공/오류 메시지를 표시하는 메서드 */
function showManagementMessage(message, type) {
    // 메시지 영역에 문구와 상태 클래스 반영
    const messageBox = document.getElementById('management-message');
    messageBox.innerText = message;
    messageBox.className = 'modal-message ' + type;
}

/* 직원 관리 모달 메시지를 초기화하는 메서드 */
function hideManagementMessage() {
    // 메시지 문구와 상태 클래스를 기본값으로 초기화
    const messageBox = document.getElementById('management-message');
    messageBox.innerText = '';
    messageBox.className = 'modal-message';
}

/* 현재 검색어, 부서, 재직 상태 조건에 맞는 직원 목록을 반환하는 메서드 */
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

        // 이름, 이메일, 사번, 부서명, 직급명, 상태명 중 검색어 포함 여부 확인
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

/* 직원 검색어 입력값을 비교용 소문자 문자열로 정규화하는 메서드 */
function getSearchKeyword() {
    // 검색어 앞뒤 공백을 제거하고 대소문자 차이를 없애기 위해 소문자로 변환
    return document.getElementById('employee-search').value.trim().toLowerCase();
}

/* 부서별 재직자 수를 객체 형태로 계산하는 메서드 */
function countByDepartment() {
    // 부서별 재직자 카운트를 저장할 객체 생성
    const counts = {};

    // 조직도 전체 기준과 맞추기 위해 ACTIVE 직원만 카운트함
    employees.filter(emp => emp.status === 'ACTIVE').forEach(emp => {
        counts[emp.department] = (counts[emp.department] || 0) + 1;
    });
    return counts;
}

/* 토큰이 없거나 만료된 상태에서 브라우저 저장소를 정리하는 메서드 */
function clearTokens() {
    // 인증 토큰 제거
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');

    // 로그인 사용자 정보 제거
    localStorage.removeItem('employeeNo');
    localStorage.removeItem('employeeName');
    localStorage.removeItem('role');
}
