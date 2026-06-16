package office_mate_2605.project.service;

import office_mate_2605.project.dto.ProjectResponseDTO;
import office_mate_2605.project.dto.ProjectTaskRequestDTO;
import office_mate_2605.project.dto.TaskResponseDTO;

import java.util.List;

/**
 * 프로젝트 세부 업무 관리 Service (작성자: 강수현)

 * <p> 프로젝트 내 개별 태스크의 등록, 수정, 삭제 및 배정된 업무 조회를 위한 기능을 정의합니다.
 * 담당자별 업무 할당 및 캘린더용 활성 업무 필터링 로직을 포함합니다.</p>
 */

public interface ProjectTaskService {
    // 프로젝트별 업무 목록 조회
    // 특정 프로젝트에 포함된 모든 업무 리스트 조회
    List<TaskResponseDTO> getTaskList(Long projectId);

    // 새 업무 등록
    // 프로젝트 내에 세부 업무를 등록하고 담당자에게 알림 발송
    Long registerTask(ProjectTaskRequestDTO dto);

    // 업무 상세 정보 조회
    // 특정 업무의 상세 내용과 담당자 정보 조회
    TaskResponseDTO getTaskDetail(Long taskId);

    // 업무 정보 수정
    // 업무 제목, 설명, 우선순위, 마감일 등의 정보를 업데이트
    void updateTask(Long taskId, ProjectTaskRequestDTO dto);

    // 업무 삭제
    // 업무 ID를 기반으로 해당 업무 데이터 삭제
    void deleteTask(Long taskId);

    // 사원별 참여 프로젝트 목록 조회
    // 특정 사원이 멤버로 속해 있는 프로젝트 리스트 조회
    List<ProjectResponseDTO> getProjectsByEmployee(String empNo);

    // 활성 프로젝트 업무 목록 조회
    // 캘린더 표시용으로, 종료되거나 취소되지 않은 프로젝트의 업무만 필터링하여 조회
    List<TaskResponseDTO> getActiveTaskList(Long projectId);
}
