package office_mate_2605.board.mapper;

import office_mate_2605.board.dto.CommentListDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 게시글 댓글(PostComment) MyBatis 매퍼 (작성자 : 서민성 )
 * <p>게시글 댓글 조회·삭제 쿼리를 담당함.</p>
 */
@Mapper
public interface PostCommentMapper {

    /* 게시글 삭제 시 연관 댓글 전체 삭제 */
    void deleteByPostId(Long postId);

    /* 게시글별 댓글 목록 조회 - 작성자 이름·직급은 employee 테이블 JOIN 결과 */
    List<CommentListDTO> selectCommentsByPostId(Long postId);
}