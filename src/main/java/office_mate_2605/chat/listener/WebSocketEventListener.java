package office_mate_2605.chat.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 웹소켓 이벤트 리스너 (작성자: 박재경)
 * 채팅 웹소켓 접속, 구독, 종료 이벤트를 감지해 온라인 상태를 전송하는 클래스다.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketEventListener {
    private final SimpMessageSendingOperations messagingTemplate;

    private static final Set<String> onlineUsers = Collections.synchronizedSet(new HashSet<>());

    /* 웹소켓 연결 성공 시 온라인 사용자 목록을 갱신한다. */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        // 인증 정보에서 접속한 사용자의 사번을 가져온다.
        Principal user = headerAccessor.getUser();

        if (user != null) {
            String employeeNo = headerAccessor.getUser().getName();

            // 온라인 사용자 목록에 사번을 추가한다.
            onlineUsers.add(employeeNo);
            log.info("사원 접속: [{}] - 현재 접속자 수: {}", employeeNo, onlineUsers.size());

            // 변경된 온라인 상태를 모든 구독자에게 전송한다.
            broadcastStatus();
        } else {
            log.error("웹소켓 연결 성공했으나 유저 정보를 찾을 수 없음 (Principal is null)");
        }
    }

    /* 상태 구독이 시작되면 현재 온라인 사용자 목록을 전송한다. */
    @EventListener
    public void handleSubscriptionEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();

        // 상태 채널 구독자에게 최신 온라인 목록을 전달한다.
        if ("/sub/status".equals(destination)) {
            log.info("새로운 구독 감지 - 명단 전송");
            broadcastStatus();
        }
    }

    /* 웹소켓 연결 종료 시 온라인 사용자 목록을 갱신한다. */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        if (headerAccessor.getUser() != null) {
            String employeeNo = headerAccessor.getUser().getName();
            onlineUsers.remove(employeeNo);

            log.info("사원 퇴장: [{}] - 현재 접속자 수: {}", employeeNo, onlineUsers.size());

            // 변경된 온라인 상태를 모든 구독자에게 전송한다.
            broadcastStatus();
        }
    }

    /* 현재 온라인 사용자 사번 목록을 상태 채널로 전송한다. */
    private void broadcastStatus() {
        messagingTemplate.convertAndSend("/sub/status", onlineUsers);
    }
}
