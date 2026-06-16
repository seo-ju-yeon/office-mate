package office_mate_2605.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/** 채팅 메시지 DTO (작성자: 박재경)
 * 채팅 메시지 화면 표시와 저장 요청에 필요한 값을 전달하는 DTO 클래스다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long id;
    private Long chatRoomId;
    private String senderNo;
    private String senderName;
    private String content;
    private String messageType;
    private Long parentMessageId;
    private boolean isAiCalled;
    private boolean isAiGenerated;
    private Map<String, Object> aiMetadata;
    private String aiMetadataJson;
    private String sentAt;
    private int unreadCount;
}
