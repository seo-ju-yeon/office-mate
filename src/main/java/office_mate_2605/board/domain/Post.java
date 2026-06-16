package office_mate_2605.board.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 게시글(Post) 엔티티 ( 작성자 : 서민성 )
 *
 * <p>공지사항·자유게시판의 게시글 데이터를 관리하는 테이블과 매핑되며,
 * 소프트 딜리트 방식으로 삭제를 처리함.</p>
 */
@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor
public class Post {
    // 게시글 PK. DB에서 자동 증가한다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게시글이 속한 게시판 ID. board 테이블의 FK이다.
    @Column(name = "board_id", nullable = false)
    private Long boardId;

    // 게시글 작성자 사번. employee 테이블의 FK이다.
    @Column(name = "author_no", nullable = false)
    private String authorNo;

    // 게시글 제목.
    @Column(nullable = false, length = 200)
    private String title;

    // 게시글 본문. 길이 제한 없는 text 타입이다.
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    // 공지 상단 고정 여부. NOTICE 게시판에서 주로 사용한다. 기본값은 false이다.
    @Column(name = "is_pinned", nullable = false)
    private boolean pinned = false;

    // 게시글 조회수. 기본값은 0이다.
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    /*
     * 게시글 상태. PUBLISHED(게시) / DELETED(삭제) 두 가지이다.
     * DB에 문자열로 저장된다. (EnumType.STRING)
     */
    @Column(nullable = false, columnDefinition = "post_status")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PostStatus status = PostStatus.PUBLISHED;

    /*
     * 삭제 처리한 직원 사번.
     * 본인이 삭제했거나 삭제되지 않은 경우 NULL이다.
     * 관리자가 삭제한 경우 해당 관리자 사번이 저장된다.
     */
    @Column(name = "deleted_by")
    private String deletedBy;

    // 게시글 최초 작성 시각.
    @Column(name = "posted_at", nullable = false)
    private OffsetDateTime postedAt = OffsetDateTime.now();

    // 게시글 마지막 수정자 사번. 수정 전이면 NULL이다.
    @Column(name = "editor_no")
    private String editorNo;

    // 게시글 마지막 수정 시각. 수정 전이면 NULL이다.
    @Column(name = "edited_at")
    private OffsetDateTime editedAt;

    // 게시글 논리 삭제 시각. 삭제 전이면 NULL이다.
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    /*
     * 게시글 등록용 생성자 - 등록 시점에 필요한 필드만 받으며,
     * viewCount·status·postedAt은 기본값으로 자동 설정됨.
     *
     * @param boardId  게시판 ID
     * @param authorNo 작성자 사번
     * @param title    제목
     * @param content  본문
     * @param pinned   공지 고정 여부
     */
    @Builder
    public Post(Long boardId, String authorNo, String title,
                String content, boolean pinned) {
        this.boardId = boardId;
        this.authorNo = authorNo;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.status = PostStatus.PUBLISHED;
        this.postedAt = OffsetDateTime.now();
    }

    /* 조회수 1 증가 */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /* 게시글 수정 - 제목·본문·고정 여부를 갱신하고 수정 시각을 기록함 */
    public void update(String title, String content, boolean pinned, String editorNo) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.editorNo = editorNo;
        this.editedAt = OffsetDateTime.now();
    }

    /* 게시글 소프트 딜리트 - status를 DELETED로 변경하고 deleted_at·deleted_by를 기록함 */
    public void delete(String deletedBy) {
        this.status = PostStatus.DELETED;
        this.deletedAt = OffsetDateTime.now();
        // 관리자가 삭제한 경우에만 deleted_by 기록 (본인 삭제 시 null)
        this.deletedBy = deletedBy;
    }
}