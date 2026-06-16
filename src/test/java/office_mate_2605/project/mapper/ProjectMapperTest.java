package office_mate_2605.project.mapper;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.project.domain.ProjectJoinResult;
import office_mate_2605.project.domain.ProjectTask;
import office_mate_2605.project.domain.TaskStatus;
import office_mate_2605.project.dto.EmployeeResponseDTO;
import office_mate_2605.project.dto.ProjectMemberResponseDTO;
import office_mate_2605.project.dto.TaskResponseDTO;
import office_mate_2605.project.repository.ProjectTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class ProjectMapperTest {
    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectTaskRepository projectTaskRepository;

    @Test
    void selectProjectsWithDetailsTest() {
        List<ProjectJoinResult> results = projectMapper.selectProjectsWithDetails();

        log.info("====================================================");
        log.info("조회 결과 보고서");
        log.info("총 프로젝트 수: {}건", results.size());
        log.info("====================================================");

        if (results.isEmpty()) {
            log.warn("조회된 데이터가 없습니다. (INNER JOIN 조건을 확인하세요)");
            log.warn("확인 사항: project 테이블의 manager_no와 owner_department가 각각 employee, departments 테이블에 존재하는지!");
        } else {
            results.forEach(p -> {
                log.info("[프로젝트 ID: {}]", p.getId());
                log.info(" - 명칭: {}", p.getName());
                log.info(" - 상태: {} (진행률: {}%)", p.getStatus(), p.getProgressRate());
                log.info(" - 담당자: {}", p.getManagerName());
                log.info(" - 주관부서: {}", p.getDepartmentName());
                log.info(" - 기간: {} ~ {}", p.getStartsOn(), p.getEndsOn());
                log.info("----------------------------------------------------");
            });
        }
        log.info("====================================================");
    }

    @Test
    void selectProjectMemberDetailsTest() {
        Long projectId = 1L;

        log.info("MyBatis selectProjectMemberDetails 호출");
        List<ProjectMemberResponseDTO> members = projectMapper.selectProjectMemberDetails(projectId);

        if (members != null && !members.isEmpty()) {
            log.info("JOIN 조회 결과 발견! (인원수: {}명)", members.size());

            members.forEach(m -> {
                log.info("--------------------------------------------------");
                log.info("프로젝트 ID: {}", m.getProjectId());
                log.info("사번: {}", m.getEmployeeNo());
                log.info("사원명(JOIN): {}", m.getEmployeeName());
                log.info("부서명(JOIN): {}", m.getDepartmentName());
                log.info("참여일: {}", m.getJoinedAt());
                log.info("--------------------------------------------------");
            });
        } else {
            log.error("JOIN 조회 결과가 없습니다. DB에 해당 사번의 직원이 있는지 확인하세요.");
        }
    }

    @Test
    void selectAllEmployeesWithDeptTest() {
        // 1. 전체 조회 테스트
        log.info("========== 전체 직원 조회 시작 ==========");
        List<EmployeeResponseDTO> allList = projectMapper.selectAllEmployeesWithDept(null);

        if (allList.isEmpty()) {
            log.info("조회된 직원이 없습니다.");
        } else {
            allList.forEach(emp -> {
                // getDepartmentD() -> getDepartmentCode()로 수정
                log.info("직원 정보: 사번={}, 이름={}, 부서={}, 부서코드={}",
                        emp.getEmployeeNo(),
                        emp.getEmployeeName(),
                        emp.getDepartmentName(),
                        emp.getDepartmentCode());
            });
        }
        log.info("전체 직원 수: {}명", allList.size());

        // 2. 부서 필터링 테스트 (예: BACKEND)
        String filterDept = "BACKEND";
        log.info("========== 부서 필터링 조회 시작 ({}) ==========", filterDept);
        List<EmployeeResponseDTO> filteredList = projectMapper.selectAllEmployeesWithDept(filterDept);

        if (filteredList.isEmpty()) {
            log.info("해당 부서에 소속된 직원이 없습니다.");
        } else {
            filteredList.forEach(emp -> {
                log.info("[필터 결과] 이름: {}, 부서: {}", emp.getEmployeeName(), emp.getDepartmentName());
            });
        }
        log.info("해당 부서 직원 수: {}명", filteredList.size());
    }

    @Test
    void selectTasksWithAssigneeTest() {
        Long testProjectId = 1L;

        ProjectTask task = ProjectTask.builder()
                .projectId(testProjectId)
                .assigneeNo("BE001")
                .title("MyBatis JOIN 테스트 업무")
                .status(TaskStatus.IN_PROGRESS)
                .priority("HIGH")
                .progressRate(30)
                .isCritical(true)
                .build();

        projectTaskRepository.save(task);

        log.info("==================================================");
        log.info("[Test] MyBatis JOIN 쿼리 실행 (Project ID: {})", testProjectId);

        List<TaskResponseDTO> taskList = projectMapper.selectTasksWithAssignee(testProjectId);

        if (!taskList.isEmpty()) {
            log.info("--- MyBatis + JOIN 데이터 ---");
            taskList.forEach(dto -> {
                log.info("업무 ID: {}, 제목: {}", dto.getId(), dto.getTitle());
                log.info("상태(ENUM): {}, 진행률: {}%", dto.getStatus(), dto.getProgressRate());
                log.info(">> 담당자 이름(JOIN): {}", dto.getAssigneeName()); // e.name
                log.info(">> 부서명(JOIN): {}", dto.getDepartmentName());    // d.name
                log.info("------------------------------------------");
            });
        } else {
            log.warn("조회된 업무 데이터가 없습니다. Project ID와 JOIN 조건을 확인하세요.");
        }
        log.info("==================================================");
    }


}