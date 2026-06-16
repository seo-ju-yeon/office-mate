package office_mate_2605.project.mapper;

import office_mate_2605.project.domain.ProjectJoinResult;
import office_mate_2605.project.dto.EmployeeResponseDTO;
import office_mate_2605.project.dto.ProjectMemberResponseDTO;
import office_mate_2605.project.dto.ProjectResponseDTO;
import office_mate_2605.project.dto.TaskResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 프로젝트 데이터 접근 관리 Mapper (작성자: 강수현)

 * <p> 프로젝트, 멤버, 업무 및 직원 정보를 통합적으로 조회하기 위해 복합적인 JOIN 쿼리를 수행합니다.
 * 프로젝트 대시보드 구성, 멤버 할당, 업무 리스트 필터링 등 프로젝트 모듈의 핵심 데이터 연동을 담당합니다.</p>
 */

@Mapper
public interface ProjectMapper {
    // 프로젝트 상세 목록 조회
    // 프로젝트 정보와 함께 매니저 이름, 부서명, 실시간 진행률을 JOIN하여 조회
    List<ProjectJoinResult> selectProjectsWithDetails();

    // 프로젝트 멤버 상세 조회
    // 특정 프로젝트에 참여 중인 멤버들의 정보와 사원 상세(이름, 부서 등)를 JOIN하여 조회
    List<ProjectMemberResponseDTO> selectProjectMemberDetails(Long projectId);

    // 부서별 전체 직원 목록 조회
    // 사원 정보와 부서명을 JOIN하여 조회하며, 부서 코드로 필터링 가능
    List<EmployeeResponseDTO> selectAllEmployeesWithDept(@Param("department") String department);

    // 프로젝트별 업무 목록 조회
    // 특정 프로젝트의 모든 업무를 조회하며, 담당자 이름과 부서명을 함께 조회
    List<TaskResponseDTO> selectTasksWithAssignee(@Param("projectId") Long projectId);

    // 프로젝트 멤버 수동 등록
    // 프로젝트 ID와 사원 번호를 매핑하여 참여 멤버 테이블에 직접 삽입
    void insertProjectMember(@Param("projectId") Long projectId, @Param("employeeNo") String employeeNo);

    // 사원별 참여 중인 활성 프로젝트 조회
    // 특정 사원이 참여 중이며, 완료/보류/취소되지 않은 프로젝트 목록 조회
    List<ProjectResponseDTO> selectProjectsByEmpNo(@Param("empNo") String empNo);

    // 업무 상세 정보 단건 조회
    // 특정 업무의 상세 내용과 담당자 및 부서 정보를 JOIN하여 조회
    TaskResponseDTO selectTaskByIdWithAssignee(@Param("taskId") Long taskId);

    // 캘린더용 활성 업무 목록 조회
    // 활성 상태인 프로젝트의 업무들만 필터링하여 담당자 정보와 함께 조회
    List<TaskResponseDTO> selectActiveTasksWithAssignee(Long projectId);
}
