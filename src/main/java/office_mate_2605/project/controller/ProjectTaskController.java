package office_mate_2605.project.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.dashboard.service.DashboardService;
import office_mate_2605.project.dto.ProjectResponseDTO;
import office_mate_2605.project.dto.ProjectTaskRequestDTO;
import office_mate_2605.project.dto.TaskResponseDTO;
import office_mate_2605.project.service.ProjectService;
import office_mate_2605.project.service.ProjectTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프로젝트 업무 관리 API Controller (작성자: 강수현)

 * <p> 프로젝트 내 개별 업무(Task)의 CRUD와 담당자 배정, 진척 상황 관리를 처리합니다.
 * 본인에게 할당된 업무 목록 조회 및 참여 중인 프로젝트 리스트 제공 등 협업을 위한 REST API를 담당합니다.</p>
 */

@Log4j2
@RestController
@RequestMapping("/api/projects/tasks")
@RequiredArgsConstructor
public class ProjectTaskController {
    private final ProjectTaskService projectTaskService;
    private final DashboardService dashboardService; // DashboardService 주입 추가

    /*
     * 본인에게 할당된 업무 조회
     - /api/projects/tasks/assigned (GET)
     - 현재 로그인한 사용자에게 배정된 모든 프로젝트 업무 리스트 조회
     */
    @GetMapping("/assigned")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TaskResponseDTO>> getMyTasks(@RequestParam("empNo") String empNo) {
        log.info("API - 내 업무 목록 조회 요청 (사번: {})", empNo);
        List<TaskResponseDTO> tasks = dashboardService.getMyTasks(empNo);
        return ResponseEntity.ok(tasks);
    }

    /*
     * 프로젝트별 업무 목록 조회
     - /api/projects/tasks/{projectId} (GET)
     - 특정 프로젝트의 업무들을 조회하며, filter=active 파라미터 시 진행 중인 업무만 반환
     */
    @GetMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER') or #filter == 'active'")
    public ResponseEntity<List<TaskResponseDTO>> getTaskList(
            @PathVariable Long projectId,
            @RequestParam(value = "filter", required = false) String filter) {

        log.info("API - 프로젝트({}) 업무 목록 조회 요청 (필터: {})", projectId, filter);

        List<TaskResponseDTO> tasks;

        // 'active' 필터가 있으면 캘린더용(활성 전용) 메서드 호출
        if ("active".equals(filter)) {
            tasks = projectTaskService.getActiveTaskList(projectId);
        } else {
            tasks = projectTaskService.getTaskList(projectId);
        }
        return ResponseEntity.ok(tasks);
    }

    /*
     * 업무 상세 정보 조회
     - /api/projects/tasks/detail/{taskId} (GET)
     - 특정 업무의 상세 내용, 담당자, 마감일 등의 정보 조회
     */
    @GetMapping("/detail/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<TaskResponseDTO> getTaskDetail(@PathVariable Long taskId) {
        log.info("API - 업무 상세 조회 요청 (ID: {})", taskId);
        TaskResponseDTO task = projectTaskService.getTaskDetail(taskId);
        return ResponseEntity.ok(task);
    }

    /*
     * 신규 업무 등록
     - /api/projects/tasks (POST)
     - 새로운 프로젝트 업무를 생성하고 담당자에게 업무 배정 알림을 발송
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<Long> registerTask(@RequestBody ProjectTaskRequestDTO requestDTO) {

         String loginEmpNo = SecurityContextHolder.getContext().getAuthentication().getName();

        // DTO에 배정자 사번 세팅
        requestDTO.setAssignedBy(loginEmpNo);

        log.info("API - 신규 업무 등록 요청: [제목: {}, 배정자: {}, 설명 요약: {}]",
                requestDTO.getTitle(),
                requestDTO.getAssignedBy(),
                requestDTO.getDescription() != null ? requestDTO.getDescription().substring(0, Math.min(requestDTO.getDescription().length(), 10)) + "..." : "없음");

        Long savedId = projectTaskService.registerTask(requestDTO);
        return ResponseEntity.ok(savedId);
    }

    /*
     * 업무 내용 수정
     - /api/projects/tasks/{taskId} (PUT)
     - 기존 업무의 제목, 설명, 우선순위, 마감일 등을 업데이트
     */
    @PutMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<Void> updateTask(@PathVariable Long taskId, @RequestBody ProjectTaskRequestDTO dto) {
        log.info("API - 업무 수정 요청 (ID: {}, 수정 제목: {})", taskId, dto.getTitle());
        projectTaskService.updateTask(taskId, dto);
        return ResponseEntity.ok().build();
    }

    /*
     * 업무 삭제
     - /api/projects/tasks/{taskId} (DELETE)
     - 업무 식별자(ID)를 통해 해당 업무 데이터 영구 삭제
     */
    @DeleteMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        log.info("API - 업무 삭제 요청 (ID: {})", taskId);
        projectTaskService.deleteTask(taskId);
        return ResponseEntity.ok().build();
    }

    /*
     * 본인 참여 프로젝트 목록 조회
     - /api/projects/tasks/my-projects (GET)
     - 사원이 멤버로 등록되어 있는 프로젝트 리스트 조회 (캘린더 필터용)
     */
    @GetMapping("/my-projects")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProjectResponseDTO>> getMyProjects(@RequestParam String empNo) {
        log.info("API - 사용자가 참여 중인 프로젝트 목록 조회: {}", empNo);
        // 서비스에서 사용자가 속한 프로젝트 리스트를 가져오는 로직 호출
        List<ProjectResponseDTO> projects = projectTaskService.getProjectsByEmployee(empNo);
        return ResponseEntity.ok(projects);
    }
}
