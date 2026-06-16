package office_mate_2605.project.repository;

import office_mate_2605.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트 기본 정보 관리 Repository (작성자: 강수현)

 * <p> 프로젝트 엔티티의 영속성 관리를 담당하며 기본적인 CRUD 기능을 제공합니다.
 * 프로젝트의 생성, 조회, 수정 및 삭제와 같은 핵심 엔티티 조작을 수행하는 인터페이스입니다.</p>
 */

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // 프로젝트 기본 데이터 접근
    // JpaRepository를 상속받아 프로젝트 엔티티에 대한 기본적인 CRUD 기능 수행
}
