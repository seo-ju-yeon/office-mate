/* 채팅 서버 접속 정보 설정 */
const serverIP = window.location.hostname;
const serverPort = "8080";

/* STOMP 웹소켓 클라이언트 설정 */
const stompClient = new StompJs.Client({
    brokerURL: `ws://${serverIP}:${serverPort}/stomp/chat`,

    // Spring Security 적용 시 웹소켓 Handshake 인증 누락 방지를 위한 JWT 헤더 처리
    connectHeaders: {
        "Authorization": "Bearer " + (localStorage.getItem("accessToken") || "")
    },

    reconnectDelay: 5000,
});

/* 채팅 화면 전역 상태 */
let currentOffset = 30; // 이전 메시지 추가 조회 시작 위치
let isLoading = false; // 이전 메시지 조회 중복 방지 상태
let isFull = false; // 더 조회할 메시지가 없는 상태
let draftTimer; // 입력 중 임시저장 지연 처리 타이머
let summaryMode = false; // AI 요약 메시지 선택 모드 상태
let summaryRangeAnchorId = null; // 요약 범위 선택 기준 메시지 ID

const messageInput = document.getElementById('messageInput');
const aiHighlightLayer = document.getElementById('aiHighlightLayer');
const sendBtn = document.getElementById('sendBtn');
const chatMessages = document.getElementById('chatMessages');
const summaryModeBtn = document.getElementById('summaryModeBtn');
const summarySubmitBtn = document.getElementById('summarySubmitBtn');
const summaryCancelBtn = document.getElementById('summaryCancelBtn');

/* 직원 목록 사이드바 렌더링 처리 */
async function loadUserList() {
    const userList = document.getElementById('userList');
    if (!userList) return;

    try {
        const response = await fetch('/api/management/employees/chat', {
            headers: {"Authorization": "Bearer " + localStorage.getItem("accessToken")}
        });
        const users = await response.json();

        userList.innerHTML = '';

        users.forEach(user => {
            // 본인 계정은 대화 상대 목록에서 제외
            if (user.employeeNo === window.currentUserNo) return;

            const userItem = document.createElement('div');
            userItem.className = 'user-item';
            userItem.setAttribute('data-employee-no', user.employeeNo);

            // 직원 항목 클릭 시 1:1 채팅방 이동 이벤트 연결
            userItem.onclick = (e) => handleUserClick(userItem, e);

            userItem.innerHTML = `
                <input type="checkbox" class="group-check" value="${user.employeeNo}" 
                       style="margin: 0 10px 0 15px;" onclick="event.stopPropagation();">
                <div class="user-info" onclick="handleUserClick(this.parentElement, event)">
                    <div class="user-main">
                        <span class="user-name">${user.name}</span>
                        <span class="status-indicator offline"></span>
                    </div>
                    <div class="user-sub">
                        <span class="user-dept">${user.department || ''}</span>
                        <span class="user-badge">${user.position || ''}</span>
                    </div>
                </div>
            `;
            userList.appendChild(userItem);
        });
    } catch (error) {
        console.error("직원 목록 로드 실패:", error);
    }
}

/* 1:1 채팅방 생성 또는 이동 처리 */
function handleUserClick(element, event) {
    if (event) event.stopPropagation();

    const employeeNo = element.getAttribute('data-employee-no');

    console.log("선택된 직원 사번:", employeeNo);

    // 1:1 채팅방 생성 또는 기존 방 조회 요청
    fetch('/api/chat/room/group', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + localStorage.getItem("accessToken")
        },
        body: JSON.stringify({
            roomName: null,
            employeeNos: [currentUserNo, employeeNo]
        })
    })
        .then(res => res.json())
        .then(data => {
            if (data.roomId) {
                // 응답받은 채팅방 번호로 채팅 화면 이동
                window.location.href = `/api/chat/room?roomId=${data.roomId}&targetNo=${employeeNo}`;
            }
        });
}

/* 메시지 말풍선 엘리먼트 생성 처리 */
function createMessageElement(msg, isMine) {
    const messageDiv = document.createElement('div');
    const isAiGenerated = Boolean(msg.aiGenerated || msg.isAiGenerated);
    messageDiv.className = `message ${isAiGenerated ? 'ai received' : (isMine ? 'sent' : 'received')}`;

    if (msg.id) {
        messageDiv.setAttribute('data-msg-id', msg.id);
    }

    const date = msg.sentAt ? new Date(msg.sentAt) : new Date();
    messageDiv.setAttribute('data-sent-at', date.toISOString());
    messageDiv.setAttribute('data-date-key', getDateKey(date));

    const sentTime = date.toLocaleTimeString('ko-KR', {
        hour: '2-digit', minute: '2-digit', hour12: true
    });

    // 읽지 않은 인원 수는 0보다 클 때만 표시
    const isAiCalled = Boolean(msg.aiCalled || msg.isAiCalled);
    const isAiMessage = isAiCalled || isAiGenerated;
    const unreadCount = isAiMessage ? 0 : ((msg.unreadCount !== undefined) ? msg.unreadCount : 0);
    const unreadHidden = (isAiMessage || unreadCount <= 0) ? 'hidden' : '';

    const senderName = isAiGenerated ? 'AI 챗봇' : (msg.senderName || '상대방');
    const safeContent = escapeHtml(msg.content || '').replace(/\n/g, '<br>');

    messageDiv.innerHTML = `
        ${(!isMine || isAiGenerated) ? `<div class="sender">${escapeHtml(senderName)}</div>` : ''}
        <div class="message-info">
            <span class="unread-mark ${unreadHidden}">${unreadCount > 0 ? unreadCount : ''}</span>
            <div class="timestamp">${sentTime}</div>
            <div class="message-content">${safeContent}</div>
        </div>
        `;
    if (summaryMode) {
        addSummaryCheckbox(messageDiv);
    }

    return messageDiv;
}

/* HTML 특수 문자 이스케이프 처리 */
function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/* @AI 멘션 하이라이트 마크업 처리 */
function renderAiMentionHighlight(value) {
    return escapeHtml(value || '')
        .replace(/(^|\s)(@AI)/g, '$1<mark>$2</mark>')
        .replace(/\n$/g, '\n\u200b');
}

/* 입력창 @AI 하이라이트 레이어 동기화 처리 */
function syncAiMentionHighlight() {
    if (!messageInput || !aiHighlightLayer) return;

    const highlighted = renderAiMentionHighlight(messageInput.value);
    aiHighlightLayer.innerHTML = highlighted || '\u200b';
    aiHighlightLayer.scrollTop = messageInput.scrollTop;
    aiHighlightLayer.scrollLeft = messageInput.scrollLeft;
}

/* 메시지 날짜 구분 키 생성 처리 */
function getDateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/* 날짜 구분선 문구 변환 처리 */
function formatDateDivider(dateKey) {
    const [year, month, day] = dateKey.split('-').map(Number);
    const date = new Date(year, month - 1, day);
    return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        weekday: 'long'
    });
}

/* 메시지 목록 날짜 구분선 갱신 처리 */
function refreshDateDividers() {
    if (!chatMessages) return;

    chatMessages.querySelectorAll('.date-divider.generated').forEach(divider => divider.remove());

    let lastDateKey = null;
    Array.from(chatMessages.querySelectorAll('.message')).forEach(messageEl => {
        const dateKey = messageEl.getAttribute('data-date-key');
        if (!dateKey || dateKey === lastDateKey) return;

        const divider = document.createElement('div');
        divider.className = 'date-divider generated';
        divider.textContent = formatDateDivider(dateKey);
        chatMessages.insertBefore(divider, messageEl);
        lastDateKey = dateKey;
    });
}

/* 요약 모드 메시지 선택 체크박스 추가 처리 */
function addSummaryCheckbox(messageEl) {
    if (!messageEl || messageEl.querySelector('.summary-check')) return;

    const msgId = messageEl.getAttribute('data-msg-id');
    if (!msgId) return;

    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.className = 'summary-check';
    checkbox.value = msgId;
    checkbox.setAttribute('aria-label', '요약할 메시지 선택');
    checkbox.addEventListener('change', handleSummaryCheckboxChange);
    messageEl.classList.add('summary-selectable');
    messageEl.prepend(checkbox);
}

/* 요약 모드 체크박스 제거 처리 */
function removeSummaryCheckboxes() {
    document.querySelectorAll('.summary-check').forEach(checkbox => checkbox.remove());
    document.querySelectorAll('.message.summary-selectable').forEach(messageEl => {
        messageEl.classList.remove('summary-selectable');
    });
    summaryRangeAnchorId = null;
}

/* 요약 체크박스 선택 상태 변경 처리 */
function handleSummaryCheckboxChange(event) {
    const checkbox = event.target;
    if (!checkbox.checked) {
        summaryRangeAnchorId = null;
        return;
    }

    const currentId = checkbox.value;
    if (!summaryRangeAnchorId) {
        summaryRangeAnchorId = currentId;
        return;
    }

    selectSummaryRange(summaryRangeAnchorId, currentId);
}

/* 요약 대상 메시지 범위 선택 처리 */
function selectSummaryRange(startId, endId) {
    const checkboxes = Array.from(document.querySelectorAll('.message .summary-check'));
    const startIndex = checkboxes.findIndex(checkbox => checkbox.value === String(startId));
    const endIndex = checkboxes.findIndex(checkbox => checkbox.value === String(endId));

    if (startIndex < 0 || endIndex < 0) return;

    const from = Math.min(startIndex, endIndex);
    const to = Math.max(startIndex, endIndex);

    checkboxes.forEach((checkbox, index) => {
        checkbox.checked = index >= from && index <= to;
    });
}

/* 요약 모드 전환 및 버튼 상태 갱신 처리 */
function setSummaryMode(enabled) {
    summaryMode = enabled;

    if (summaryModeBtn) summaryModeBtn.style.display = enabled ? 'none' : '';
    if (summarySubmitBtn) summarySubmitBtn.style.display = enabled ? '' : 'none';
    if (summaryCancelBtn) summaryCancelBtn.style.display = enabled ? '' : 'none';

    if (enabled) {
        summaryRangeAnchorId = null;
        document.querySelectorAll('.message').forEach(addSummaryCheckbox);
        return;
    }

    removeSummaryCheckboxes();
}

/* 선택 메시지 AI 요약 요청 처리 */
async function submitSummary() {
    const selectedIds = Array.from(document.querySelectorAll('.summary-check:checked'))
        .map(checkbox => Number(checkbox.value))
        .filter(Boolean);

    if (selectedIds.length === 0) {
        alert('요약할 메시지를 선택해 주세요.');
        return;
    }

    if (!currentRoomId) {
        alert('채팅방 정보를 찾을 수 없습니다.');
        return;
    }

    if (summarySubmitBtn) summarySubmitBtn.disabled = true;

    try {
        const response = await fetch(`/api/ai/summary/${currentRoomId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + localStorage.getItem("accessToken")
            },
            body: JSON.stringify({messageIds: selectedIds}),
            credentials: 'include'
        });

        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || '요약에 실패했습니다.');
        }

        const summaryMessage = await response.json();
        appendMessageToUI(summaryMessage, false);
        setSummaryMode(false);
    } catch (error) {
        console.error('요약 실패:', error);
        alert(error.message || '요약 중 오류가 발생했습니다.');
    } finally {
        if (summarySubmitBtn) summarySubmitBtn.disabled = false;
    }
}

/* 새 메시지 화면 추가 처리 */
function appendMessageToUI(msg, isMine) {
    const noMsgNotice = document.getElementById('noMessageNotice');

    // 메시지가 추가되면 빈 대화 안내 문구 제거
    if (noMsgNotice) noMsgNotice.remove();

    if (!chatMessages) return;

    const messageDiv = createMessageElement(msg, isMine);

    chatMessages.appendChild(messageDiv);
    refreshDateDividers();
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

/* 현재 사용자 메시지 읽음 전송 처리 */
function sendReadReceipt() {
    // 방 번호가 없으면 읽음 이벤트 전송 중단
    if (!currentRoomId) return;

    // 사이드바가 대화창을 가린 상태면 읽음 처리 제외
    const sidebar = document.getElementById('userSidebar');
    if (sidebar && sidebar.classList.contains('active') && window.innerWidth < 600) {
        // 좁은 화면에서는 목록이 열려 있을 때 읽지 않은 상태로 유지
        return;
    }

    stompClient.publish({
        destination: `/pub/chat/${currentRoomId}/read`,
        body: currentUserNo
    });
}

/* 채팅방 항목 이동 처리 */
function handleRoomClick(element, event) {
    if (event) event.stopPropagation();

    const roomId = element.getAttribute('data-room-id');

    if (!roomId || roomId === 'null') {
        console.error("Room ID가 유효하지 않습니다.");
        return;
    }

    window.location.href = `/api/chat/room?roomId=${roomId}`;
}

/* 현재 채팅방 메시지 전송 처리 */
const sendMessage = () => {
    const content = messageInput.value.trim();

    // 내용이나 방 번호가 없으면 전송 중단
    if (!content || !currentRoomId) {
        console.error("방 번호가 없습니다. 전송을 중단합니다.");
        return;
    }

    if (content && stompClient && stompClient.connected) {
        stompClient.publish({
            destination: `/pub/chat/${currentRoomId}`,
            body: JSON.stringify({
                roomId: currentRoomId,
                senderNo: currentUserNo,
                senderName: currentUserName,
                content: content,
                messageType: "TEXT"
            })
        });

        messageInput.value = '';
        syncAiMentionHighlight();
        messageInput.focus();
    }

    // 전송 후 Redis 임시저장 내용 초기화
    fetch(`/api/chat/draft?roomId=${currentRoomId}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': 'Bearer ' + localStorage.getItem("accessToken")
        },
        body: JSON.stringify({content: ""}),
        credentials: 'include'
    });

    messageInput.focus();
}

/* 브라우저 포커스 복귀 시 읽음 상태 전송 처리 */
window.addEventListener('focus', () => {
    if (stompClient && stompClient.connected) {
        sendReadReceipt();
    }
});

/* 온라인 사용자 상태 표시 처리 */
function updateAllUserStatus(onlineList) {
    console.log("현재 온라인 사번들:", onlineList);

    // 모든 상태 표시점 오프라인 초기화
    document.querySelectorAll('.status-indicator').forEach(dot => {
        dot.classList.remove('online');
        dot.classList.add('offline');
    });

    if (!onlineList) return;

    // 온라인 목록에 포함된 사번만 온라인 표시
    onlineList.forEach(empNo => {
        const userItem = document.querySelector(`.user-item[data-employee-no="${empNo}"]`);

        if (userItem) {
            const dot = userItem.querySelector('.status-indicator');
            if (dot) {
                dot.classList.remove('offline');
                dot.classList.add('online');
            }
        }
    });
}

/* 직원 목록/채팅방 목록 탭 전환 처리 */
function switchChatTab(type) {
    const userList = document.getElementById('userList');
    const roomList = document.getElementById('roomList');
    const createArea = document.getElementById('groupCreateArea');
    const tabEmp = document.getElementById('tabEmp');
    const tabRoom = document.getElementById('tabRoom');

    if (type === 'EMP') {
        userList.style.display = 'block';
        createArea.style.display = 'block';
        roomList.style.display = 'none';
        tabEmp.classList.add('active');
        tabRoom.classList.remove('active');
    } else {
        userList.style.display = 'none';
        createArea.style.display = 'none';
        roomList.style.display = 'block';
        tabEmp.classList.remove('active');
        tabRoom.classList.add('active');
        loadChatRoomList();
    }
}

/* 참여 중인 채팅방 목록 조회 처리 */
function loadChatRoomList() {
    const roomList = document.getElementById('roomList');

    // 채팅방 목록 조회 요청
    fetch('/api/chat/rooms',
        {
            headers: {
                "Authorization": `Bearer ` + localStorage.getItem("accessToken")
            }
        })
        .then(res => res.json())
        .then(rooms => {
            roomList.innerHTML = '';

            rooms.forEach(room => {
                // 기존 사용자 항목 스타일을 활용해 채팅방 항목 생성
                const roomItem = document.createElement('div');
                roomItem.className = 'user-item';
                roomItem.setAttribute('data-room-id', room.roomId);

                // 클릭 시 해당 채팅방 번호로 이동
                roomItem.onclick = () => {
                    handleRoomClick(roomItem);
                };

                let displayTime = "";
                if (room.lastMessageTime) {
                    const date = new Date(room.lastMessageTime);
                    displayTime = date.toLocaleTimeString('ko-KR', {
                        hour: '2-digit',
                        minute: '2-digit',
                        hour12: true
                    });
                }

                const displayName = room.roomName || "그룹 채팅";
                const memberCountDisplay = room.memberCount > 2 ? ` [${room.memberCount}]` : "";
                const lastMsg = room.lastMessage || "대화 내용이 없습니다.";

                roomItem.innerHTML = `
                    <div class="user-info">
                        <div class="user-main">
                            <span class="user-name">${displayName}${memberCountDisplay}</span>
                            <span class="last-msg-time" style="font-size: 11px; color: #999; margin-left: auto;">
                                ${displayTime}
                            </span>
                        </div>
                        <div class="user-sub">
                            <span class="user-dept">${lastMsg}</span>
                        </div>
                    </div>
                `;
                roomList.appendChild(roomItem);
            });
        })
        .catch(err => {
            console.error("채팅방 목록 로드 중 에러:", err);
            roomList.innerHTML = '<div class="error-msg">목록을 불러오지 못했습니다.</div>';
        });
}

/* 그룹 채팅방 생성 요청 처리 */
function createGroupChat() {
    const roomNameInput = document.getElementById('groupRoomName');
    const roomName = roomNameInput.value.trim();
    const selectedNodes = document.querySelectorAll('.group-check:checked');

    if (!roomName) {
        alert("채팅방 이름을 입력해주세요.");
        return;
    }

    if (selectedNodes.length < 2) {
        alert("대화할 직원을 2명 이상 선택 해주세요.");
        return;
    }

    // 선택된 직원 체크박스에서 사번 목록 추출
    const employeeNos = Array.from(selectedNodes).map(node => node.value);

    fetch('/api/chat/room/group', {
        method: 'POST',
        headers: {
            "Content-Type": "application/json",
            'Authorization': 'Bearer ' + localStorage.getItem("accessToken")
        },
        body: JSON.stringify({
            roomName: roomName,
            employeeNos: employeeNos
        }),
        credentials: 'include'
    })
        .then(res => res.json())
        .then(data => {
            // 생성된 채팅방 번호로 이동
            if (data.roomId) {
                location.href = `/api/chat/room?roomId=${data.roomId}`;
            }
        });
}

/* 이전 메시지 추가 조회 처리 */
async function loadMoreMessages() {
    if (isLoading || isFull) return;
    isLoading = true;

    // 메시지 추가 후 스크롤 위치 유지를 위해 현재 높이 저장
    const previousHeight = chatMessages.scrollHeight;

    try {
        // 현재 채팅방 번호와 오프셋으로 이전 메시지 조회
        const response = await fetch(`/api/chat/messages?roomId=${currentRoomId}&offset=${currentOffset}`,
            {
                method: 'GET',
                headers: {
                    "Authorization": "Bearer " + localStorage.getItem("accessToken")
                },
                credentials: 'include'
            });
        const messages = await response.json();

        if (messages.length === 0) {
            isFull = true;
            return;
        }

        // 오래된 메시지가 위에 오도록 조회 결과를 역순 추가
        messages.reverse().forEach(msg => {
            const isMine = msg.senderNo === currentUserNo;
            const messageDiv = createMessageElement(msg, isMine);
            chatMessages.prepend(messageDiv);
        });
        refreshDateDividers();

        // 다음 조회를 위한 오프셋 증가
        currentOffset += 30;

        // 추가된 콘텐츠 높이만큼 스크롤 위치 보정
        chatMessages.scrollTop = chatMessages.scrollHeight - previousHeight;

    } catch (error) {
        console.error("이전 메시지 로드 실패:", error);
    } finally {
        isLoading = false;
    }
}

/* STOMP 연결 완료 후 실시간 채널 구독 처리 */
stompClient.onConnect = (frame) => {
    console.log('Connected: ' + frame);

    // 실시간 온라인 상태 채널 구독
    stompClient.subscribe('/sub/status', (message) => {
        console.log("실시간 상태 수신:", message.body);
        const onlineEmployeeNos = JSON.parse(message.body);
        updateAllUserStatus(onlineEmployeeNos);
    });

    // 현재 채팅방 번호가 있을 때만 메시지 채널 구독
    if (currentRoomId) {
        console.log(currentRoomId + "번 방");

        stompClient.subscribe(`/sub/chat/${currentRoomId}`, (message) => {
            const msg = JSON.parse(message.body);

            // 내가 보낸 일반 메시지 여부 확인
            const isMine = msg.senderNo === window.currentUserNo;

            // 메시지 방향을 결정해 화면 추가
            appendMessageToUI(msg, isMine);

            // 다른 사람이 보낸 메시지면 읽음 신호 전송
            if (!isMine && document.visibilityState === 'visible') {
                sendReadReceipt();
            }
        });

        stompClient.subscribe(`/sub/chat/${currentRoomId}/read`, (payload) => {
            const data = JSON.parse(payload.body);
            const oldReadId = Number(data.oldReadId || 0);
            const lastReadId = Number(data.lastReadMessageId || 0);

            // 화면 메시지 중 새로 읽힌 구간만 처리
            document.querySelectorAll('.message').forEach(msgEl => {
                const msgId = Number(msgEl.getAttribute('data-msg-id'));
                const unreadMark = msgEl.querySelector('.unread-mark');

                // 읽음 숫자가 표시 중이고 새 읽음 위치 안에 있는 메시지만 갱신
                if (msgId && unreadMark && !unreadMark.classList.contains('hidden')) {
                    if (msgId > oldReadId && msgId <= lastReadId) {
                        // 그룹 채팅에서 한 명이 읽었으므로 읽지 않은 수 1 감소
                        let currentCount = Number(unreadMark.innerText || 0);
                        if (!isNaN(currentCount) && currentCount > 0) {
                            const nextCount = currentCount - 1;
                            unreadMark.innerText = nextCount <= 0 ? '' : nextCount;
                            if (nextCount <= 0) unreadMark.classList.add('hidden');
                        }
                    }
                }
            });
        });

        // AI 질문과 답변은 요청자 전용 채널에서만 수신
        stompClient.subscribe(`/sub/chat/${currentRoomId}/ai/${window.currentUserNo}`, (message) => {
            const msg = JSON.parse(message.body);

            const isMine = msg.senderNo === window.currentUserNo;
            appendMessageToUI(msg, isMine);
        });

        sendReadReceipt();
    }
}

/* 웹소켓 연결 오류 기록 처리 */
stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
}

/* STOMP 브로커 오류 기록 처리 */
stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' +
        frame.headers['message']);
    console.error('Additional details: ' +
        frame.body);
}

/* 채팅 화면 초기화 처리 */
document.addEventListener('DOMContentLoaded', async () => {
    syncAiMentionHighlight();

    // 처음 열릴 때 채팅 화면 기준으로 창 크기 조정
    if (window.outerWidth !== 600) {
        window.resizeTo(600, 800);
    }

    // 브라우저 저장소에서 인증 토큰과 사번 조회
    const token = localStorage.getItem("accessToken");
    const empNo = localStorage.getItem("employeeNo");

    if (!token) return;

    if (token) localStorage.setItem("accessToken", token);
    if (empNo) localStorage.setItem("employeeNo", empNo);
    console.log("인증 정보 동기화 완료");

    try {
        // 내 직원 정보를 조회해 현재 사용자 사번 확정
        const myInfoRes = await fetch('/api/management/employees/me', {
            headers: {"Authorization": `Bearer ${token}`}
        });
        const myInfo = await myInfoRes.json();
        window.currentUserNo = myInfo.employeeNo;
        window.currentUserName = myInfo.name;

        // 직원 목록 로드
        await loadUserList();

        // URL 파라미터에서 채팅방 번호와 대상 사번 조회
        const urlParams = new URLSearchParams(window.location.search);
        const paramRoomId = urlParams.get('roomId');
        const paramTargetNo = urlParams.get('targetNo');

        // URL 파라미터가 있으면 전역 채팅 상태 반영
        if (paramRoomId) window.currentRoomId = paramRoomId;
        if (paramTargetNo) window.targetNo = paramTargetNo;

        if (window.currentRoomId || window.targetNo) {
            const initRes = await fetch(`/api/chat/room/init?roomId=${window.currentRoomId || ''}&targetNo=${window.targetNo || ''}`, {
                headers: {"Authorization": "Bearer " + token}
            });

            if (initRes.ok) {
                const data = await initRes.json();

                // 서버가 확정한 방 번호와 멤버 수를 전역 상태 반영
                window.currentRoomId = data.roomId;
                window.roomMemberCount = data.roomMemberCount;

                // 채팅방 이름 표시 영역 갱신
                const opponentNameEl = document.getElementById('opponentName');
                if (opponentNameEl) opponentNameEl.innerText = data.roomName || "알 수 없는 대화";

                // 초기 채팅 내역 렌더링
                if (data.chatHistory && chatMessages) {
                    chatMessages.innerHTML = '';
                    if (data.chatHistory.length === 0) {
                        // 메시지가 없으면 빈 대화 안내 문구 표시
                        chatMessages.innerHTML = '<div id="noMessageNotice" style="text-align: center; color: #999; margin-top: 50px;">대화 내용이 없습니다</div>';
                    } else {
                        data.chatHistory.forEach(msg => {
                            const isMine = msg.senderNo === window.currentUserNo;
                            const msgDiv = createMessageElement(msg, isMine);
                            chatMessages.appendChild(msgDiv);
                        });
                    }
                    refreshDateDividers();
                    chatMessages.scrollTop = chatMessages.scrollHeight;
                }

                if (data.draft && messageInput) {
                    messageInput.value = data.draft;
                    syncAiMentionHighlight();
                }
            }
        }

        // 방 번호와 사용자 정보 확정 후 STOMP 구독 시작
        if (!stompClient.connected) {
            stompClient.activate();
        }
    } catch (error) {
        console.error("데이터 로드 실패:", error);
    }

    const toggleBtn = document.getElementById('toggleUserList');
    const sidebar = document.getElementById('userSidebar');

    if (sidebar) {
        if (currentRoomId) {
            sidebar.classList.remove('active');
        } else {
            sidebar.classList.add('active');
        }
    }

    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('active');
        });
    }

    if (summaryModeBtn) {
        summaryModeBtn.addEventListener('click', () => setSummaryMode(true));
    }

    if (summarySubmitBtn) {
        summarySubmitBtn.addEventListener('click', submitSummary);
    }

    if (summaryCancelBtn) {
        summaryCancelBtn.addEventListener('click', () => setSummaryMode(false));
    }

    // 전송 버튼 클릭 이벤트 등록
    if (sendBtn) {
        sendBtn.addEventListener('click', () => {
            sendMessage();
        });
    }

    // Enter 전송과 Shift+Enter 줄바꿈 처리
    if (messageInput) {
        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        // 입력값 변경 시 자동 임시저장 예약
        messageInput.addEventListener('input', () => {
            const el = messageInput;

            const value = el.value;

            // @ai 입력을 @AI로 통일
            const normalized = value.replace(/@ai/gi, '@AI');

            if (normalized !== value) {
                const cursor = el.selectionStart;

                el.value = normalized;

                // 커서 위치 보정 처리
                requestAnimationFrame(() => {
                    el.setSelectionRange(cursor, cursor);
                });
            }

            syncAiMentionHighlight();
            clearTimeout(draftTimer);

            // 1초 동안 추가 입력이 없으면 서버 임시저장
            draftTimer = setTimeout(() => {
                if (!currentRoomId) return;

                fetch(`/api/chat/draft?roomId=${currentRoomId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        "Authorization": "Bearer " + localStorage.getItem("accessToken")
                    },
                    body: JSON.stringify({content: messageInput.value}),
                    credentials: 'include'
                });
            }, 1000);
        });

        messageInput.addEventListener('scroll', syncAiMentionHighlight);
    }

    // 상단 스크롤 도달 이벤트 등록
    if (chatMessages) {
        chatMessages.addEventListener('scroll', () => {
            // 맨 위에 도달했고 추가 조회가 가능하면 이전 메시지 조회
            if (chatMessages.scrollTop === 0 && !isLoading && !isFull) {
                loadMoreMessages();
            }
        });
    }

    // 초기 화면은 가장 최근 메시지가 보이도록 하단 이동
    setTimeout(() => {
        chatMessages.scrollTop = chatMessages.scrollHeight;
    }, 100);
});

window.addEventListener('focus', () => {
    if (stompClient && stompClient.connected && currentRoomId) {
        console.log("창 포커스 감지: 읽음 처리 전송");
        sendReadReceipt();
    }
});
