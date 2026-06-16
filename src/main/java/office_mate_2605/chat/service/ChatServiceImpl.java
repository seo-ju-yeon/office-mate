package office_mate_2605.chat.service;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.chat.domain.ChatMessage;
import office_mate_2605.chat.domain.ChatRoom;
import office_mate_2605.chat.domain.ChatRoomMember;
import office_mate_2605.chat.dto.ChatMessageDTO;
import office_mate_2605.chat.dto.ChatRoomDetailDTO;
import office_mate_2605.chat.mapper.ChatMapper;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.repository.EmployeeRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 서비스 구현체 (작성자: 박재경)
 * 채팅방 생성, 메시지 저장, 읽음 상태, AI 메시지 표시 정보를 처리하는 서비스 클래스다.
 */
@Log4j2
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {
    private static final Gson GSON = new Gson();

    private final ChatMapper chatMapper;
    private final ModelMapper modelMapper;
    private final EmployeeRepository employeeRepository;

    /* 메시지를 저장하고 생성된 메시지 정보를 DTO에 반영한다. */
    @Override
    public ChatMessageDTO saveMessage(ChatMessageDTO chatMessageDTO) {
        // DTO 값을 채팅 메시지 엔티티로 변환한다.
        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(ChatRoom.builder()
                        .id(chatMessageDTO.getChatRoomId())
                        .build())
                .senderNo(chatMessageDTO.getSenderNo())
                .parentMessageId(chatMessageDTO.getParentMessageId())
                .content(chatMessageDTO.getContent())
                .messageType(chatMessageDTO.getMessageType() != null ? chatMessageDTO.getMessageType() : "TEXT")
                .isAiCalled(chatMessageDTO.isAiCalled())
                .isAiGenerated(chatMessageDTO.isAiGenerated())
                .aiMetadata(chatMessageDTO.getAiMetadata())
                .aiMetadataJson(chatMessageDTO.getAiMetadata() != null ? GSON.toJson(chatMessageDTO.getAiMetadata()) : null)
                .sentAt(OffsetDateTime.now())
                .build();

        // 메시지를 DB에 저장한다.
        chatMapper.insertMessage(chatMessage);

        // 생성된 메시지 번호와 저장 값을 응답 DTO에 세팅한다.
        chatMessageDTO.setId(chatMessage.getId());
        chatMessageDTO.setMessageType(chatMessage.getMessageType());
        chatMessageDTO.setAiMetadataJson(chatMessage.getAiMetadataJson());
        chatMessageDTO.setSentAt(chatMessage.getSentAt().toString());

        return chatMessageDTO;
    }

    /* 새로운 1:1 채팅방을 생성한다. */
    @Override
    public Long createPersonalChatRoom(String currentUserNo, String targetNo) {
        // 1:1 채팅방은 별도 이름 없이 생성한다.
        ChatRoom chatRoom = ChatRoom.builder()
                .name(null)
                .isGroup(false)
                .build();

        chatMapper.insertChatRoom(chatRoom);

        // 현재 로그인한 사용자를 채팅방 멤버로 추가한다.
        ChatRoomMember me = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .employeeNo(currentUserNo)
                .build();

        chatMapper.insertChatRoomMember(me);

        // 상대 사용자를 채팅방 멤버로 추가한다.
        ChatRoomMember target = ChatRoomMember.builder()
                .chatRoom(chatRoom)
                .employeeNo(targetNo)
                .build();

        chatMapper.insertChatRoomMember(target);

        return chatRoom.getId();
    }

    /* 기존 1:1 채팅방을 조회하고 없으면 새 채팅방을 생성한다. */
    @Override
    public Long getOrCreateRoom(String currentUserNo, String targetNo) {
        // 두 사용자의 기존 1:1 채팅방이 있는지 먼저 확인한다.
        Long existingRoomId = chatMapper.findPrivateRoomByMembers(currentUserNo, targetNo);

        if (existingRoomId != null) {
            log.info("기존 채팅방 반환: " + existingRoomId);
            return existingRoomId;
        }

        // 기존 방이 없으면 새 1:1 채팅방을 만든다.
        return createPersonalChatRoom(currentUserNo, targetNo);
    }

    /* 그룹 채팅방을 생성하고 방 번호를 반환한다. */
    @Override
    public Long createGroupChatRoom(String roomName, List<String> employeeNos) {
        // 참여 인원이 2명이면 기존 1:1 채팅방을 재사용한다.
        if (employeeNos.size() == 2) {
            Long existingRoomId = chatMapper.findPrivateRoomByMembers(employeeNos.get(0), employeeNos.get(1));
            if (existingRoomId != null) {
                return existingRoomId;
            }
        }

        // 기존 방이 없을 때만 새 채팅방을 생성한다.
        ChatRoom chatRoom = ChatRoom.builder()
                .name(roomName)
                .isGroup(employeeNos.size() > 2)
                .build();

        chatMapper.insertChatRoom(chatRoom);

        // 요청된 참여자들을 채팅방 멤버로 등록한다.
        for (String empNo : employeeNos) {
            ChatRoomMember member = ChatRoomMember.builder()
                    .chatRoom(chatRoom)
                    .employeeNo(empNo)
                    .build();
            chatMapper.insertChatRoomMember(member);
        }

        return chatRoom.getId();
    }

    /* 채팅방의 최근 메시지 내역을 조회한다. */
    @Override
    public List<ChatMessageDTO> getChatHistory(Long roomId, String userNo) {
        // 조회 사용자의 읽음 위치를 최신 메시지로 갱신한다.
        updateLastRead(roomId, userNo);

        // 최근 메시지 내역을 조회한다.
        List<ChatMessage> chatMessages = chatMapper.findMessagesByRoomId(roomId, userNo, 30, 0);

        if (chatMessages == null || chatMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // 메시지별 안 읽은 사람 수를 계산해 DTO에 담는다.
        return chatMessages.stream()
                .map(message -> {
                    ChatMessageDTO dto = toDto(message);

                    // AI 질문과 AI 답변은 요청자 전용 메시지이므로 읽음 숫자를 표시하지 않는다.
                    int unreadCount = (message.isAiCalled() || message.isAiGenerated())
                            ? 0
                            : chatMapper.countUnreadMembers(
                            roomId,
                            message.getSenderNo(),
                            message.getId()
                    );

                    dto.setUnreadCount(unreadCount);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /* 스크롤 위치에 맞춰 메시지를 페이징 조회한다. */
    @Override
    public List<ChatMessageDTO> getChatHistoryWithOffset(Long roomId, String userNo, int limit, int offset) {
        // 방 번호와 페이징 기준으로 메시지 목록을 조회한다.
        List<ChatMessage> chatMessages = chatMapper.findMessagesByRoomId(roomId, userNo, limit, offset);

        if (chatMessages == null) return Collections.emptyList();

        // 페이징 결과에도 메시지별 읽지 않은 인원 수를 계산한다.
        return chatMessages.stream()
                .map(msg -> {
                    ChatMessageDTO dto = toDto(msg);

                    // AI 질문과 AI 답변은 요청자 전용 메시지이므로 읽음 숫자를 표시하지 않는다.
                    int unreadCount = (msg.isAiCalled() || msg.isAiGenerated())
                            ? 0
                            : chatMapper.countUnreadMembers(
                            roomId,
                            msg.getSenderNo(),
                            msg.getId()
                    );

                    dto.setUnreadCount(unreadCount);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /* 선택된 메시지 번호 목록으로 요약 대상 메시지를 조회한다. */
    @Override
    public List<ChatMessageDTO> findMessagesByIds(Long roomId, String userNo, List<Long> messageIds) {
        // 선택 메시지가 없으면 빈 목록을 반환한다.
        if (messageIds == null || messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 방 번호와 사용자 권한 기준으로 메시지를 조회한다.
        List<ChatMessage> messages = chatMapper.findMessagesByIds(roomId, userNo, messageIds);
        if (messages == null) {
            return Collections.emptyList();
        }

        return messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* 사용자의 마지막 읽음 위치를 최신 메시지로 갱신한다. */
    @Override
    public boolean updateLastRead(Long roomId, String employeeNo) {
        // 채팅방의 가장 최신 메시지 번호를 조회한다.
        Long latestId = chatMapper.findLatestMessageIdByRoomId(roomId);
        if (latestId == null) return false;

        // 멤버의 마지막 읽음 메시지 번호를 최신 번호로 갱신한다.
        int result = chatMapper.updateMemberLastRead(roomId, employeeNo, latestId);

        // 갱신 행 수가 있으면 읽음 위치가 변경된 것으로 판단한다.
        return result > 0;
    }

    /* 사용자가 참여 중인 채팅방 목록을 상세 정보와 함께 조회한다. */
    @Override
    public List<ChatRoomDetailDTO> findAllRoomsByEmployeeNo(String employeeNo) {
        // 해당 직원이 참여 중인 모든 채팅방을 조회한다.
        List<ChatRoom> rooms = chatMapper.findAllRoomsWithDetails(employeeNo);
        if (rooms == null) return Collections.emptyList();

        // 채팅방 엔티티를 화면 표시용 DTO로 변환한다.
        return rooms.stream()
                .map(room -> {
                    ChatRoomDetailDTO dto = modelMapper.map(room, ChatRoomDetailDTO.class);

                    dto.setMemberCount(chatMapper.getRoomMemberCount(room.getId()));

                    // 마지막 메시지가 있으면 목록 표시용 문구와 시간을 채운다.
                    if (room.getLastMessage() != null) {
                        dto.setLastMessage(room.getLastMessage().getContent());
                        dto.setLastMessageTime(String.valueOf(room.getLastMessage().getSentAt()));
                        log.info("방 번호 {}의 마지막 메시지: {}", room.getId(), dto.getLastMessage());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /* 방 번호로 채팅방 상세 정보를 조회한다. */
    @Override
    public ChatRoomDetailDTO findRoomDetailById(Long id) {
        ChatRoom room = chatMapper.findById(id);
        if (room == null) return null;

        // 채팅방 엔티티를 상세 DTO로 변환한다.
        return modelMapper.map(room, ChatRoomDetailDTO.class);
    }

    /* 메시지를 읽지 않은 멤버 수를 조회한다. */
    @Override
    public int getUnreadCount(Long roomId, Long messageId, String senderNo) {
        return chatMapper.countUnreadMembers(roomId, senderNo, messageId);
    }

    /* 특정 직원의 마지막 읽음 메시지 번호를 조회한다. */
    @Override
    public Long getOpponentLastReadId(Long roomId, String employeeNo) {
        return chatMapper.findOpponentLastReadId(roomId, employeeNo);
    }

    /* 채팅방 참여자의 사번 목록을 조회한다. */
    @Override
    public List<String> findRoomMemberNos(Long roomId) {
        return chatMapper.findRoomMemberNos(roomId);
    }

    /* 직원 사번으로 이름을 조회한다. */
    @Override
    public String findEmployeeName(String employeeNo) {
        return employeeRepository
                .findByEmployeeNoAndDeletedAtIsNull(employeeNo)
                .map(Employee::getName)
                .orElse("알 수 없음");
    }

    /* 채팅 메시지 엔티티를 화면 응답 DTO로 변환한다. */
    private ChatMessageDTO toDto(ChatMessage message) {
        // AI 메시지는 고정 발신자명을 사용하고 일반 메시지는 직원 이름을 조회한다.
        boolean aiGenerated = message.isAiGenerated();
        boolean aiCalled = message.isAiCalled();
        String senderName = aiGenerated ? "AI 챗봇" : findEmployeeName(message.getSenderNo());

        return ChatMessageDTO.builder()
                .id(message.getId())
                .chatRoomId(message.getChatRoom() != null ? message.getChatRoom().getId() : null)
                .senderNo(message.getSenderNo())
                .senderName(senderName)
                .parentMessageId(message.getParentMessageId())
                .content(message.getContent())
                .messageType(message.getMessageType())
                .isAiCalled(aiCalled)
                .isAiGenerated(aiGenerated)
                .aiMetadata(message.getAiMetadata())
                .sentAt(message.getSentAt() != null ? message.getSentAt().toString() : null)
                .build();
    }
}
