package office_mate_2605.board.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 게시판(Board) 엔티티 ( 작성자 : 서민성 )
 * <p>공지사항·자유게시판 유형 및 활성화 여부를 관리하는 테이블과 매핑됨.</p>
 */
@Entity
@Getter
@Table(name = "board")
@NoArgsConstructor
public class Board {

    // 게시판 고유 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 게시판 유형 (NOTICE: 공지사항 / GENERAL: 자유게시판)
     * - DB에 문자열(String)로 저장
     * - PostgreSQL의 커스텀 타입 board_type 컬럼에 매핑
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "board_type")
    private BoardType type; // NOTICE / GENERAL

    // 게시판 표시 이름 (예: "공지사항", "자유게시판")
    private String name;

    // 게시판 활성화 여부 - true: 사용 중 / false: 비활성(숨김 처리)
    @Column(name = "is_active")
    private boolean isActive;

    /*
     * 연결된 프로젝트 ID
     * - NULL: 전사 게시판 (공지사항, 자유게시판)
     * - NOT NULL: 프로젝트 전용 게시판
     * - Project 엔티티가 별도 팀원 패키지에 있으므로 @ManyToOne 대신 ID만 보관
     */
    @Column(name = "project_id")
    private Long projectId;

    /*
     * 논리 삭제 시각
     * - NULL: 정상 상태
     * - NOT NULL: 논리 삭제됨 (프로젝트가 DONE/CANCELED 상태로 마감될 때 채워짐)
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    // 프로젝트 마감 시 논리 삭제 처리
    public void markDeleted() {
        this.deletedAt = OffsetDateTime.now();
    }

    /* 프로젝트 전용 공지 게시판 생성용 생성자 */
    public Board(Long projectId, String name) {
        this.type = BoardType.NOTICE;
        this.name = name;
        this.projectId = projectId;
        this.isActive = true;
    }
}