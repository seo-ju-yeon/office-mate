package office_mate_2605.board.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 게시글 댓글(PostComment) 엔티티 ( 작성자 : 서민성 )
 * <p>게시글에 작성된 댓글 데이터를 관리하는 테이블과 매핑되며,
 * 소프트 딜리트 방식으로 삭제를 처리함.</p>
 */
@Entity
@Table(name = "post_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostComment {

    // 댓글 PK. DB에서 자동 증가한다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글이 속한 게시글 ID. post 테이블의 FK이다.
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // 댓글 작성자 사번. employee 테이블의 FK이다.
    @Column(name = "author_no", nullable = false)
    private String authorNo;

    // 댓글 본문. 최대 200자이다.
    @Column(nullable = false, length = 200)
    private String content;

    // 댓글 최초 작성 시각.
    @Column(name = "posted_at", nullable = false)
    private OffsetDateTime postedAt = OffsetDateTime.now();

    // 댓글 논리 삭제 시각. 삭제 전이면 NULL이다.
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 댓글 등록용 생성자 - 작성에 필요한 필드만 받으며, postedAt은 기본값으로 자동 설정됨.
    @Builder
    public PostComment(Long postId, String authorNo, String content) {
        this.postId = postId;
        this.authorNo = authorNo;
        this.content = content;
    }

    // 댓글 소프트 딜리트 - deleted_at을 현재 시각으로 기록함
    public void delete() {
        this.deletedAt = OffsetDateTime.now();
    }
}