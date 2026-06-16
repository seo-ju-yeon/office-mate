package office_mate_2605.project.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.dashboard.domain.NotificationRefType;
import office_mate_2605.dashboard.service.NotificationService;
import office_mate_2605.project.domain.ProjectMember;
import office_mate_2605.project.domain.ProjectTask;
import office_mate_2605.project.domain.TaskStatus;
import office_mate_2605.project.dto.ProjectResponseDTO;
import office_mate_2605.project.dto.ProjectTaskRequestDTO;
import office_mate_2605.project.dto.TaskResponseDTO;
import office_mate_2605.project.mapper.ProjectMapper;
import office_mate_2605.project.repository.ProjectMemberRepository;
import office_mate_2605.project.repository.ProjectRepository;
import office_mate_2605.project.repository.ProjectTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 프로젝트 세부 업무 관리 Service 구현체 (작성자: 강수현)

 * <p> 프로젝트 업무 생성 시 담당자에게 알림을 전송하며, JPA의 변경 감지(Dirty Checking)를 통해 업무 정보를 수정합니다.
 * MyBatis를 활용해 복잡한 담당자 정보 조인을 처리하고, 캘린더 화면에 필요한 활성 프로젝트 업무 리스트를 제공합니다.</p>
 */

@Log4j2
@Service
@RequiredArgsConstructor
public class ProjectTaskServiceImpl implements ProjectTaskService {
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectMapper projectMapper;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    // 프로젝트 업무 목록 조회 (MyBatis)
    // Mapper를 통해 업무 정보와 담당자 상세 정보를 JOIN하여 리스트로 반환
    @Override
    public List<TaskResponseDTO> getTaskList(Long projectId) {
        log.info("--- ProjectTaskServiceImpl getTaskList: 프로젝트 ID {} ---", projectId);
        return projectMapper.selectTasksWithAssignee(projectId);
    }

    // 업무 단건 상세 조회
    // 특정 업무의 상세 데이터를 조회하며, 존재하지 않을 경우 예외 발생
    @Override
    public TaskResponseDTO getTaskDetail(Long taskId) {
        log.info("--- ProjectTaskServiceImpl getTaskDetail: 업무 ID {} ---", taskId);
        TaskResponseDTO task = projectMapper.selectTaskByIdWithAssignee(taskId);

        if (task == null) {
            throw new EntityNotFoundException("업무를 찾을 수 없습니다. ID: " + taskId);
        }

        return task;
    }

    // 캘린더용 활성 업무 조회
    // 프로젝트 상태가 유효한 업무들만 필터링하여 MyBatis를 통해 조회
    @Override
    public List<TaskResponseDTO> getActiveTaskList(Long projectId) {
        log.info("--- ProjectTaskServiceImpl getActiveTaskList (활성 전용): 프로젝트 ID {} ---", projectId);
        return projectMapper.selectActiveTasksWithAssignee(projectId);
    }

    // 업무 등록 및 담당자 알림 전송
    // 새로운 업무를 할당하고, 담당 사원에게 프로젝트명과 업무명이 포함된 알림 발송
    @Override
    @Transactional
    public Long registerTask(ProjectTaskRequestDTO dto) {
        log.info("--- ProjectTaskServiceImpl registerTask: {} ---", dto.getTitle());

        ProjectTask task = ProjectTask.builder()
                .projectId(dto.getProjectId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .assigneeNo(dto.getAssigneeNo())
                .status(TaskStatus.TODO)
                .priority(dto.getPriority())
                .dueOn(dto.getDueOn())
                .progressRate(dto.getProgressRate() != null ? dto.getProgressRate() : 0)
                .isCritical(dto.getIsCritical() != null ? dto.getIsCritical() : false)
                .assignedBy(dto.getAssignedBy())
                .build();

        ProjectTask savedTask = projectTaskRepository.save(task);

        // 알림 로직
        if (savedTask.getAssigneeNo() != null) {
            // 프로젝트 명 조회 후 알림 전송
            String projectName = projectRepository.findById(dto.getProjectId())
                    .map(p -> p.getName())
                    .orElse("프로젝트");

            notificationService.createNotification(
                    savedTask.getAssigneeNo(),
                    "새 업무 배정",
                    "[" + projectName + "] 프로젝트에서 '" + savedTask.getTitle() + "' 업무가 배정되었습니다.",
                    String.valueOf(savedTask.getId()),
                    NotificationRefType.PROJECT_TASK
            );
        }
        return savedTask.getId();
    }

    // 업무 정보 업데이트
    // JPA 엔티티를 조회하여 필드를 수정하며, Dirty Checking을 통해 변경 사항 저장
    @Override
    @Transactional
    public void updateTask(Long taskId, ProjectTaskRequestDTO dto) {
        log.info("--- ProjectTaskServiceImpl updateTask: 업무 ID {} ---", taskId);
        ProjectTask task = projectTaskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("업무를 찾을 수 없습니다. ID: " + taskId));

        task.updateInfo(
                dto.getTitle(),
                dto.getDescription(), // 상세 설명 수정 반영
                dto.getPriority(),
                dto.getDueOn()
        );
    }

    // 업무 데이터 삭제
    // 특정 업무 ID가 존재하는지 확인한 후 영구적으로 삭제
    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        log.info("--- ProjectTaskServiceImpl deleteTask: 업무 ID {} ---", taskId);
        if (!projectTaskRepository.existsById(taskId)) {
            throw new EntityNotFoundException("삭제할 업무가 존재하지 않습니다. ID: " + taskId);
        }
        projectTaskRepository.deleteById(taskId);
    }

    // 사원별 참여 중인 프로젝트 목록 조회
    // 특정 사원의 번호를 통해 해당 사원이 멤버로 등록된 프로젝트 리스트 반환
    @Override
    public List<ProjectResponseDTO> getProjectsByEmployee(String empNo) {
        log.info("--- MyBatis 활용: 사용자가 참여 중인 프로젝트 목록 조회 (사번: {}) ---", empNo);

        // 1. MyBatis Mapper를 통해 데이터 조회
        List<ProjectResponseDTO> projects = projectMapper.selectProjectsByEmpNo(empNo);

        // 2. 결과를 담을 리스트 생성
        List<ProjectResponseDTO> dtoList = new ArrayList<>();

        // 3. forEach를 사용하여 리스트에 추가 (가공이 필요한 경우 여기서 수행)
        projects.forEach(project -> {
            // 프로젝트 이름이 비어있을 경우에 대한 예외 처리 등을 할 수 있습니다.
            if (project.getName() == null) {
                project.setName("이름 없는 프로젝트 (" + project.getId() + ")");
            }
            dtoList.add(project);
        });

        return dtoList;
    }
}
