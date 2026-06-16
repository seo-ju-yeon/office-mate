package office_mate_2605.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.dashboard.mapper.DashboardMapper;
import office_mate_2605.project.domain.ProjectTask;
import office_mate_2605.project.domain.TaskStatus;
import office_mate_2605.project.dto.TaskResponseDTO;
import office_mate_2605.project.repository.ProjectTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 대시보드 데이터 관리 Service 구현체 (작성자: 강수현)

 * <p> 사원별 업무 현황 통계와 참여 중인 업무 리스트를 MyBatis Mapper를 통해 조회합니다.
 * 업무 진척률 변경 시 엔티티의 상태를 자동으로 업데이트하거나 강제 변경하는 트랜잭션 로직을 수행합니다.</p>
 */

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
    private final DashboardMapper dashboardMapper;
    private final ProjectTaskRepository projectTaskRepository;

    // 업무 통계 수치 조회
    // Mapper를 호출하여 할 일, 진행 중, 완료 등 상태별 업무 개수를 Map 형태로 반환
    @Override
    public Map<String, Long> getDashboardStats(String empNo) {
        log.info("--- DashboardServiceImpl getDashboardStats");
        return dashboardMapper.getTaskStatistics(empNo);
    }

    // 업무 진행률 업데이트 및 상태 동기화
    // 진척도를 수정하며, 필요 시 요청받은 상태값(status)으로 업무 상태 강제 변경
    @Override
    @Transactional
    public void updateTaskProgress(Long taskId, String status, int progressRate) {
        log.info("--- DashboardServiceImpl updateTaskProgress");
        log.info("--- [Service] 업무 수정 시작 | ID: {}, 상태: {}, 진척도: {}%", taskId, status, progressRate);

        // 1. 엔티티 조회
        ProjectTask task = projectTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("해당 업무를 찾을 수 없습니다. ID: " + taskId));

        // 엔티티 내부 로직(updateProgressRate) 호출로 상태 자동 업데이트
        task.updateProgressRate(progressRate);

        // 3. 화면에서 명시적으로 보낸 상태값이 엔티티의 자동 변경된 상태와 다를 경우에만 추가 변경
        if (status != null) {
            TaskStatus newStatus = TaskStatus.valueOf(status);
            if (task.getStatus() != newStatus) {
                task.changeStatus(newStatus);
            }
        }
    }

    // 사원별 할당 업무 리스트 조회
    // Join 쿼리를 사용하여 본인의 업무 정보와 프로젝트 정보를 함께 조회
    @Override
    public List<TaskResponseDTO> getMyTasks(String empNo) {
        log.info("--- DashboardServiceImpl getMyTasks");
        // Mapper를 통해 JOIN된 데이터를 가져옴
        return dashboardMapper.getMyTaskList(empNo);
    }
}
