package office_mate_2605.board.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.board.domain.*;
import office_mate_2605.board.dto.*;
import office_mate_2605.board.mapper.PostCommentMapper;
import office_mate_2605.board.mapper.PostMapper;
import office_mate_2605.board.repository.BoardRepository;
import office_mate_2605.board.repository.PostAttachmentRepository;
import office_mate_2605.board.repository.PostCommentRepository;
import office_mate_2605.board.repository.PostRepository;
import office_mate_2605.util.FileUploadUtil;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 게시판(Board) 서비스 ( 작성자 : 서민성 )
 * <p>게시글·댓글·첨부파일의 등록·조회·수정·삭제(CRUD) 비즈니스 로직을 담당하며,
 * 단순 CRUD는 JPA, JOIN·집계 쿼리는 MyBatis를 사용함.</p>
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class BoardService {

    private final PostCommentMapper postCommentMapper;
    private final PostMapper postMapper;
    private final PostRepository postRepository;
    private final PostAttachmentRepository postAttachmentRepository;
    private final PostCommentRepository postCommentRepository;
    private final FileUploadUtil fileUploadUtil;
    private final BoardRepository boardRepository;

    /* 전체 게시판 게시글 목록 조회 */
    public PageResponseDTO<PostListItemDTO> getAllPostList(PageRequestDTO request) {
        log.info("=== 전체 게시글 목록 조회 - page: {}, size: {} ===",
                request.getPage(), request.getSize());

        List<PostListItemDTO> list = postMapper.selectAllPostList(request);
        int total = postMapper.countAllPostList(request);

        log.info("전체 조회 결과 건수: {}, 전체 수: {}", list.size(), total);

        return PageResponseDTO.<PostListItemDTO>withAll()
                .pageRequestDTO(request)
                .dtoList(list)
                .total(total)
                .build();
    }

    /* 특정 게시판 게시글 목록 조회 */
    public PageResponseDTO<PostListItemDTO> getPostList(Long boardId, PageRequestDTO requestDTO) {
        log.info("=== 게시글 목록 조회 - boardId: {}, page: {}, size: {} ===",
                boardId, requestDTO.getPage(), requestDTO.getSize());

        List<PostListItemDTO> list = postMapper.selectPostList(boardId, requestDTO);
        int total = postMapper.countPostList(boardId, requestDTO);

        log.info("조회 결과 건수: {}, 전체 수: {}", list.size(), total);

        return PageResponseDTO.<PostListItemDTO>withAll()
                .pageRequestDTO(requestDTO)
                .dtoList(list)
                .total(total)
                .build();
    }

    /* 대시보드용 공지사항 최신 N개 조회 */
    public List<PostListItemDTO> getRecentNotices(int size) {
        return postMapper.selectRecentNotices(size);
    }

    /*
     * 프로젝트 공지 목록 조회
     * - 로그인 직원이 project_member로 속한 프로젝트의 공지글만 반환
     * - 맡은 프로젝트가 없으면 빈 리스트 반환 → 뷰에서 "맡은 프로젝트가 없습니다." 처리
     */
    public PageResponseDTO<PostListItemDTO> getProjectNoticeList(String loginEmployeeNo, PageRequestDTO request) {
        log.info("=== 프로젝트 공지 목록 조회 - employeeNo: {}, page: {}, size: {} ===",
                loginEmployeeNo, request.getPage(), request.getSize());

        List<PostListItemDTO> list = postMapper.selectProjectNoticeList(loginEmployeeNo, request);
        int total = postMapper.countProjectNoticeList(loginEmployeeNo, request);

        log.info("프로젝트 공지 조회 결과 건수: {}, 전체 수: {}", list.size(), total);

        return PageResponseDTO.<PostListItemDTO>withAll()
                .pageRequestDTO(request)
                .dtoList(list)
                .total(total)
                .build();
    }

    /*
     * 프로젝트 마감 시 해당 게시판 논리 삭제
     * - 프로젝트 status가 DONE 또는 CANCELED로 바뀔 때 호출
     * - board.deleted_at 을 현재 시각으로 설정 (실제 DELETE 없음)
     */
    @Transactional
    public void deleteProjectBoard(Long projectId) {
        boardRepository.findByProjectId(projectId)
                .ifPresent(board -> {
                    board.markDeleted();
                    log.info("=== 프로젝트 게시판 논리 삭제 완료 - projectId: {} ===", projectId);
                });
    }

    /*
     * 작성 폼 드롭다운용 — 로그인 직원이 속한 프로젝트 게시판 목록 반환
     * - board.id(boardId)와 project.name을 Map으로 반환
     * - 논리 삭제된 게시판(deleted_at IS NOT NULL) 제외
     */
    public List<Map<String, Object>> getProjectBoardsForEmployee(String loginEmployeeNo) {
        return postMapper.selectProjectBoards(loginEmployeeNo);
    }

    /*
     * 글쓰기 폼 드롭다운용 — 진행 중인 프로젝트 게시판 목록 반환
     * - DONE, CANCELED 상태 프로젝트 제외 (완료/취소된 프로젝트에는 글쓰기 불가)
     */
    public List<Map<String, Object>> getWritableProjectBoardsForEmployee(String loginEmployeeNo) {
        return postMapper.selectWritableProjectBoards(loginEmployeeNo);
    }

    /*
     * 프로젝트 생성 시 전용 공지 게시판 자동 생성
     * - ProjectServiceImpl.createProject() 에서 프로젝트 저장 직후 호출
     */
    @Transactional
    public void createProjectBoard(Long projectId, String projectName) {
        Board board = new Board(projectId, projectName + " 공지");
        boardRepository.save(board);
        log.info("=== 프로젝트 게시판 자동 생성 완료 - projectId: {} ===", projectId);
    }

    /* 게시글 등록 - 게시글 저장 후 첨부파일이 있으면 파일 저장 및 메타데이터를 함께 저장함 */
    @Transactional
    public void createPost(PostCreateDTO postCreateDTO, String authorNo) throws IOException {

        // 1. 게시글 저장
        Post post = Post.builder()
                .boardId(postCreateDTO.getBoardId())
                .authorNo(authorNo)
                .title(postCreateDTO.getTitle())
                .content(postCreateDTO.getContent())
                .pinned(postCreateDTO.isPinned())
                .build();

        postRepository.save(post);
        log.info("=== 게시글 저장 완료 - postId: {} ===", post.getId());

        // 2. 첨부파일 저장 (파일이 없거나 빈 파일이면 건너뜀)
        if (postCreateDTO.getAttachments() != null) {
            for (MultipartFile file : postCreateDTO.getAttachments()) {
                if (file.isEmpty()) continue;

                // FileUploadUtil이 저장 + URL 경로 반환 (/upload/uuid_원본파일명.ext)
                String storedPath = fileUploadUtil.save(file);
                log.info("=== 파일 저장 완료 - storedPath: {} ===", storedPath);

                // 3. 첨부파일 메타데이터 저장
                PostAttachment attachment = PostAttachment.builder()
                        .postId(post.getId())
                        .originalName(file.getOriginalFilename())
                        .storedPath(storedPath)
                        .contentType(file.getContentType())
                        .fileSize(file.getSize())
                        .build();

                postAttachmentRepository.save(attachment);
                log.info("=== 첨부파일 메타데이터 저장 완료 - originalName: {} ===", file.getOriginalFilename());
            }
        }
    }

    /* 게시글 조회수 1 증가 */
    @Transactional
    public void incrementViewCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        post.incrementViewCount();
    }

    /* 게시글 상세 조회 */
    public PostDetailDTO getPostDetail(Long postId) {
        PostDetailDTO detailDTO = postMapper.selectPostDetail(postId);
        if (detailDTO == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        return detailDTO;
    }

    /* 게시글 수정 - 제목·본문·고정 여부 업데이트 후 삭제할 첨부파일 제거 및 새 첨부파일을 저장함 */
    @Transactional
    public void updatePost(PostUpdateDTO postUpdateDTO) throws IOException {

        Post post = postRepository.findById(postUpdateDTO.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        // 1. 제목·내용·상단 고정 여부 업데이트
        String editorNo = postUpdateDTO.getEditorNo(); // PostUpdateDTO에서 꺼냄
        post.update(postUpdateDTO.getTitle(), postUpdateDTO.getContent(), postUpdateDTO.isPinned(), editorNo);
        log.info("=== 게시글 수정 완료 - PostId: {} ===", postUpdateDTO.getPostId());

        // 2. 기존 첨부파일 삭제 (× 버튼으로 선택한 것들)
        if (postUpdateDTO.getDeleteAttachmentIds() != null) {
            for (Long attachId : postUpdateDTO.getDeleteAttachmentIds()) {
                PostAttachment attachment = postAttachmentRepository.findById(attachId)
                        .orElse(null);
                if (attachment == null) continue;

                // 실제 파일 삭제 후 DB 레코드 삭제
                fileUploadUtil.delete(attachment.getStoredPath());
                postAttachmentRepository.deleteById(attachId);
                log.info("=== 첨부파일 삭제 완료 - attachId: {} ===", attachId);
            }
        }

        // 3. 새 첨부파일 저장 (파일이 없거나 빈 파일이면 건너뜀)
        if (postUpdateDTO.getAttachments() != null) {
            for (MultipartFile file : postUpdateDTO.getAttachments()) {
                if (file.isEmpty()) continue;

                String storedPath = fileUploadUtil.save(file);
                log.info("=== 수정 중 파일 저장 완료 - storedPath: {} ===", storedPath);

                PostAttachment attachment = PostAttachment.builder()
                        .postId(postUpdateDTO.getPostId())
                        .originalName(file.getOriginalFilename())
                        .storedPath(storedPath)
                        .contentType(file.getContentType())
                        .fileSize(file.getSize())
                        .build();

                postAttachmentRepository.save(attachment);
                log.info("=== 첨부파일 메타데이터 저장 완료 - originalName: {} ===", file.getOriginalFilename());
            }
        }
    }

    /*
     * 게시글 소프트 딜리트.
     * - 본인 삭제 시 deletedBy = null
     * - 관리자 삭제 시 deletedBy = 관리자 사번
     * - 연관 댓글은 MyBatis로 일괄 소프트 딜리트 처리
     * - 첨부파일은 별도 처리 없음 (post가 DELETED면 조회 시 자동 미노출)
     */
    @Transactional
    public void deletePost(Long postId, String requesterNo, String requesterRole) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        // 이미 삭제된 게시글 체크
        if (post.getStatus() == PostStatus.DELETED) {
            throw new IllegalStateException("이미 삭제된 게시글입니다.");
        }

        // 권한 체크: 본인 또는 ADMIN·SUPER만 삭제 가능
        boolean isOwner = post.getAuthorNo().equals(requesterNo);
        boolean isAdmin = requesterRole.equals("ADMIN") || requesterRole.equals("SUPER");

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("게시글 삭제 권한이 없습니다.");
        }

        // 관리자가 타인 게시글 삭제 시 deleted_by에 관리자 사번 기록 (본인 삭제 시 null)
        String deletedBy = isOwner ? null : requesterNo;
        post.delete(deletedBy);

        // 연관 댓글 일괄 소프트 딜리트 (MyBatis)
        postCommentMapper.deleteByPostId(postId);
    }

    /* 댓글 작성 */
    @Transactional
    public void saveComment(Long postId, CommentRequestDTO dto, String authorNo) {
        PostComment comment = PostComment.builder()
                .postId(postId)
                .authorNo(authorNo)
                .content(dto.getContent())
                .build();

        postCommentRepository.save(comment);
    }

    /* 게시글별 댓글 목록 조회 */
    public List<CommentListDTO> getComments(Long postId) {
        return postCommentMapper.selectCommentsByPostId(postId);
    }

    /* 댓글 소프트 딜리트 - 본인 또는 ADMIN·SUPER 권한 보유 시 삭제 가능 */
    @Transactional
    public void deleteComment(Long commentId, String requesterNo, String requesterRole) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // 권한 체크: 본인 또는 ADMIN·SUPER만 삭제 가능
        boolean isOwner = comment.getAuthorNo().equals(requesterNo);
        boolean isAdmin = requesterRole.equals("ADMIN") || requesterRole.equals("SUPER");

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("댓글 삭제 권한이 없습니다.");
        }

        comment.delete(); // 논리 삭제 (deletedAt 필드 업데이트)
    }
}