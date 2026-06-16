package office_mate_2605.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 임시저장 DTO (작성자: 박재경)
 * 채팅 입력창에 작성 중인 메시지 내용을 전달하는 DTO 클래스다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DraftDTO {
    private String content;
}
