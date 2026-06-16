package office_mate_2605.chat.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/** 채팅방 AI 설정 엔티티 (작성자: 박재경)
 * 채팅방별 AI 활성화 여부와 요약 설정을 저장하는 엔티티 클래스다.
 */
@Entity
@Table(name = "chat_room_ai_config")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatRoomAiConfig {
    @Id
    private Long roomId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "room_id")
    private ChatRoom chatRoom;

    private boolean isAiActive;
    private String systemPrompt;
    private boolean summaryEnabled;
    private String lastSummary;
    private Long lastSummarizedId;

    @Column(updatable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
