package office_mate_2605.chat.domain;

import jakarta.persistence.*;
import lombok.*;

/** 채팅방 엔티티 (작성자: 박재경)
 * 1:1 또는 그룹 채팅방과 AI 설정 연결 정보를 나타내는 엔티티 클래스다.
 */
@Entity
@Table(name = "chat_room")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "is_group")
    private Boolean isGroup;

    @OneToOne(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    private ChatRoomAiConfig chatRoomAiConfig;

    @Transient
    private ChatMessage lastMessage;
}
