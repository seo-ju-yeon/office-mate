package office_mate_2605.board.dto;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 게시글 목록 조회용 DTO ( 작성자 : 서민성 )
 * <p>게시판 목록 페이지에 필요한 게시글 요약 정보를 담아 뷰에 전달함.</p>
 */
@Data
public class PostListItemDTO {

    private Long id;                   // 게시글 번호
    private Long boardId;              // 게시판 ID. 전체 목록에서 상세 링크 분기용
    private String boardName;          // 게시판 이름
    private String title;              // 게시글 제목
    private String authorName;         // 작성자 이름
    private int viewCount;             // 조회수
    private OffsetDateTime postedAt;   // 작성 시각
    private boolean pinned;            // 공지 상단 고정 여부
    private Long commentCount;         // 댓글 수
    private Long attachmentCount;      // 첨부파일 수. 1 이상이면 아이콘 표시
    private String projectName;        // 프로젝트명. 프로젝트 공지 목록에서만 채워지며 전사 게시판에서는 null이다.
}