package office_mate_2605.board.controller;

import lombok.RequiredArgsConstructor;
import office_mate_2605.board.dto.*;
import office_mate_2605.board.service.BoardService;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *게시판(Board) API 컨트롤러 (작성자 : 서민성 )
 * <p>게시글 및 댓글의 등록·조회·삭제(CRUD) REST API를 제공하며,
 * 공지사항 작성 권한 체크 및 대시보드용 최신 공지사항 조회 기능을 담당함.</p>
 */
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class BoardApiController {

    private final BoardService boardService;

    /* 대시보드용 공지사항 최신 N개 조회 - GET /api/board/notices/recent?size=5 */
    @GetMapping("/notices/recent")
    public ResponseEntity<List<PostListItemDTO>> getRecentNotices(
            @RequestParam(defaultValue = "5") int size) {
        // size 파라미터 미입력 시 기본값 5로 조회
        return ResponseEntity.ok(boardService.getRecentNotices(size));
    }

    /*
     * 프로젝트 공지 목록 조회 - GET /api/board/project/notices?page=1&size=10
     * - 로그인 직원이 속한 프로젝트의 공지글만 반환
     * - TokenCheckFilter가 /api/** 를 검사하므로 EmployeePrincipal 정상 주입됨
     */
    @GetMapping("/project/notices")
    public ResponseEntity<PageResponseDTO<PostListItemDTO>> getProjectNotices(
            PageRequestDTO requestDTO,
            @AuthenticationPrincipal EmployeePrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        PageResponseDTO<PostListItemDTO> response =
                boardService.getProjectNoticeList(principal.getEmployeeNo(), requestDTO);
        return ResponseEntity.ok(response);
    }

    /*
     * 내가 속한 프로젝트 게시판 목록 - GET /api/board/project/boards
     * - 목록 페이지 상태 탭 드롭다운에 사용 (전체 상태 포함)
     * - board.id(boardId)와 project.name, project.status를 반환
     */
    @GetMapping("/project/boards")
    public ResponseEntity<List<Map<String, Object>>> getProjectBoards(
            @AuthenticationPrincipal EmployeePrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        List<Map<String, Object>> boards =
                boardService.getProjectBoardsForEmployee(principal.getEmployeeNo());
        return ResponseEntity.ok(boards);
    }

    /*
     * 글쓰기 폼 드롭다운용 프로젝트 게시판 목록 - GET /api/board/project/boards/writable
     * - DONE, CANCELED 상태 프로젝트 제외 (완료/취소된 프로젝트에는 글쓰기 불가)
     */
    @GetMapping("/project/boards/writable")
    public ResponseEntity<List<Map<String, Object>>> getWritableProjectBoards(
            @AuthenticationPrincipal EmployeePrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        List<Map<String, Object>> boards =
                boardService.getWritableProjectBoardsForEmployee(principal.getEmployeeNo());
        return ResponseEntity.ok(boards);
    }

    /* 공지사항 작성 권한 체크 - GET /api/board/notice/write-check */
    @GetMapping("/notice/write-check")
    public ResponseEntity<Void> noticeWriteCheck(
            @AuthenticationPrincipal EmployeePrincipal principal) {

        // fetch로 Authorization 헤더를 붙여서 호출 → TokenCheckFilter가 토큰 검증 후 SecurityContext 채움
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        // ADMIN, SUPER 권한만 공지사항 작성 허용
        String role = principal.getRole();
        if (!role.equals("ADMIN") && !role.equals("SUPER")) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok().build();
    }

    /* 게시글 등록 - POST /api/board/write */
    @PostMapping("/write")
    public ResponseEntity<Void> write(
            PostCreateDTO postCreateDTO,
            @AuthenticationPrincipal EmployeePrincipal principal) throws IOException {

        // 인증되지 않은 사용자 접근 차단
        if (principal == null) return ResponseEntity.status(401).build();

        boardService.createPost(postCreateDTO, principal.getEmployeeNo());
        return ResponseEntity.ok().build();
    }

    /* 게시글 삭제 - DELETE /api/board/{postId} */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal EmployeePrincipal principal) {

        // Filter를 통과했으므로 principal이 null이 아님이 보장됨
        // 만약 비정상적인 접근으로 null이라면 안전하게 401 반환
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        boardService.deletePost(
                postId,
                principal.getEmployeeNo(),
                principal.getRole()
        );

        return ResponseEntity.noContent().build(); // 204 Success
    }

    /* 댓글 목록 조회 - GET /api/board/{postId}/comments */
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentListDTO>> getComments(@PathVariable Long postId) {
        List<CommentListDTO> list = boardService.getComments(postId);
        return ResponseEntity.ok(list);
    }

    /* 댓글 작성 - POST /api/board/{postId}/comments */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Void> createComment(
            @PathVariable Long postId,
            @RequestBody CommentRequestDTO requestDTO,
            @AuthenticationPrincipal EmployeePrincipal principal) {

        // 로그인한 사원 번호로 댓글 저장
        boardService.saveComment(postId, requestDTO, principal.getEmployeeNo());
        return ResponseEntity.ok().build();
    }

    /* 댓글 삭제 - DELETE /api/board/comments/{commentId} */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal EmployeePrincipal principal) {

        // 본인 댓글 또는 ADMIN·SUPER 권한 보유 시 삭제 가능
        boardService.deleteComment(commentId, principal.getEmployeeNo(), principal.getRole());
        return ResponseEntity.noContent().build();
    }
}