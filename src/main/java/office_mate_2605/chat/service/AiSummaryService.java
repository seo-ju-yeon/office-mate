package office_mate_2605.chat.service;

import lombok.RequiredArgsConstructor;
import office_mate_2605.chat.dto.ChatMessageDTO;
import office_mate_2605.chat.mapper.ChatMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 요약 서비스 (작성자: 박재경)
 * 사용자가 선택한 채팅 메시지를 AI로 요약하고 요약 메시지를 저장하는 서비스 클래스다.
 */
@Service
@RequiredArgsConstructor
public class AiSummaryService {
    private static final Logger log = LogManager.getLogger(AiSummaryService.class);
    private final ChatClient chatClient;
    private final ChatService chatService;
    private final ChatMapper chatMapper;

    /* 선택된 메시지를 업무 중심으로 요약하고 요약 메시지를 저장한다. */
    @Transactional
    public ChatMessageDTO summarize(Long roomId, String employeeNo, List<Long> messageIds) {
        // 현재 사용자가 조회할 수 있는 선택 메시지 목록을 가져온다.
        List<ChatMessageDTO> messages = chatService.findMessagesByIds(roomId, employeeNo, messageIds);

        // 요약 대상이 없으면 사용자에게 선택을 요청한다.
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("요약할 메시지를 선택해 주세요.");
        }

        // 발신자와 본문을 합쳐 AI 프롬프트에 넣을 대화 원문을 만든다.
        String content = messages.stream()
                .map(message -> "%s: %s".formatted(message.getSenderName(), message.getContent()))
                .collect(Collectors.joining("\n"));

        // 선택된 메시지 원문만 근거로 요약을 생성한다.
        String summary = chatClient.prompt()
                .user("""
                        다음 대화를 핵심 내용 위주로 요약해라.
                        
                        업무 관련 대화라면:
                        - 중요한 일정
                        - 요청사항
                        - 결정사항
                        - 담당자
                        - 후속 작업
        
                        위주로 정리해라.
        
                        일반 대화라면:
                        - 주요 화제
                        - 대화 흐름
                        - 핵심 감정이나 분위기
                        - 중요한 언급 사항
        
                        을 자연스럽게 정리해라.
        
                        불확실한 내용은 추측하지 말고 대화에 나온 내용만 사용해라.
                        답변은 한국어로 간결하고 읽기 쉽게 작성해라.
                        !!! 중요 지침: 답변 시 강조를 위한 '**' 기호를 절대 사용하지 마라. !!!
                        강조가 필요하면 기호 없이 텍스트만 작성하거나 숫자를 사용하여 나열해라.
        
                        필요하면 아래 형식을 참고해 정리해라.
        
                        1. 대화 요약
                        2. 주요 내용
                        3. 후속 사항
        
                        [선택된 메시지]
                        %s
                        """.formatted(content))
                .call()
                .content();
        log.info("--- summary. {} ---", summary);

        // 마지막으로 요약된 메시지 번호를 계산해 방 요약 상태에 반영한다.
        Long lastSummarizedId = messages.stream()
                .map(ChatMessageDTO::getId)
                .max(Long::compareTo)
                .orElse(null);

        chatMapper.upsertChatRoomSummary(roomId, summary, lastSummarizedId);

        // 요약 결과를 AI 생성 메시지로 저장해 채팅창에 표시한다.
        return chatService.saveMessage(ChatMessageDTO.builder()
                .chatRoomId(roomId)
                .senderNo(employeeNo)
                .senderName("AI 챗봇")
                .content(summary)
                .messageType("SUMMARY")
                .isAiGenerated(true)
                .aiMetadata(Map.of(
                        "type", "CHAT_SUMMARY",
                        "model", "spring-ai-openai",
                        "sourceMessageIds", messageIds
                ))
                .build());
    }
}
