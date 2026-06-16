package office_mate_2605.chat.dto;

import lombok.Data;

import java.util.List;

/** 채팅방 상세 DTO (작성자: 박재경)
 * 채팅방 목록과 채팅방 초기 화면에 필요한 상세 값을 전달하는 DTO 클래스다.
 */
@Data
public class ChatRoomDetailDTO {
    private Long roomId;
    private String roomName;

    private Boolean isGroup;

    private String lastSummary;

    private String lastMessage;
    private String lastMessageTime;
    private int unreadCount;
    private int memberCount;

    private List<ChatMessageDTO> recentMessages;
}
