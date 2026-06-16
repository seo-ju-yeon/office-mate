package office_mate_2605.project.repository;

import office_mate_2605.project.domain.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 프로젝트 세부 업무 관리 Repository (작성자: 강수현)

 * <p> 프로젝트에 포함된 개별 업무(Task) 엔티티에 대한 데이터베이스 접근을 담당합니다.
 * 업무 등록, 수정, 삭제 및 상태 변경을 위한 표준 JPA 기능을 제공합니다.</p>
 */

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long> {
    // 프로젝트 업무 데이터 접근
    // JpaRepository를 상속받아 세부 업무(Task) 엔티티에 대한 기본적인 CRUD 기능 수행

    // 특정 프로젝트에 속한 모든 세부 업무(Task) 목록 조회
    // Spring Data JPA가 메서드 이름을 분석하여 자동으로 `WHERE project_id = ?` 쿼리 생성
    List<ProjectTask> findByProjectId(Long projectId);
}
