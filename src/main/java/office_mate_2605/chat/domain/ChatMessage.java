package office_mate_2605.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/** 채팅 메시지 엔티티 (작성자: 박재경)
 * 채팅방에 저장되는 사용자 메시지와 AI 메시지 정보를 나타내는 엔티티 클래스다.
 */
@Entity
@Table(name = "chat_message")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private ChatRoom chatRoom;

    private String senderNo;
    private Long parentMessageId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String messageType;
    private boolean isAiCalled;
    private boolean isAiGenerated;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> aiMetadata;

    @Transient
    private String aiMetadataJson;

    private OffsetDateTime sentAt = OffsetDateTime.now();
}
