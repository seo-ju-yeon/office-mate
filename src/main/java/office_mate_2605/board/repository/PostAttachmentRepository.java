package office_mate_2605.board.repository;

import office_mate_2605.board.domain.PostAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 게시글 첨부파일(PostAttachment) JPA 레포지토리 ( 작성자 : 서민성 )
 * <p>첨부파일 엔티티의 기본 CRUD 기능을 제공함.</p>
 */
@Repository
public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {
}