package office_mate_2605.chat.service;

import office_mate_2605.chat.dto.ChatMessageDTO;
import office_mate_2605.chat.dto.ChatRoomDetailDTO;

import java.util.List;

/** 채팅 서비스 인터페이스 (작성자: 박재경)
 * 채팅방과 메시지, 읽음 상태 처리를 위한 서비스 계약을 정의하는 인터페이스다.
 */
public interface ChatService {
    /* 메시지를 저장한다. */
    ChatMessageDTO saveMessage(ChatMessageDTO chatMessageDTO);

    /* 새로운 1:1 채팅방을 생성한다. */
    Long createPersonalChatRoom(String currentUserNo, String targetNo);

    /* 기존 1:1 채팅방을 찾고 없으면 새로 생성한다. */
    Long getOrCreateRoom(String currentUserNo, String targetNo);

    /* 그룹 채팅방을 생성하고 방 번호를 반환한다. */
    Long createGroupChatRoom(String roomName, List<String> employeeNos);

    /* 기존 메시지 내역을 조회한다. */
    List<ChatMessageDTO> getChatHistory(Long roomId, String userNo);

    /* 스크롤 위치에 맞춰 메시지를 페이징 조회한다. */
    List<ChatMessageDTO> getChatHistoryWithOffset(Long roomId, String userNo, int limit, int offset);

    /* 선택된 메시지 번호 목록으로 메시지를 조회한다. */
    List<ChatMessageDTO> findMessagesByIds(Long roomId, String userNo, List<Long> messageIds);

    /* 사용자의 읽음 상태를 갱신한다. */
    boolean updateLastRead(Long roomId, String employeeNo);

    /* 사용자가 참여 중인 모든 채팅방을 조회한다. */
    List<ChatRoomDetailDTO> findAllRoomsByEmployeeNo(String employeeNo);

    /* 방 번호로 방 정보를 조회한다. */
    ChatRoomDetailDTO findRoomDetailById(Long id);

    /* 안 읽은 메시지 수를 조회한다. */
    int getUnreadCount(Long roomId, Long messageId, String senderNo);

    /* 직원별 마지막 읽음 메시지 번호를 조회한다. */
    Long getOpponentLastReadId(Long roomId, String employeeNo);

    /* 채팅방 참여자의 사번 목록을 조회한다. */
    List<String> findRoomMemberNos(Long roomId);

    /* 직원 사번으로 이름을 조회한다. */
    String findEmployeeName(String employeeNo);
}
