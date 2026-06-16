package office_mate_2605.board.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 게시글 첨부파일(PostAttachment) 엔티티 (작성자 : 서민성 )
 * <p>게시글에 첨부된 파일의 메타데이터를 관리하는 테이블과 매핑됨.</p>
 */
@Entity
@Table(name = "post_attachment")
@Getter
@NoArgsConstructor
public class PostAttachment {

    // 첨부파일 PK. DB에서 자동 증가한다.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 첨부파일이 연결된 게시글 ID. post 테이블의 FK이다.
    @Column(name = "post_id", nullable = false)
    private Long postId;

    // 사용자가 업로드한 원본 파일명. 화면에 표시할 때 사용한다.
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /*
     * 서버에 저장된 실제 파일 경로.
     * 중복 방지를 위해 UUID 등으로 변환한 이름을 사용한다.
     * 예: /uploads/board/2026/05/uuid_filename.pdf
     */
    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    // 파일 MIME 타입. 예: image/png, application/pdf
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    // 파일 크기. byte 단위이다.
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // 첨부파일 업로드 시각.
    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt = OffsetDateTime.now();

    /*
     * 첨부파일 저장용 생성자 - 파일 저장 후 반환된 경로와 메타데이터를 받아 엔티티를 생성함.
     *
     * @param postId       연결된 게시글 ID
     * @param originalName 원본 파일명
     * @param storedPath   서버 저장 경로
     * @param contentType  MIME 타입
     * @param fileSize     파일 크기 (byte)
     */
    @Builder
    public PostAttachment(Long postId, String originalName,
                          String storedPath, String contentType, Long fileSize) {
        this.postId = postId;
        this.originalName = originalName;
        this.storedPath = storedPath;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedAt = OffsetDateTime.now();
    }
}