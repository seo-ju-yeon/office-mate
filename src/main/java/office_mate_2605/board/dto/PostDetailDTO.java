package office_mate_2605.board.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 게시글 상세 조회용 DTO ( 작성자 : 서민성 )
 * <p>게시글 상세 페이지에 필요한 게시글 정보·작성자 정보·첨부파일 목록을 담아 뷰에 전달함.</p>
 */
@Data
public class PostDetailDTO {

    private Long id;          // 게시글 번호
    private Long boardId;     // 게시판 ID (notice=1, general=2). 수정·삭제 후 리다이렉트 분기에 사용
    private String boardName; // 게시판 이름. 예: "공지사항"
    private String authorNo;  // 작성자 사번. 로그인 사용자와 비교해 수정·삭제 버튼 노출 여부 판단
    private String authorName; // 작성자 이름
    private String editorName; // 수정자 이름
    private String title;      // 게시글 제목
    private String content;    // 게시글 본문
    private boolean pinned;    // 공지 상단 고정 여부
    private int viewCount;     // 조회수. 상세 진입 시 이미 +1 된 값
    private OffsetDateTime postedAt;  // 최초 작성 시각
    private OffsetDateTime editedAt;  // 마지막 수정 시각. null이면 수정 이력 없음
    private List<PostAttachmentDTO> attachments; // 첨부파일 목록
}