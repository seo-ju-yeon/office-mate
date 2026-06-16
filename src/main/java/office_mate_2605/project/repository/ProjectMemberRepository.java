package office_mate_2605.project.repository;

import office_mate_2605.project.domain.ProjectMember;
import office_mate_2605.project.domain.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 프로젝트 참여 멤버 관리 Repository (작성자: 강수현)

 * <p> 프로젝트와 사원 간의 다대다 매핑 엔티티인 ProjectMember에 대한 데이터 접근을 담당합니다.
 * 특정 프로젝트의 참여 인원수 집계 및 멤버 중복 등록 여부 확인 등 구성원 검증 기능을 수행합니다.</p>
 */

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    // 프로젝트별 참여 멤버 수 조회
    // 특정 프로젝트 ID에 속한 전체 멤버의 인원 수 반환
    long countByProjectId(Long projectId);

    // 멤버 중복 참여 여부 확인
    // 프로젝트 ID와 사원 번호를 통해 이미 해당 프로젝트에 등록된 멤버인지 확인
    boolean existsByProjectIdAndEmployeeNo(Long projectId, String employeeNo);
}
