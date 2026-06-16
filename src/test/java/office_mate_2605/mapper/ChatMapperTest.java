package office_mate_2605.mapper;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.chat.domain.ChatMessage;
import office_mate_2605.chat.domain.ChatRoom;
import office_mate_2605.chat.mapper.ChatMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class ChatMapperTest {
    @Autowired
    ChatMapper chatMapper;

    @Test
    void findAllRoomsWithDetails() {
        String employeeNo = "ADMIN001";

        List<ChatRoom> rooms = chatMapper.findAllRoomsWithDetails(employeeNo);

        log.info("rooms: {}", rooms);
    }

    @Test
    void insertMessage() {
        ChatRoom room = ChatRoom.builder()
                .id(1L)
                .build();

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .senderNo("ADMIN001")
                .content("테스트 메시지입니다.")
                .messageType("TEXT")
                .sentAt(OffsetDateTime.now())
                .isAiCalled(false)
                .isAiGenerated(false)
                .build();

        chatMapper.insertMessage(message);

        log.info("메시지: {}", message.getContent());
    }

    @Test
    void findMessagesByRoomId() {
        Long roomId = 1L;
        int limit = 30;
        int offset = 0;

        List<ChatMessage> messages = chatMapper.findMessagesByRoomId(roomId, null, limit, offset);

        assertNotNull(messages);
        log.info("조회된 메시지 개수: {}", messages.size());
        messages.forEach(msg -> log.info("메시지 내용: {}, 발신 시간: {}", msg.getContent(), msg.getSentAt()));
    }
}
