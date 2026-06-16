package office_mate_2605.board.dto;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 게시글 첨부파일 조회용 DTO ( 작성자 : 서민성 )
 * <p>게시글 상세 조회 시 첨부파일 메타데이터를 담아 뷰에 전달함.</p>
 */
@Data
public class PostAttachmentDTO {

    private Long id;                 // 첨부파일 PK
    private String originalName;     // 화면에 표시할 원본 파일명
    private String storedPath;       // 다운로드·접근 URL 경로. 예: /upload/uuid_파일명.pdf
    private String contentType;      // 파일 MIME 타입. 예: image/png, application/pdf
    private Long fileSize;           // 파일 크기. byte 단위
    private OffsetDateTime uploadedAt; // 업로드 시각
}