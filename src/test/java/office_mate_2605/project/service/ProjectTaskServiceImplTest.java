package office_mate_2605.project.service;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.project.dto.ProjectTaskRequestDTO;
import office_mate_2605.project.dto.TaskResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class ProjectTaskServiceImplTest {
    @Autowired
    private ProjectTaskService projectTaskService;

    @Test
    void registerAndGetListTest() {
        Long testProjectId = 3L;
        ProjectTaskRequestDTO requestDTO = ProjectTaskRequestDTO.builder()
                .projectId(testProjectId)
                .title("서비스 레이어 테스트 업무")
                .assigneeNo("BE001")
                .priority("HIGH")
                .isCritical(true)
                .build();

        Long savedId = projectTaskService.registerTask(requestDTO);
        log.info("[Result] 업무 등록 완료 - 생성된 ID: {}", savedId);

        List<TaskResponseDTO> taskList = projectTaskService.getTaskList(testProjectId);

        log.info("[Result] 조회된 업무 개수: {}", taskList.size());

        if (!taskList.isEmpty()) {
            log.info("--- [조회 리스트 상세 정보] ---");
            taskList.forEach(dto -> {
                log.info("업무명: {}", dto.getTitle());
                log.info("상태: {}, 진행률: {}%", dto.getStatus(), dto.getProgressRate());
                log.info("담당자명(JOIN): {}", dto.getAssigneeName()); // employee 테이블 JOIN 결과
                log.info("부서명(JOIN): {}", dto.getDepartmentName());   // department 테이블 JOIN 결과
                log.info("------------------------------------------");
            });
        } else {
            log.warn("[Check] 해당 프로젝트에 등록된 업무가 없거나 JOIN 조건이 맞지 않습니다.");
        }
        log.info("==================== [END] 등록 및 목록 조회 테스트 ====================");
    }

    @Test
    void getTaskDetailTest() {
        ProjectTaskRequestDTO requestDTO = ProjectTaskRequestDTO.builder()
                .projectId(1L)
                .title("상세 조회용 업무")
                .priority("NORMAL")
                .build();
        Long savedId = projectTaskService.registerTask(requestDTO);
        log.info("[Step] 테스트 데이터 등록 완료 (ID: {})", savedId);

        TaskResponseDTO detail = projectTaskService.getTaskDetail(savedId);

        log.info("--- [단건 상세 조회 결과] ---");
        log.info("조회된 제목: {}", detail.getTitle());
        log.info("조회된 우선순위: {}", detail.getPriority());
        log.info("중요 업무 여부: {}", detail.isCritical());
    }

}