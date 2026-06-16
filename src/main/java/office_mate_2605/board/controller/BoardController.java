package office_mate_2605.board.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.board.dto.*;
import office_mate_2605.board.service.BoardService;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 게시판(Board) 뷰 컨트롤러 ( 작성자 : 서민성 )
 * <p>공지사항·자유게시판의 목록·상세·작성·수정 페이지 요청을 처리하며,
 * Thymeleaf 뷰 템플릿과 Model 데이터 바인딩을 담당함.</p>
 */
@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
@Log4j2
public class BoardController {

    private final BoardService boardService;

    // 게시판 화면도 레이아웃의 로그인/권한 확인 대상에 포함
    private void applyBoardPageAccess(Model model, String activePage) {
        model.addAttribute("activePage", activePage);
        model.addAttribute("pageRoles", "USER,ADMIN,SUPER");
    }

    /* 전체 게시글 목록 - GET /board/list */
    @GetMapping("/list")
    public String board(PageRequestDTO requestDTO, Model model) {
        log.info("=== 전체 게시판 목록 요청 - page: {}, size: {} ===",
                requestDTO.getPage(), requestDTO.getSize());

        PageResponseDTO<PostListItemDTO> response = boardService.getAllPostList(requestDTO);
        model.addAttribute("response", response);
        applyBoardPageAccess(model, "boardAll");

        log.info("전체 게시글 수: {}", response.getTotal());
        return "board/board_list";
    }

    /* 공지사항 목록 - GET /board/notice/list */
    @GetMapping("/notice/list")
    public String notice(PageRequestDTO requestDTO, Model model) {
        log.info("=== 공지사항 목록 요청 - page: {}, size: {} ===",
                requestDTO.getPage(), requestDTO.getSize());

        PageResponseDTO<PostListItemDTO> response = boardService.getPostList(1L, requestDTO);
        model.addAttribute("response", response);
        applyBoardPageAccess(model, "notice");

        log.info("공지사항 총 게시글 수: {}", response.getTotal());
        return "board/notice_list";
    }

    /* 자유게시판 목록 - GET /board/general/list */
    @GetMapping("/general/list")
    public String general(PageRequestDTO requestDTO, Model model) {
        log.info("=== 일반 게시판 목록 요청 - page: {}, size: {} ===",
                requestDTO.getPage(), requestDTO.getSize());

        PageResponseDTO<PostListItemDTO> response = boardService.getPostList(2L, requestDTO);
        model.addAttribute("response", response);
        applyBoardPageAccess(model, "general");

        log.info("일반 게시판 총 게시글 수: {}", response.getTotal());
        return "board/general_list";
    }

    /* 공지사항 상세 조회 - GET /board/notice/{postId} */
    @GetMapping("/notice/{postId}")
    public String noticeDetail(@PathVariable Long postId, Model model) {
        // 조회수 증가 후 상세 데이터 조회
        boardService.incrementViewCount(postId);
        PostDetailDTO postDetailDTO = boardService.getPostDetail(postId);
        model.addAttribute("post", postDetailDTO);
        applyBoardPageAccess(model, "notice");
        return "board/post_detail";
    }

    /* 자유게시판 상세 조회 - GET /board/general/{postId} */
    @GetMapping("/general/{postId}")
    public String generalDetail(@PathVariable Long postId, Model model) {
        // 조회수 증가 후 상세 데이터 조회
        boardService.incrementViewCount(postId);
        PostDetailDTO postDetailDTO = boardService.getPostDetail(postId);
        model.addAttribute("post", postDetailDTO);
        applyBoardPageAccess(model, "general");
        return "board/post_detail";
    }

    /* 공지사항 작성 폼 - GET /board/notice/write */
    @GetMapping("/notice/write")
    public String noticeWriteForm(Model model) {
        // 권한 체크는 /api/board/notice/write-check 에서 이미 완료
        model.addAttribute("boardId", 1L);
        model.addAttribute("boardType", "notice");
        applyBoardPageAccess(model, "notice");
        return "board/write";
    }

    /* 자유게시판 작성 폼 - GET /board/general/write */
    @GetMapping("/general/write")
    public String generalWriteForm(Model model) {
        // 자유게시판은 권한 체크 없이 모든 로그인 사용자 접근 가능
        model.addAttribute("boardId", 2L);
        model.addAttribute("boardType", "general");
        applyBoardPageAccess(model, "general");
        return "board/write";
    }

    /* 공지사항 수정 폼 - GET /board/notice/{postId}/edit */
    @GetMapping("/notice/{postId}/edit")
    public String noticeEditForm(@PathVariable Long postId, Model model) {
        // 수정 권한 체크는 JS에서 /api/board/notice/write-check 호출 후 진입
        PostDetailDTO post = boardService.getPostDetail(postId);
        model.addAttribute("post", post);
        model.addAttribute("boardType", "notice");
        applyBoardPageAccess(model, "notice");
        return "board/edit";
    }

    /* 자유게시판 수정 폼 - GET /board/general/{postId}/edit */
    @GetMapping("/general/{postId}/edit")
    public String generalEditForm(@PathVariable Long postId, Model model) {
        // 수정 권한 체크는 JS에서 /api/board/notice/write-check 호출 후 진입
        PostDetailDTO post = boardService.getPostDetail(postId);
        model.addAttribute("post", post);
        model.addAttribute("boardType", "general");
        applyBoardPageAccess(model, "general");
        return "board/edit";
    }

    /* 게시글 수정 저장 - POST /board/update */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String update(@ModelAttribute PostUpdateDTO postUpdateDTO,
                         RedirectAttributes redirectAttributes) throws IOException {
        boardService.updatePost(postUpdateDTO);

        redirectAttributes.addFlashAttribute("successMsg", "게시글이 수정되었습니다.");

        // boardId로 공지/자유 상세 페이지 리다이렉트 분기
        String redirectUrl = (postUpdateDTO.getBoardId() == 1L)
                ? "/board/notice/" + postUpdateDTO.getPostId()
                : "/board/general/" + postUpdateDTO.getPostId();
        return "redirect:" + redirectUrl;
    }

    /* 프로젝트 공지 목록 화면 - GET /board/project/notice/list */
    // 데이터는 JS에서 /api/board/project/notices 호출로 받아옴
    @GetMapping("/project/notice/list")
    public String projectNotice(Model model) {
        applyBoardPageAccess(model, "projectNotice");
        return "board/project_notice_list";
    }

    /* 프로젝트 공지 작성 폼 - GET /board/project/notice/write */
    @GetMapping("/project/notice/write")
    public String projectNoticeWriteForm(Model model) {
        model.addAttribute("boardType", "projectNotice");
        // boardId는 write.js에서 /api/board/project/boards 드롭다운 선택으로 결정
        applyBoardPageAccess(model, "projectNotice");
        return "board/write";
    }

    /* 프로젝트 공지 수정 폼 - GET /board/project/notice/{postId}/edit */
    @GetMapping("/project/notice/{postId}/edit")
    public String projectNoticeEditForm(@PathVariable Long postId, Model model) {
        PostDetailDTO post = boardService.getPostDetail(postId);
        model.addAttribute("post", post);
        model.addAttribute("boardType", "projectNotice");
        applyBoardPageAccess(model, "projectNotice");
        return "board/edit";
    }

    /* 프로젝트 공지 상세 조회 - GET /board/project/notice/{postId} */
    @GetMapping("/project/notice/{postId}")
    public String projectNoticeDetail(@PathVariable Long postId, Model model) {
        boardService.incrementViewCount(postId);
        PostDetailDTO postDetailDTO = boardService.getPostDetail(postId);
        model.addAttribute("post", postDetailDTO);
        applyBoardPageAccess(model, "projectNotice");
        return "board/post_detail";
    }

}