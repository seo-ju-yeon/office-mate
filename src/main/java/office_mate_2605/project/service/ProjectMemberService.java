package office_mate_2605.project.service;

import office_mate_2605.project.dto.EmployeeResponseDTO;
import office_mate_2605.project.dto.ProjectMemberResponseDTO;

import java.util.List;

/**
 * 프로젝트 참여 멤버 관리 Service (작성자: 강수현)

 * <p> 프로젝트별 구성원 추가, 삭제 및 참여 인원 조회를 위한 비즈니스 로직을 정의합니다.
 * 전사 직원 목록 조회 기능을 통해 프로젝트 멤버 구성을 지원합니다.</p>
 */

public interface ProjectMemberService {
    // 프로젝트 멤버 추가
    // 특정 프로젝트에 새로운 직원을 멤버로 등록
    void addMember(Long projectId, String employeeNo);

    // 프로젝트 멤버 삭제
    // 프로젝트에서 특정 멤버를 제외
    void removeMember(Long projectId, String employeeNo);

    // 프로젝트별 멤버 목록 조회
    // 해당 프로젝트에 참여 중인 모든 멤버의 상세 정보를 조회
    List<ProjectMemberResponseDTO> getMembersByProjectId(Long projectId);

    // 전체 직원 목록 조회
    // 부서 필터링을 통해 드롭다운 등에 표시할 직원 리스트를 가져옵니다.
    List<EmployeeResponseDTO> getAllEmployees(String department);
}
