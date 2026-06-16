package office_mate_2605.board.domain;

/**
 * 게시판 유형(BoardType) 열거형 ( 작성자 : 서민성 )
 *
 * <p>공지사항·자유게시판 유형을 구분하는 Enum으로,
 * Board 엔티티의 type 컬럼에 매핑됨.</p>
 */
public enum BoardType {
    NOTICE,   // 공지 게시판
    GENERAL   // 자유게시판
}
