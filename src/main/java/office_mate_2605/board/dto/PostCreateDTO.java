package office_mate_2605.board.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 게시글 등록 요청용 DTO ( 작성자 : 서민성 )
 * <p>게시글 작성 폼에서 전달받는 데이터를 담으며,
 * 첨부파일 다중 업로드를 지원함.</p>
 */
@Data
public class PostCreateDTO {

    private Long boardId;   // 게시글이 등록될 게시판 ID. 폼의 hidden 필드로 전달됨
    private String title;   // 게시글 제목
    private String content; // 게시글 본문

    /*
     * 공지 상단 고정 여부.
     * ADMIN·SUPER 권한만 true로 설정할 수 있으며,
     * 일반 사용자는 항상 false이다.
     */
    private boolean pinned = false;

    /*
     * 첨부파일 목록.
     * 파일을 선택하지 않은 경우 null 또는 빈 리스트가 됨.
     * 여러 파일 동시 업로드를 지원함.
     */
    private List<MultipartFile> attachments;
}