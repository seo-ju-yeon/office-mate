package office_mate_2605.chat.domain;

import lombok.*;

import java.io.Serializable;

/** 채팅방 멤버 복합 키 클래스 (작성자: 박재경)
 * 채팅방 번호와 직원 사번을 조합한 복합 기본 키를 나타내는 클래스다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ChatRoomMemberId implements Serializable {
    private Long chatRoom;
    private String employeeNo;
}