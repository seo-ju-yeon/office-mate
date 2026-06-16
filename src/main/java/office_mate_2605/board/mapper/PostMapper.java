package office_mate_2605.board.mapper;

import office_mate_2605.board.dto.PageRequestDTO;
import office_mate_2605.board.dto.PostDetailDTO;
import office_mate_2605.board.dto.PostListItemDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 게시글(Post) MyBatis 매퍼 ( 작성자 : 서민성 )
 * <p>게시글 목록·상세 조회 및 페이지네이션 카운트 쿼리를 담당함.</p>
 */
@Mapper
public interface PostMapper {

    /* 전체 게시판 게시글 목록 조회 */
    List<PostListItemDTO> selectAllPostList(
            @Param("request") PageRequestDTO request
    );

    /* 전체 게시판 게시글 수 카운트 */
    int countAllPostList(
            @Param("request") PageRequestDTO request
    );

    /* 특정 게시판 게시글 목록 조회 */
    List<PostListItemDTO> selectPostList(
            @Param("boardId") Long boardId,
            @Param("request") PageRequestDTO requestDTO
    );

    /* 특정 게시판 게시글 수 카운트 */
    int countPostList(
            @Param("boardId") Long boardId,
            @Param("request") PageRequestDTO requestDTO
    );

    /* 게시글 상세 조회 */
    PostDetailDTO selectPostDetail(@Param("postId") Long postId);

    /* 대시보드용 공지사항 최신 N개 조회 */
    List<PostListItemDTO> selectRecentNotices(@Param("size") int size);

    /* 프로젝트 공지 목록 조회 — 로그인 직원이 속한 프로젝트의 공지글만 반환 */
    List<PostListItemDTO> selectProjectNoticeList(
            @Param("loginEmployeeNo") String loginEmployeeNo,
            @Param("request") PageRequestDTO request
    );

    /* 프로젝트 공지 게시글 수 카운트 — 페이징 계산용 */
    int countProjectNoticeList(
            @Param("loginEmployeeNo") String loginEmployeeNo,
            @Param("request") PageRequestDTO request
    );

    /* 작성 폼 드롭다운용 — 로그인 직원이 속한 프로젝트 게시판 목록 (boardId, projectName) */
    List<Map<String, Object>> selectProjectBoards(
            @Param("loginEmployeeNo") String loginEmployeeNo
    );

    /* 글쓰기 폼 드롭다운용 — 진행 중인 프로젝트 게시판 목록 (DONE/CANCELED 제외) */
    List<Map<String, Object>> selectWritableProjectBoards(
            @Param("loginEmployeeNo") String loginEmployeeNo
    );
}