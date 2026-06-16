package office_mate_2605.chat.mapper;

import office_mate_2605.chat.domain.ChatMessage;
import office_mate_2605.chat.domain.ChatRoom;
import office_mate_2605.chat.domain.ChatRoomMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 채팅 매퍼 (작성자: 박재경)
 * MyBatis로 채팅방, 메시지, 읽음 상태 데이터를 조회하고 저장하는 매퍼 인터페이스다.
 */
@Mapper
public interface ChatMapper {
    /* 사용자가 속한 채팅방 목록과 최신 메시지 정보를 함께 조회한다. */
    List<ChatRoom> findAllRoomsWithDetails(String employeeNo);

    /* 채팅 메시지를 저장한다. */
    void insertMessage(ChatMessage chatMessage);

    /* 채팅방 메시지를 페이징 조회한다. */
    List<ChatMessage> findMessagesByRoomId(
            @Param("roomId") Long roomId,
            @Param("userNo") String userNo,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    /* 선택한 메시지 번호 목록에 해당하는 메시지를 조회한다. */
    List<ChatMessage> findMessagesByIds(
            @Param("roomId") Long roomId,
            @Param("userNo") String userNo,
            @Param("messageIds") List<Long> messageIds
    );

    /* 채팅방 AI 요약 상태를 저장하거나 갱신한다. */
    void upsertChatRoomSummary(
            @Param("roomId") Long roomId,
            @Param("summary") String summary,
            @Param("lastSummarizedId") Long lastSummarizedId
    );

    /* 채팅방을 생성한다. */
    int insertChatRoom(ChatRoom chatRoom);

    /* 채팅방 멤버를 추가한다. */
    int insertChatRoomMember(ChatRoomMember chatRoomMember);

    /* 두 사용자 사이의 1:1 채팅방 번호를 조회한다. */
    Long findPrivateRoomByMembers(@Param("currentUserNo") String currentUserNo, @Param("targetNo") String targetNo);

    /* 멤버의 마지막 읽음 메시지 번호를 갱신한다. */
    int updateMemberLastRead(@Param("roomId") Long roomId, @Param("employeeNo") String employeeNo, @Param("lastMessageId") Long lastMessageId);

    /* 멤버의 마지막 읽음 메시지 번호를 조회한다. */
    Long findOpponentLastReadId(@Param("roomId") Long roomId, @Param("opponentNo") String opponentNo);

    /* 채팅방의 가장 최신 메시지 번호를 조회한다. */
    Long findLatestMessageIdByRoomId(@Param("roomId") Long roomId);

    /* 방 번호로 채팅방 정보를 조회한다. */
    ChatRoom findById(@Param("id") Long id);

    /* 메시지를 읽지 않은 멤버 수를 조회한다. */
    int countUnreadMembers(@Param("roomId") Long roomId, @Param("senderNo") String senderNo, @Param("messageId") Long messageId);

    /* 채팅방 참여자 수를 조회한다. */
    int getRoomMemberCount(Long roomId);

    /* 채팅방 참여자의 사번 목록을 조회한다. */
    List<String> findRoomMemberNos(Long roomId);
}
