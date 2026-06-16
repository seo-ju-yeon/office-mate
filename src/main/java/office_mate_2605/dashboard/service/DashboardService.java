package office_mate_2605.dashboard.service;

import office_mate_2605.project.dto.TaskResponseDTO;

import java.util.List;
import java.util.Map;

/**
 * 대시보드 데이터 관리 Service (작성자: 강수현)

 * <p> 사용자의 업무 통계 데이터 산출 및 할당된 업무 목록 조회 기능을 정의합니다.
 * 업무의 진척도 변경에 따른 상태 동기화 등 대시보드 핵심 비즈니스 로직을 명시합니다.</p>
 */

public interface DashboardService {
    // 대시보드 통계 데이터 조회
    // 사원 번호를 기반으로 업무의 진행 상태별 수치 데이터 출력
    Map<String, Long> getDashboardStats(String empNo);

    // 업무 진척도 및 상태 수정
    // 특정 업무의 진척률(%)을 업데이트하고, 수치에 따라 업무 상태를 자동 변경
    void updateTaskProgress(Long taskId, String status, int progressRate);

    // 내 업무 목록 조회
    // 본인에게 할당된 업무 리스트를 상세 정보와 함께 조회
    List<TaskResponseDTO> getMyTasks(String empNo);
}
