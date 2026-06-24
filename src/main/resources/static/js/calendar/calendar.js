/* Google Calendar 표준 색상 ID 매핑 */
const COLOR_MAP = {
    "1": "#a4bdfc", "2": "#7ae7bf", "3": "#dbadff", "4": "#ff887c",
    "5": "#fbd75b", "6": "#ffb878", "7": "#46d6db", "8": "#e1e1e1",
    "9": "#5484ed", "10": "#51b886", "11": "#dc2127"
};

/* 캘린더 화면 전역 상태 */
let calendar; // FullCalendar 인스턴스
let eventModal; // Bootstrap 모달 인스턴스
const currentScope = 'PERSONAL'; // 개인 일정 고정 범위
const currentEmpNo = localStorage.getItem('employeeNo');

/* 캘린더 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', function () {
    if (window.lucide) lucide.createIcons();

    const modalEl = document.getElementById('eventModal');
    eventModal = new bootstrap.Modal(modalEl);

    // 모달 닫힘 후 포커스 잔상 제거
    modalEl.addEventListener('hidden.bs.modal', function () {
        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur();
        }
        document.body.focus();
    });

    if (!currentEmpNo) {
        alert("로그인 정보가 없습니다.");
        return;
    }

    initColorPicker(); // 색상 선택 버튼 초기화
    initCalendar(); // 캘린더 초기화와 일정 조회
    fetchMyProjects(); // 참여 프로젝트 필터 조회
});

/* API 요청 공통 인증 헤더 생성 처리 */
function getAuthHeaders() {
    const token = localStorage.getItem('accessToken');
    const formatToken = token && !token.startsWith('Bearer ') ? `Bearer ${token}` : token;
    return {'Authorization': formatToken || ''};
}

/* 입력 날짜를 서버 전송 형식으로 변환 처리 */
function formatToOffsetDateTime(dateStr) {
    if (!dateStr) return null;
    const date = new Date(dateStr);
    const pad = (n) => n < 10 ? '0' + n : n;
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}+09:00`;
}

/* 색상 선택 버튼 초기화 처리 */
function initColorPicker() {
    const container = document.getElementById('colorPickerContainer');
    if (!container) return;
    container.innerHTML = '';
    Object.entries(COLOR_MAP).forEach(([id, hex]) => {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'color-dot-btn';
        btn.dataset.id = id;
        btn.style.backgroundColor = hex;
        btn.onclick = () => selectColor(id);
        container.appendChild(btn);
    });
}

/* 일정 색상 선택 처리 */
function selectColor(id) {
    const colorId = String(id);
    const colorInput = document.getElementById('eventColor');
    if (colorInput) colorInput.value = colorId;
    document.querySelectorAll('.color-dot-btn').forEach(btn => {
        btn.classList.toggle('active', btn.dataset.id === colorId);
    });
}

/* FullCalendar 초기화 및 일정 데이터 소스 설정 처리 */
function initCalendar() {
    const calendarEl = document.getElementById('calendar');
    calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: 'ko',
        editable: true,
        headerToolbar: {left: 'prev,next today', center: 'title', right: 'dayGridMonth'},

        // 개인 일정 목록 조회 후 FullCalendar 이벤트 형식으로 변환
        events: async function (info, successCallback, failureCallback) {
            try {
                const response = await axios.get(`/api/calendar/list/${currentScope}`, {
                    params: {
                        empNo: currentEmpNo,
                        start: info.startStr,
                        end: info.endStr
                    },
                    headers: getAuthHeaders()
                });

                const events = response.data.map(event => {
                    const cid = String(event.colorId || "9");
                    const hexColor = event.color || COLOR_MAP[cid] || "#5484ed";
                    return {
                        id: event.id,
                        title: event.title,
                        start: event.startsAt,
                        end: event.endsAt,
                        backgroundColor: hexColor,
                        borderColor: hexColor,
                        allDay: event.isAllDay,
                        extendedProps: {
                            description: event.description,
                            colorId: cid,
                            scope: event.scope
                        }
                    };
                });
                successCallback(events);

                // 일정 소스 로딩 직후 하단 리스트 동기화
                setTimeout(() => {
                    if (typeof updateEventList === 'function') updateEventList();
                }, 50);
            } catch (error) {
                failureCallback(error);
            }
        },

        /* 날짜 범위 변경 또는 이벤트 소스 변경 후 하단 리스트 동기화 처리 */
        datesSet: function () {
            setTimeout(() => {
                if (typeof updateEventList === 'function') updateEventList();
            }, 100);
        },

        dateClick: (info) => openModal(null, info.dateStr), // 날짜 클릭 시 등록 모달 표시
        eventClick: (info) => openModal(info.event), // 일정 클릭 시 수정 모달 표시
        eventDrop: handleEventChange, // 드래그 날짜 변경 처리
        eventResize: handleEventChange, // 일정 기간 조정 처리
        eventDidMount: function (info) {
            // 완료 업무는 취소선과 투명도 표시
            if (info.event.extendedProps.isTask && info.event.extendedProps.status === 'DONE') {
                const titleEl = info.el.querySelector('.fc-event-title');
                if (titleEl) {
                    titleEl.style.textDecoration = 'line-through';
                    titleEl.style.textDecorationColor = 'black';
                }
                info.el.style.opacity = '0.5';
            }
        }
    });
    calendar.render();
}

/* 일정 드래그/리사이즈 변경 요청 처리 */
async function handleEventChange(info) {
    const ev = info.event;
    // 프로젝트 업무는 읽기 전용 처리
    if (ev.extendedProps.scope === 'PROJECT' || ev.extendedProps.isTask) {
        alert("프로젝트 일정은 수정할 수 없습니다.");
        info.revert();
        return;
    }

    // 드래그로 과거 날짜 이동 방지
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (ev.start < today) {
        alert("과거 날짜로 일정을 이동할 수 없습니다.");
        info.revert();
        return;
    }

    const data = {
        id: ev.id,
        title: ev.title,
        description: ev.extendedProps.description,
        startsAt: formatToOffsetDateTime(ev.start),
        endsAt: formatToOffsetDateTime(ev.end || ev.start),
        colorId: ev.extendedProps.colorId,
        ownerNo: currentEmpNo,
        scope: ev.extendedProps.scope
    };
    try {
        await axios.put('/api/calendar', data, {headers: getAuthHeaders()});
        updateEventList();
    } catch (e) {
        alert("수정 실패");
        info.revert();
    }
}

/* 일정 등록/수정 모달 표시 처리 */
function openModal(event = null, dateStr = null) {
    const isTask = event && event.extendedProps.isTask;
    const isProject = event && event.extendedProps.scope === 'PROJECT';

    // 프로젝트 업무 일정은 삭제/저장 버튼 숨김
    document.getElementById('btnDelete').style.display = (event && !isTask && !isProject) ? 'block' : 'none';
    document.getElementById('btnSave').style.display = (isTask || isProject) ? 'none' : 'block';
    document.getElementById('projectTaskInfo').style.display = isTask ? 'block' : 'none';

    const inputs = ['eventTitle', 'eventDescription', 'eventStart', 'eventEnd'];

    // 오늘 날짜 기준 최소 선택 일시 생성
    const today = new Date();
    const minDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}T00:00`;

    // min 속성으로 과거 날짜 선택 방지
    document.getElementById('eventStart').min = minDate;
    document.getElementById('eventEnd').min = minDate;

    if (event) { // 수정 모드 데이터 채움
        document.getElementById('eventId').value = event.id;
        document.getElementById('eventTitle').value = event.title;
        document.getElementById('eventDescription').value = event.extendedProps.description || '';
        document.getElementById('eventStart').value = event.startStr.slice(0, 16);
        document.getElementById('eventEnd').value = event.endStr ? event.endStr.slice(0, 16) : '';
        selectColor(event.extendedProps.colorId || "9");

        // 프로젝트/업무 일정은 읽기 전용 처리
        inputs.forEach(id => document.getElementById(id).disabled = isProject || isTask);
        document.querySelectorAll('.color-dot-btn').forEach(btn => btn.style.pointerEvents = (isProject || isTask) ? 'none' : 'auto');

        if (isTask) {
            document.getElementById('taskStatus').innerText = event.extendedProps.status;
            document.getElementById('taskProgress').innerText = event.extendedProps.progressRate;
        }
    } else { // 신규 등록 모드 초기화
        inputs.forEach(id => document.getElementById(id).disabled = false);
        document.querySelectorAll('.color-dot-btn').forEach(btn => btn.style.pointerEvents = 'auto');
        document.getElementById('eventId').value = '';
        document.getElementById('eventTitle').value = '';
        document.getElementById('eventDescription').value = '';

        // 클릭한 날짜가 과거면 오늘 날짜로 보정
        const clickedDate = new Date(dateStr);
        today.setHours(0, 0, 0, 0);
        const targetDateStr = clickedDate < today ? minDate.slice(0, 10) : dateStr;

        document.getElementById('eventStart').value = targetDateStr + "T09:00";
        document.getElementById('eventEnd').value = targetDateStr + "T10:00";
        selectColor("9");
    }
    eventModal.show();
    setTimeout(() => {
        document.getElementById('eventTitle').focus();
    }, 200);
}

/* 리스트 일정 수정 모달 표시 처리 */
function openModalById(id) {
    const ev = calendar.getEventById(id);
    if (ev) openModal(ev);
}

/* 일정 등록/수정 저장 요청 처리 */
async function saveEvent() {
    const btnSave = document.getElementById('btnSave');
    const id = document.getElementById('eventId').value;
    if (btnSave) btnSave.blur();

    // 입력 요소와 값 조회
    const titleEl = document.getElementById('eventTitle');
    const descEl = document.getElementById('eventDescription');
    const startVal = document.getElementById('eventStart').value;
    const endVal = document.getElementById('eventEnd').value;

    const titleVal = titleEl ? titleEl.value.trim() : '';
    const descVal = descEl ? descEl.value.trim() : '';

    // 제목과 내용 공백 검증
    if (!titleVal) {
        alert("일정 제목을 입력해 주세요.");
        if (titleEl) titleEl.focus();
        return;
    }

    if (!descVal) {
        alert("일정 내용을 입력해 주세요.");
        if (descEl) descEl.focus();
        return;
    }

    // 시작/종료 일시 유효성 검증
    if (!startVal || !endVal) {
        alert("시작 일시와 종료 일시를 모두 입력해 주세요.");
        return;
    }

    const todayDate = new Date();
    todayDate.setHours(0, 0, 0, 0); // 오늘 날짜 자정 기준

    if (new Date(startVal) < todayDate || new Date(endVal) < todayDate) {
        alert("오늘 날짜보다 과거로 일정을 등록하거나 수정할 수 없습니다.");
        return;
    }

    if (new Date(endVal) <= new Date(startVal)) {
        alert("종료 일시가 시작 일시보다 빠르거나 같을 수 없습니다.");
        return;
    }

    // 저장 요청 데이터 구성
    const data = {
        id: id ? parseInt(id) : null,
        title: titleVal,
        description: descVal,
        startsAt: formatToOffsetDateTime(startVal),
        endsAt: formatToOffsetDateTime(endVal),
        colorId: document.getElementById('eventColor').value,
        ownerNo: currentEmpNo,
        scope: currentScope
    };

    try {
        if (id) {
            await axios.put('/api/calendar', data, {headers: getAuthHeaders()});
        } else {
            await axios.post('/api/calendar', data, {headers: getAuthHeaders()});
        }
        eventModal.hide();
        calendar.refetchEvents();
    } catch (e) {
        alert("저장에 실패했습니다.");
    }
}

/* 일정 삭제 요청 처리 */
async function deleteEvent(id = null) {
    const targetId = id || document.getElementById('eventId').value;
    if (!targetId || !confirm("삭제하시겠습니까?")) return;
    try {
        await axios.delete(`/api/calendar/${targetId}`, {headers: getAuthHeaders()});
        eventModal.hide();
        calendar.refetchEvents();
    } catch (e) {
        alert("삭제 실패");
    }
}

/* 하단 이달의 일정 리스트 갱신 처리 */
function updateEventList() {
    const listContent = document.getElementById('event-list-content');
    if (!listContent) return;

    // FullCalendar 전체 일정 중 개인 일정만 필터링
    const allEvents = calendar.getEvents();
    const personalEvents = allEvents.filter(ev => ev.extendedProps.scope === 'PERSONAL');

    // 상단 배지 건수를 개인 일정 개수로 갱신
    document.getElementById('event-count').innerText = personalEvents.length + "건";

    // 일정이 없으면 빈 상태 표시
    listContent.innerHTML = personalEvents.length ? '' : '<div class="text-muted p-3">일정이 없습니다.</div>';

    // 필터링된 개인 일정만 리스트에 렌더링
    personalEvents.forEach(ev => {
        const dotColor = ev.backgroundColor;
        const item = document.createElement('div');
        item.className = 'event-item d-flex align-items-center justify-content-between';

        // 개인 일정은 복사/수정/삭제 버튼 표시
        let actionButtons = `<div class="btn-group">
                    <button class="btn btn-sm btn-outline-info" onclick="copyEvent('${ev.id}')">복사</button>
                    <button class="btn btn-sm btn-outline-secondary" onclick="openModalById('${ev.id}')">수정</button>
                    <button class="btn btn-sm btn-outline-danger" onclick="deleteEvent('${ev.id}')">삭제</button>
                </div>`;

        item.innerHTML = `
                    <div class="d-flex align-items-center" onclick="openModalById('${ev.id}')" style="cursor:pointer; flex-grow:1;">
                        <div class="event-color-dot" style="background:${dotColor}"></div>
                        <div class="event-info">
                            <div class="fw-bold">${ev.title}</div>
                            <small class="text-muted">${new Date(ev.start).toLocaleString()}</small>
                        </div>
                    </div>
                    ${actionButtons}`;
        listContent.appendChild(item);
    });
}

/* 기존 일정 복사 등록 모달 표시 처리 */
function copyEvent(id) {
    const ev = calendar.getEventById(id);
    if (!ev) return;
    openModal(ev);
    document.getElementById('eventId').value = '';
    document.getElementById('eventTitle').value = ev.title + " (복사)";
    document.getElementById('btnDelete').style.display = 'none';
}

/* 참여 프로젝트 필터 목록 렌더링 처리 */
async function fetchMyProjects() {
    const list = document.getElementById('project-filter-list');
    if (!list) return;

    try {
        calendar.getEventSources().forEach(source => {
            if (source.id && source.id.startsWith('p-src-')) {
                source.remove();
            }
        });

        const res = await axios.get('/api/projects/tasks/my-projects', {
            params: {empNo: currentEmpNo},
            headers: getAuthHeaders()
        });

        const activeProjects = res.data.filter(p => {
            const status = (p.status || "").toUpperCase();
            return status !== 'ON_HOLD' && status !== 'DONE' && status !== 'CANCELED';
        });

        list.innerHTML = '';

        if (activeProjects.length === 0) {
            list.innerHTML = '<small class="text-muted">참여 중인 활성 프로젝트가 없습니다.</small>';
            return;
        }

        activeProjects.forEach((p, i) => {
            const color = ['#f4b400', '#dbadff', '#7ae7bf'][i % 3];
            const div = document.createElement('div');
            // 프로젝트 필터 항목 정렬 클래스 적용
            div.className = 'form-check d-flex align-items-center gap-1';

            // 프로젝트별 Google Calendar 내보내기 아이콘 표시
            div.innerHTML = `
                <input class="form-check-input" type="checkbox" value="${p.id}" id="chk-${p.id}" onchange="toggleTasks(this, '${color}')" checked>
                <label class="form-check-label me-1" for="chk-${p.id}" style="color:${color}; font-weight:500; cursor:pointer;">${p.name}</label>
                <span class="text-success ms-1" style="cursor:pointer;" onclick="exportProjectToGoogle('${p.id}', '${p.name}')" title="이 프로젝트 업무만 구글로 내보내기">
                    <i data-lucide="external-link" style="width:13px; height:13px; vertical-align:middle; margin-bottom:2px;"></i>
                </span>
            `;
            list.appendChild(div);

            const chkInput = div.querySelector(`#chk-${p.id}`);
            if (chkInput) {
                toggleTasks(chkInput, color);
            }
        });

        // 새로 추가된 Lucide 아이콘 렌더링
        if (window.lucide) lucide.createIcons();

    } catch (e) {
        console.error("프로젝트 목록 로드 실패:", e);
        list.innerHTML = '<small class="text-danger">목록을 불러오지 못했습니다.</small>';
    }
}

/* 특정 프로젝트 업무 Google Calendar 내보내기 요청 처리 */
async function exportProjectToGoogle(projectId, projectName) {
    if (!confirm(`'${projectName}' 프로젝트의 업무 일정을 구글 캘린더로 내보내시겠습니까?`)) return;

    try {
        // 프로젝트 전용 Google Calendar 내보내기 API 호출
        await axios.post(`/api/calendar/export/google/project`, null, {
            params: {
                empNo: currentEmpNo,
                projectId: projectId
            },
            headers: getAuthHeaders()
        });
        alert(`'${projectName}' 프로젝트 업무 내보내기 완료!`);
    } catch (e) {
        console.error(e);
        if (confirm("연동 정보가 없거나 만료되었습니다. 지금 구글 연동을 진행하시겠습니까?")) {
            connectGoogle();
        }
    }
}

/* 프로젝트 업무 일정 소스 등록과 제거 처리 */
function toggleTasks(chk, color) {
    const sourceId = `p-src-${chk.value}`;
    if (chk.checked) {
        calendar.addEventSource({
            id: sourceId,
            events: async function (info, successCallback, failureCallback) {
                try {
                    const response = await axios.get(`/api/projects/tasks/${chk.value}`, {
                        headers: getAuthHeaders(),
                        params: {
                            start: info.startStr,
                            end: info.endStr,
                            filter: 'active'
                        }
                    });

                    const tasks = response.data.map(t => {
                        const isDone = t.status === 'DONE';
                        return {
                            id: 'tk-' + t.id,
                            title: `${isDone ? '[완료] ' : '[업무] '}${t.title} (${t.assigneeName || '미지정'})`,
                            start: t.dueOn,
                            allDay: true,
                            backgroundColor: isDone ? '#A0A0A0' : color,
                            borderColor: isDone ? '#A0A0A0' : color,
                            extendedProps: {
                                ...t,
                                isTask: true,
                                scope: 'PROJECT'
                            }
                        };
                    });
                    successCallback(tasks);
                } catch (error) {
                    failureCallback(error);
                }
            }
        });
    } else {
        const src = calendar.getEventSourceById(sourceId);
        if (src) src.remove();
    }

    setTimeout(() => {
        updateEventList();
    }, 150);
}

/* 개인 일정 및 선택 프로젝트 업무 전체 내보내기 요청 처리 */
async function exportAllToGoogle() {
    if (!confirm("전체 개인 일정 및 선택된 프로젝트의 업무를 구글 캘린더로 내보내시겠습니까?")) return;

    const btn = event.currentTarget;
    btn.disabled = true;

    // 현재 선택된 프로젝트 ID 목록 수집
    const checkedBoxes = document.querySelectorAll('#project-filter-list .form-check-input:checked');
    const projectIds = Array.from(checkedBoxes).map(chk => chk.value);

    try {
        const response = await axios.post(
            `/api/calendar/export/google/all`,
            null, // 요청 본문 없이 params만 전달
            {
                // 선택 프로젝트 ID를 쉼표 문자열로 전달
                params: {
                    empNo: currentEmpNo,
                    projectIds: projectIds.join(',')
                },
                headers: getAuthHeaders() // 요청 설정에 인증 헤더 포함
            }
        );
        alert("내보내기 완료!");
    } catch (e) {
        console.error(e);
        if (confirm("연동 정보가 없거나 만료되었습니다. 지금 구글 연동을 진행하시겠습니까?")) {
            connectGoogle();
        }
    } finally {
        btn.disabled = false;
    }
}

/* Google OAuth 인증 팝업 및 연동 완료 메시지 처리 */
async function connectGoogle() {
    try {
        const res = await axios.get('/api/calendar/google-auth-url', {
            params: {empNo: currentEmpNo},
            headers: getAuthHeaders()
        });

        window.open(res.data, 'googleLogin', 'width=500,height=600');

        // 팝업창에서 보내는 연동 완료 메시지 대기
        const receiveMessage = (event) => {
            if (event.data === 'google-link-success') {
                alert("연동에 성공했습니다. 내보내기를 다시 시도해 주세요.");
                window.removeEventListener('message', receiveMessage);
            }
        };
        window.addEventListener('message', receiveMessage);
    } catch (err) {
        alert("인증 URL을 가져오지 못했습니다.");
    }
}
