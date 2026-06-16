package office_mate_2605.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.chat.dto.ChatMessageDTO;
import office_mate_2605.chat.service.AiChatService;
import office_mate_2605.chat.service.ChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * STOMP 채팅 컨트롤러 (작성자: 박재경)
 * 웹소켓으로 들어온 채팅 메시지와 읽음 상태 및 AI 응답 전송을 처리하는 컨트롤러 클래스다.
 */
@Log4j2
@Controller
@RequiredArgsConstructor
public class StompChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final AiChatService aiChatService;

    /* 채팅방별 메시지를 저장하고 구독자에게 전송한다. */
    @MessageMapping("/chat/{roomId}")
    public void handleMessage(@DestinationVariable Long roomId, ChatMessageDTO chatMessageDTO) {
        log.info("수신된 메시지: {}", chatMessageDTO);

        // 요청 경로의 채팅방 번호와 AI 호출 여부를 메시지에 반영한다.
        chatMessageDTO.setChatRoomId(roomId);
        boolean aiQuestion = isAiQuestion(chatMessageDTO.getContent());
        chatMessageDTO.setAiCalled(aiQuestion);
        if (aiQuestion) {
            chatMessageDTO.setAiMetadata(Map.of(
                    "type", "CALENDAR_QA_REQUEST",
                    "trigger", "@AI"
            ));
        }

        try {
            // 사용자 메시지를 먼저 저장한다.
            ChatMessageDTO savedMessage = chatService.saveMessage(chatMessageDTO);

            // 발신자 이름을 조회해 화면 표시용 DTO에 채운다.
            String senderName = chatService.findEmployeeName(chatMessageDTO.getSenderNo());

            savedMessage.setSenderName(senderName);

            log.info("발신자 이름: {}", senderName);
            log.info("방번호: {}, 메시지ID: {}, 보낸사람: {}", roomId, savedMessage.getId(), chatMessageDTO.getSenderNo());

            // 본인이 보낸 메시지는 즉시 읽음 처리한다.
            chatService.updateLastRead(roomId, chatMessageDTO.getSenderNo());

            // AI 질문 메시지는 요청자에게만 보이므로 읽지 않은 수를 표시하지 않는다.
            int unreadCount = aiQuestion
                    ? 0
                    : chatService.getUnreadCount(roomId, savedMessage.getId(), chatMessageDTO.getSenderNo());
            savedMessage.setUnreadCount(unreadCount);

            log.info("계산된 unreadCount: {}", unreadCount);

            // AI 질문은 요청자 전용 채널로 보내고 일반 메시지는 방 전체에 보낸다.
            if (aiQuestion) {
                messagingTemplate.convertAndSend(
                        "/sub/chat/" + roomId + "/ai/" + chatMessageDTO.getSenderNo(),
                        savedMessage
                );
            } else {
                messagingTemplate.convertAndSend("/sub/chat/" + roomId, savedMessage);
            }

            if (aiQuestion) {
                // @AI 접두어를 제거해 실제 질문만 추출한다.
                String question = extractAiQuestion(chatMessageDTO.getContent());

                log.info("AI 질문 감지: {}", question);

                // AI 응답 생성 실패 시 사용자에게 안내할 기본 문구를 준비한다.
                String aiAnswer;
                try {
                    aiAnswer = aiChatService.generateAiResponse(chatMessageDTO.getSenderNo(), question);
                } catch (Exception aiException) {
                    log.error("AI 응답 생성 중 오류 발생: ", aiException);
                    aiAnswer = "AI 답변을 생성하지 못했습니다. 잠시 후 다시 질문해 주세요.";
                }

                log.info("AI 응답: {}", aiAnswer);

                // AI 답변 메시지를 원본 질문에 연결해 저장할 DTO로 만든다.
                ChatMessageDTO aiMessage =
                        ChatMessageDTO.builder()
                                .chatRoomId(roomId)
                                .senderNo(chatMessageDTO.getSenderNo())
                                .senderName("AI 챗봇")
                                .parentMessageId(savedMessage.getId())
                                .content(aiAnswer)
                                .messageType("TEXT")
                                .isAiGenerated(true)
                                .aiMetadata(Map.of(
                                        "type", "CALENDAR_QA_RESPONSE",
                                        "model", "spring-ai-openai",
                                        "sourceMessageId", savedMessage.getId()
                                ))
                                .build();

                // AI 메시지를 저장하고 요청자 전용 채널로 전송한다.
                ChatMessageDTO savedAiMessage = chatService.saveMessage(aiMessage);
                savedAiMessage.setSenderName("AI 챗봇");
                savedAiMessage.setAiGenerated(true);

                messagingTemplate.convertAndSend(
                        "/sub/chat/" + roomId + "/ai/" + chatMessageDTO.getSenderNo(),
                        savedAiMessage
                );
            }

            // AI 질문은 개인 응답이므로 채팅방 알림 전송을 생략한다.
            if (aiQuestion) {
                return;
            }

            // 일반 메시지는 본인을 제외한 채팅방 멤버에게 알림을 전송한다.
            List<String> memberNos = chatService.findRoomMemberNos(roomId);
            for (String memberNo : memberNos) {
                if (!memberNo.equals(savedMessage.getSenderNo())) {

                    messagingTemplate.convertAndSend(
                            "/sub/chat/notification/" + memberNo, savedMessage
                    );
                }
            }
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: ", e);
        }
    }

    /* 메시지가 @AI 질문인지 확인한다. */
    private boolean isAiQuestion(String content) {
        return content != null && content.trim().startsWith("@AI");
    }

    /* @AI 접두어를 제거하고 비어 있는 질문에는 안내 문구를 반환한다. */
    private String extractAiQuestion(String content) {
        String question = content.trim().substring("@AI".length()).trim();
        if (question.isBlank()) {
            return "사용자가 일정에 대해 질문하지 않았습니다. @AI 뒤에 일정 질문을 입력하라고 안내해 주세요.";
        }

        return question;
    }

    /* 사용자의 읽음 위치를 갱신하고 같은 채팅방에 읽음 이벤트를 전송한다. */
    @MessageMapping("/chat/{roomId}/read")
    public void handleReadReceipt(@DestinationVariable Long roomId, String employeeNo) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("readerNo", employeeNo);

        // 갱신 전 읽음 위치를 저장해 화면의 읽음 숫자 감소 범위를 계산한다.
        payload.put("oldReadId", chatService.getOpponentLastReadId(roomId, employeeNo));

        // 현재 채팅방의 최신 메시지까지 읽음 처리한다.
        chatService.updateLastRead(roomId, employeeNo);

        // 갱신 후 읽음 위치를 함께 전송한다.
        payload.put("lastReadMessageId", chatService.getOpponentLastReadId(roomId, employeeNo));

        messagingTemplate.convertAndSend("/sub/chat/" + roomId + "/read", payload);
    }
}
