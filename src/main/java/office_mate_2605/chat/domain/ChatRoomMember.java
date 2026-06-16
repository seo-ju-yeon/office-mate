package office_mate_2605.chat.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/** 채팅방 멤버 엔티티 (작성자: 박재경)
 * 채팅방에 참여한 직원과 참여 시각 정보를 나타내는 엔티티 클래스다.
 */
@Entity
@Table(name = "chat_room_member")
@IdClass(ChatRoomMemberId.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatRoomMember {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom chatRoom;

    @Id
    @Column(name = "employee_no")
    private String employeeNo;

    @Column(name = "joined_at")
    @Builder.Default
    private OffsetDateTime joinedAt = OffsetDateTime.now();

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
}
