/* 채팅 서버 접속 정보를 현재 브라우저 주소 기준으로 설정한다. */
const serverIP = window.location.hostname;
const serverPort = "8080";

/* STOMP 웹소켓 클라이언트를 생성한다. */
const stompClient = new StompJs.Client({
    brokerURL: `ws://${serverIP}:${serverPort}/stomp/chat`,

    connectHeaders: {
        "Authorization": "Bearer " + (localStorage.getItem("accessToken") || "")
    },

    reconnectDelay: 5000,
});

/* 채팅 화면에서 공유하는 상태 값을 선언한다. */
let currentOffset = 30;
let isLoading = false;
let isFull = false;
let draftTimer;
let summaryMode = false;
let summaryRangeAnchorId = null;

const messageInput = document.getElementById('messageInput');
const aiHighlightLayer = document.getElementById('aiHighlightLayer');
const sendBtn = document.getElementById('sendBtn');
const chatMessages = document.getElementById('chatMessages');
const summaryModeBtn = document.getElementById('summaryModeBtn');
const summarySubmitBtn = document.getElementById('summarySubmitBtn');
const summaryCancelBtn = document.getElementById('summaryCancelBtn');

/* 직원 목록을 조회해 사이드바에 렌더링한다. */
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
            // 본인 계정은 대화 상대 목록에서 제외한다.
            if (user.employeeNo === window.currentUserNo) return;

            const userItem = document.createElement('div');
            userItem.className = 'user-item';
            userItem.setAttribute('data-employee-no', user.employeeNo);

            // 직원 항목 클릭 시 1:1 채팅방으로 이동하도록 이벤트를 연결한다.
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

/* 직원 항목 클릭 시 1:1 채팅방을 생성하거나 기존 방으로 이동한다. */
function handleUserClick(element, event) {
    if (event) event.stopPropagation();

    const employeeNo = element.getAttribute('data-employee-no');

    console.log("선택된 직원 사번:", employeeNo);

    // 1:1 채팅방 생성 또는 기존 방 조회 API를 호출한다.
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
                // 응답받은 채팅방 번호로 채팅 화면을 이동한다.
                window.location.href = `/api/chat/room?roomId=${data.roomId}&targetNo=${employeeNo}`;
            }
        });
}

/* 메시지 데이터를 채팅 말풍선 엘리먼트로 변환한다. */
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

    // 읽지 않은 인원 수는 0보다 클 때만 표시한다.
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

function escapeHtml(value) {
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/* @AI 멘션에 하이라이트 마크업을 적용한다. */
function renderAiMentionHighlight(value) {
    return escapeHtml(value || '')
        .replace(/(^|\s)(@AI)/g, '$1<mark>$2</mark>')
        .replace(/\n$/g, '\n\u200b');
}

/* 입력창의 @AI 하이라이트 레이어를 입력값과 동기화한다. */
function syncAiMentionHighlight() {
    if (!messageInput || !aiHighlightLayer) return;

    const highlighted = renderAiMentionHighlight(messageInput.value);
    aiHighlightLayer.innerHTML = highlighted || '\u200b';
    aiHighlightLayer.scrollTop = messageInput.scrollTop;
    aiHighlightLayer.scrollLeft = messageInput.scrollLeft;
}

/* 메시지 날짜 구분에 사용할 날짜 키를 만든다. */
function getDateKey(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
}

/* 날짜 키를 한국어 날짜 구분선 문구로 변환한다. */
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

/* 메시지 목록의 날짜 구분선을 다시 계산해 표시한다. */
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

/* 요약 모드에서 메시지 선택 체크박스를 추가한다. */
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

/* 요약 모드 체크박스를 모두 제거한다. */
function removeSummaryCheckboxes() {
    document.querySelectorAll('.summary-check').forEach(checkbox => checkbox.remove());
    document.querySelectorAll('.message.summary-selectable').forEach(messageEl => {
        messageEl.classList.remove('summary-selectable');
    });
    summaryRangeAnchorId = null;
}

/* 요약 체크박스 선택 상태 변경을 처리한다. */
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

/* 두 메시지 사이의 요약 대상 범위를 선택한다. */
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

/* 요약 모드를 켜거나 끄고 관련 버튼 상태를 갱신한다. */
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

/* 선택한 메시지들을 서버에 보내 AI 요약을 요청한다. */
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

/* 새 메시지를 채팅 화면에 추가한다. */
function appendMessageToUI(msg, isMine) {
    const noMsgNotice = document.getElementById('noMessageNotice');

    // 메시지가 하나라도 추가되면 빈 대화 안내 문구를 제거한다.
    if (noMsgNotice) noMsgNotice.remove();

    if (!chatMessages) return;

    const messageDiv = createMessageElement(msg, isMine);

    chatMessages.appendChild(messageDiv);
    refreshDateDividers();
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

/* 현재 사용자가 채팅방 메시지를 읽었음을 서버에 알린다. */
function sendReadReceipt() {
    // 방 번호가 없으면 읽음 이벤트를 보내지 않는다.
    if (!currentRoomId) return;

    // 사이드바가 대화창을 가린 상태라면 읽음 처리하지 않는다.
    const sidebar = document.getElementById('userSidebar');
    if (sidebar && sidebar.classList.contains('active') && window.innerWidth < 600) {
        // 좁은 화면에서는 목록이 열려 있을 때 메시지를 읽지 않은 것으로 본다.
        return;
    }

    stompClient.publish({
        destination: `/pub/chat/${currentRoomId}/read`,
        body: currentUserNo
    });
}

/* 채팅방 항목 클릭 시 해당 채팅방으로 이동한다. */
function handleRoomClick(element, event) {
    if (event) event.stopPropagation();

    const roomId = element.getAttribute('data-room-id');

    if (!roomId || roomId === 'null') {
        console.error("Room ID가 유효하지 않습니다.");
        return;
    }

    window.location.href = `/api/chat/room?roomId=${roomId}`;
}

/* 입력창의 메시지를 현재 채팅방으로 전송한다. */
const sendMessage = () => {
    const content = messageInput.value.trim();

    // 내용이나 방 번호가 없으면 전송을 중단한다.
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

    // 전송 후 Redis 임시저장 내용을 비운다.
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

/* 브라우저 창이 다시 활성화되면 읽음 상태를 전송한다. */
window.addEventListener('focus', () => {
    if (stompClient && stompClient.connected) {
        sendReadReceipt();
    }
});

/* 온라인 사용자 목록을 화면 상태 표시점에 반영한다. */
function updateAllUserStatus(onlineList) {
    console.log("현재 온라인 사번들:", onlineList);

    // 모든 상태 표시점을 오프라인으로 초기화한다.
    document.querySelectorAll('.status-indicator').forEach(dot => {
        dot.classList.remove('online');
        dot.classList.add('offline');
    });

    if (!onlineList) return;

    // 온라인 목록에 포함된 사번만 온라인으로 변경한다.
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

/* 직원 목록 탭과 채팅방 목록 탭을 전환한다. */
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

/* 참여 중인 채팅방 목록을 서버에서 조회한다. */
function loadChatRoomList() {
    const roomList = document.getElementById('roomList');

    // 서버의 채팅방 목록 조회 API를 호출한다.
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
                // 기존 사용자 항목 스타일을 활용해 채팅방 목록 항목을 만든다.
                const roomItem = document.createElement('div');
                roomItem.className = 'user-item';
                roomItem.setAttribute('data-room-id', room.roomId);

                // 클릭 시 해당 채팅방 번호로 이동한다.
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

/* 선택한 직원들로 그룹 채팅방 생성을 요청한다. */
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

    // 선택된 직원 체크박스에서 사번 목록을 추출한다.
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
            // 생성된 채팅방 번호로 이동한다.
            if (data.roomId) {
                location.href = `/api/chat/room?roomId=${data.roomId}`;
            }
        });
}

/* 이전 메시지를 추가로 조회해 목록 상단에 붙인다. */
async function loadMoreMessages() {
    if (isLoading || isFull) return;
    isLoading = true;

    // 메시지 추가 후 스크롤 위치를 유지하기 위해 현재 높이를 저장한다.
    const previousHeight = chatMessages.scrollHeight;

    try {
        // 현재 채팅방 번호와 오프셋으로 이전 메시지를 조회한다.
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

        // 오래된 메시지가 위에 오도록 조회 결과를 역순으로 추가한다.
        messages.reverse().forEach(msg => {
            const isMine = msg.senderNo === currentUserNo;
            const messageDiv = createMessageElement(msg, isMine);
            chatMessages.prepend(messageDiv);
        });
        refreshDateDividers();

        // 다음 조회를 위해 오프셋을 증가시킨다.
        currentOffset += 30;

        // 추가된 콘텐츠 높이만큼 스크롤 위치를 보정한다.
        chatMessages.scrollTop = chatMessages.scrollHeight - previousHeight;

    } catch (error) {
        console.error("이전 메시지 로드 실패:", error);
    } finally {
        isLoading = false;
    }
}

/* STOMP 연결이 완료되면 상태와 채팅 채널을 구독한다. */
stompClient.onConnect = (frame) => {
    console.log('Connected: ' + frame);

    // 실시간 온라인 상태 채널을 구독한다.
    stompClient.subscribe('/sub/status', (message) => {
        console.log("실시간 상태 수신:", message.body);
        const onlineEmployeeNos = JSON.parse(message.body);
        updateAllUserStatus(onlineEmployeeNos);
    });

    // 현재 채팅방 번호가 있을 때만 메시지 채널을 구독한다.
    if (currentRoomId) {
        console.log(currentRoomId + "번 방");

        stompClient.subscribe(`/sub/chat/${currentRoomId}`, (message) => {
            const msg = JSON.parse(message.body);

            // 내가 보낸 일반 메시지인지 확인한다.
            const isMine = msg.senderNo === window.currentUserNo;

            // 메시지 방향을 결정해 화면에 추가한다.
            appendMessageToUI(msg, isMine);

            // 다른 사람이 보낸 메시지면 읽음 신호를 전송한다.
            if (!isMine && document.visibilityState === 'visible') {
                sendReadReceipt();
            }
        });

        stompClient.subscribe(`/sub/chat/${currentRoomId}/read`, (payload) => {
            const data = JSON.parse(payload.body);
            const oldReadId = Number(data.oldReadId || 0);
            const lastReadId = Number(data.lastReadMessageId || 0);

            // 화면의 모든 메시지를 순회하며 새로 읽힌 구간만 처리한다.
            document.querySelectorAll('.message').forEach(msgEl => {
                const msgId = Number(msgEl.getAttribute('data-msg-id'));
                const unreadMark = msgEl.querySelector('.unread-mark');

                // 읽음 숫자가 표시 중이고 새 읽음 위치 안에 있는 메시지만 갱신한다.
                if (msgId && unreadMark && !unreadMark.classList.contains('hidden')) {
                    if (msgId > oldReadId && msgId <= lastReadId) {
                        // 그룹 채팅에서 한 명이 읽었으므로 읽지 않은 수를 1 줄인다.
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

        // AI 질문과 답변은 요청자 전용 채널에서만 수신한다.
        stompClient.subscribe(`/sub/chat/${currentRoomId}/ai/${window.currentUserNo}`, (message) => {
            const msg = JSON.parse(message.body);

            const isMine = msg.senderNo === window.currentUserNo;
            appendMessageToUI(msg, isMine);
        });

        sendReadReceipt();
    }
}

/* 웹소켓 연결 오류를 콘솔에 기록한다. */
stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
}

/* STOMP 브로커 오류를 콘솔에 기록한다. */
stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' +
        frame.headers['message']);
    console.error('Additional details: ' +
        frame.body);
}

/* 문서 로딩 완료 후 채팅 화면 초기화 이벤트를 등록한다. */
document.addEventListener('DOMContentLoaded', async () => {
    syncAiMentionHighlight();

    // 처음 열릴 때 창 크기를 채팅 화면 기준으로 조정한다.
    if (window.outerWidth !== 600) {
        window.resizeTo(600, 800);
    }

    // 브라우저 저장소에서 인증 토큰과 사번을 읽어온다.
    const token = localStorage.getItem("accessToken");
    const empNo = localStorage.getItem("employeeNo");

    if (!token) return;

    if (token) localStorage.setItem("accessToken", token);
    if (empNo) localStorage.setItem("employeeNo", empNo);
    console.log("인증 정보 동기화 완료");

    try {
        // 내 직원 정보를 조회해 현재 사용자 사번을 확정한다.
        const myInfoRes = await fetch('/api/management/employees/me', {
            headers: {"Authorization": `Bearer ${token}`}
        });
        const myInfo = await myInfoRes.json();
        window.currentUserNo = myInfo.employeeNo;
        window.currentUserName = myInfo.name;

        // 직원 목록을 로드한다.
        await loadUserList();

        // URL 파라미터에서 채팅방 번호와 대상 사번을 가져온다.
        const urlParams = new URLSearchParams(window.location.search);
        const paramRoomId = urlParams.get('roomId');
        const paramTargetNo = urlParams.get('targetNo');

        // URL 파라미터가 있으면 전역 채팅 상태에 반영한다.
        if (paramRoomId) window.currentRoomId = paramRoomId;
        if (paramTargetNo) window.targetNo = paramTargetNo;

        if (window.currentRoomId || window.targetNo) {
            const initRes = await fetch(`/api/chat/room/init?roomId=${window.currentRoomId || ''}&targetNo=${window.targetNo || ''}`, {
                headers: {"Authorization": "Bearer " + token}
            });

            if (initRes.ok) {
                const data = await initRes.json();

                // 서버가 확정한 방 번호와 멤버 수를 전역 상태에 반영한다.
                window.currentRoomId = data.roomId;
                window.roomMemberCount = data.roomMemberCount;

                // 채팅방 이름 표시 영역을 갱신한다.
                const opponentNameEl = document.getElementById('opponentName');
                if (opponentNameEl) opponentNameEl.innerText = data.roomName || "알 수 없는 대화";

                // 초기 채팅 내역을 화면에 렌더링한다.
                if (data.chatHistory && chatMessages) {
                    chatMessages.innerHTML = '';
                    if (data.chatHistory.length === 0) {
                        // 메시지가 없으면 빈 대화 안내 문구를 표시한다.
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

        // 방 번호와 사용자 정보가 확정된 뒤 STOMP 구독을 시작한다.
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

    // 전송 버튼 클릭 이벤트를 등록한다.
    if (sendBtn) {
        sendBtn.addEventListener('click', () => {
            sendMessage();
        });
    }

    // Enter 키로 메시지를 전송하고 Shift+Enter는 줄바꿈으로 유지한다.
    if (messageInput) {
        messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });

        // 입력값 변경 시 자동 임시저장을 예약한다.
        messageInput.addEventListener('input', () => {
            const el = messageInput;

            const value = el.value;

            // @ai / @Ai / @aI → @AI 로 통일
            const normalized = value.replace(/@ai/gi, '@AI');

            if (normalized !== value) {
                const cursor = el.selectionStart;

                el.value = normalized;

                // 커서 위치 보정 (최소 안정 버전)
                requestAnimationFrame(() => {
                    el.setSelectionRange(cursor, cursor);
                });
            }

            syncAiMentionHighlight();
            clearTimeout(draftTimer);

            // 1초 동안 추가 입력이 없으면 서버에 임시저장한다.
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

    // 상단 스크롤 도달 이벤트를 등록한다.
    if (chatMessages) {
        chatMessages.addEventListener('scroll', () => {
            // 맨 위에 도달했고 추가 조회가 가능하면 이전 메시지를 불러온다.
            if (chatMessages.scrollTop === 0 && !isLoading && !isFull) {
                loadMoreMessages();
            }
        });
    }

    // 초기 화면은 가장 최근 메시지가 보이도록 아래로 이동한다.
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
