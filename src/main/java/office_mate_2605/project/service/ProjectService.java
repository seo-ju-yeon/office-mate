package office_mate_2605.project.service;

import office_mate_2605.project.dto.ProjectDetailResponseDTO;
import office_mate_2605.project.dto.ProjectRequestDTO;

import java.util.List;

/**
 * 프로젝트 기본 정보 및 생명주기 관리 Service (작성자: 강수현)

 * <p> 프로젝트의 생성, 조회, 취소 및 상태 변경과 관련된 핵심 비즈니스 명세를 정의합니다.
 * 대시보드 구성을 위한 상세 데이터 추출 기능을 포함합니다.</p>
 */

public interface ProjectService {
    // 상세 정보가 포함된 전체 프로젝트 목록 조회
    // 프로젝트 정보와 매니저명, 부서명 등 조인된 상세 데이터를 함께 조회
    List<ProjectDetailResponseDTO> getAllProjectsWithDetails();

    // 새 프로젝트 생성
    // 프로젝트 기본 정보를 저장하고 생성한 관리자를 멤버로 자동 등록
    Long createProject(ProjectRequestDTO dto);

    // 프로젝트 삭제 (논리 삭제)
    // 프로젝트를 실제로 삭제하지 않고 상태를 'CANCELED'로 변경
    void deleteProject(Long id);

    // 프로젝트 상태 변경
    // 특정 프로젝트의 진행 상태(준비, 진행, 완료 등)를 업데이트
    void updateProjectStatus(Long id, String status);
}
