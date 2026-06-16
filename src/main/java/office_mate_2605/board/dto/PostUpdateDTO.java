package office_mate_2605.board.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 게시글 수정 요청용 DTO ( 작성자 : 서민성 )
 * <p>게시글 수정 폼에서 전달받는 데이터를 담으며,
 * 기존 첨부파일 삭제 목록과 새 첨부파일 추가를 함께 처리함.</p>
 */
@Getter
@Setter
public class PostUpdateDTO {

    private Long postId;    // 수정할 게시글 ID
    private Long boardId;   // 게시판 ID (1: 공지, 2: 자유). 수정 후 리다이렉트 분기에 사용
    private String title;   // 수정된 제목
    private String editorNo;// 수정자 사번
    private String content; // 수정된 본문
    private boolean pinned = false; // 공지 상단 고정 여부
    private List<Long> deleteAttachmentIds; // 삭제할 기존 첨부파일 ID 목록
    private List<MultipartFile> attachments; // 새로 추가할 첨부파일 목록
}