package office_mate_2605.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
/**
 * 댓글 목록 조회용 DTO ( 작성자 : 서민성 )
 * <p>게시글 상세 페이지에서 댓글 목록을 반환할 때 사용하며,
 * 작성자 이름·직급은 employee 테이블 JOIN 결과를 담음.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentListDTO {
    private Long id;            // 댓글 PK
    private String content;      // 댓글 내용
    private String authorNo;     // 작성자 사번
    private String authorName;   // 작성자 이름 (JOIN 결과)
    private String position;     // 작성자 직급 (JOIN 결과)
    private OffsetDateTime postedAt; // 작성 시각
}