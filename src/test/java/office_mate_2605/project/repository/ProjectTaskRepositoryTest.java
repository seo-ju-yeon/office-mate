package office_mate_2605.project.repository;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.project.domain.ProjectTask;
import office_mate_2605.project.domain.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class ProjectTaskRepositoryTest {
    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Test
    void saveAndGetTaskLogTest() {
        ProjectTask newTask = ProjectTask.builder()
                .projectId(1L)
                .assigneeNo("MS001")
                .title("로그 출력 테스트 업무")
                .description("JPA 저장 후 로그로 데이터를 확인합니다.")
                .status(TaskStatus.TODO)
                .progressRate(0)
                .priority("HIGH")
                .dueOn(LocalDate.now().plusDays(3))
                .isCritical(true)
                .build();

        ProjectTask savedTask = projectTaskRepository.save(newTask);
        Optional<ProjectTask> foundTaskOpt = projectTaskRepository.findById(savedTask.getId());

        if (foundTaskOpt.isPresent()) {
            ProjectTask task = foundTaskOpt.get();
            log.info("---  DB 데이터 확인 ---");
            log.info("  업무 ID     : {}", task.getId());
            log.info("  프로젝트 ID : {}", task.getProjectId());
            log.info("  제목        : {}", task.getTitle());
            log.info("  담당자 사번 : {}", task.getAssigneeNo());
            log.info("  상태 (ENUM) : {}", task.getStatus());
            log.info("  우선순위    : {}", task.getPriority());
            log.info("  진행률      : {}%", task.getProgressRate());
            log.info("  마감일      : {}", task.getDueOn());
            log.info("  중요경로    : {}", task.isCritical() ? "Yes" : "No");
        } else {
            log.error("데이터 저장에 실패했거나 조회되지 않습니다.");
        }
    }

    @Test
    void updateTaskLogTest() {
        ProjectTask task = projectTaskRepository.save(ProjectTask.builder()
                .projectId(1L)
                .title("상태 변경 전 업무")
                .status(TaskStatus.TODO)
                .priority("NORMAL")
                .progressRate(10)
                .isCritical(false)
                .build());

        log.info("==================================================");
        log.info("[Test] 수정 전 상태: {} (진행률: {}%)", task.getStatus(), task.getProgressRate());

        ProjectTask updateTarget = ProjectTask.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .title("상태 변경 완료 업무")
                .status(TaskStatus.IN_PROGRESS)
                .priority("NORMAL") // <--- 유지
                .progressRate(75)
                .isCritical(false)
                .build();

        projectTaskRepository.save(updateTarget);

        ProjectTask result = projectTaskRepository.findById(task.getId()).orElse(null);

        if (result != null) {
            log.info("--- 변경된 데이터 확인 ---");
            log.info("  변경된 제목   : {}", result.getTitle());
            log.info("  변경된 상태   : {}", result.getStatus());
            log.info("  변경된 진행률 : {}%", result.getProgressRate());
        }
    }


}