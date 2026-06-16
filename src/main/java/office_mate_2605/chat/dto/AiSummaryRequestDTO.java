package office_mate_2605.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** AI 요약 요청 DTO (작성자: 박재경)
 * AI 요약 대상 메시지 번호 목록을 전달하는 DTO 클래스다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryRequestDTO {
    private List<Long> messageIds;
}
