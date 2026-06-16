package office_mate_2605.board.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
/**
 * 댓글 작성 요청용 DTO ( 작성자 : 서민성 )
 * <p>댓글 작성 시 클라이언트로부터 전달받는 요청 데이터를 담음.</p>
 */
@Data
@NoArgsConstructor
public class CommentRequestDTO {
    private String content; // 댓글 내용
}
