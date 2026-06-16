package office_mate_2605.board.domain;

/**
 * 게시글 상태(PostStatus) 열거형 ( 작성자 : 서민성 )
 * <p>게시글의 게시·삭제 상태를 구분하는 Enum으로,
 * Post 엔티티의 status 컬럼에 매핑됨.</p>
 */
public enum PostStatus {
    PUBLISHED,  // 정상 게시 상태
    DELETED     // 논리 삭제 상태. 목록에서 노출되지 않는다. deleted_at에 삭제 시각이 기록
}
