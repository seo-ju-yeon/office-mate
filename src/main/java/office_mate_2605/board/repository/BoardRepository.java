package office_mate_2605.board.repository;

import office_mate_2605.board.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 게시판(Board) JPA 레포지토리 ( 작성자 : 서민성 )
 * <p>게시판 엔티티의 기본 CRUD 및 프로젝트 ID 기반 조회를 제공함.</p>
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    /*
     * 프로젝트 ID로 게시판 조회
     * - 프로젝트 마감 시 논리 삭제(markDeleted) 대상 게시판을 찾을 때 사용
     */
    Optional<Board> findByProjectId(Long projectId);
}
