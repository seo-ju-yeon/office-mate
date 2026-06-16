package office_mate_2605.project.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.project.dto.ProjectDetailResponseDTO;
import office_mate_2605.project.dto.ProjectRequestDTO;
import office_mate_2605.project.service.ProjectService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 프로젝트 기본 정보 및 상태 관리 API Controller (작성자: 강수현)

 * <p> 프로젝트의 생성, 상태 변경(진행/취소), 대시보드용 상세 목록 조회를 처리합니다.
 * 프로젝트의 생명주기를 관리하며 전반적인 진행 현황 데이터를 제공하는 REST API를 담당합니다.</p>
 */

@Log4j2
@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasAnyRole('ADMIN','SUPER')")
@RequiredArgsConstructor
public class ProjectRestController {
    private final ProjectService projectService;

    /*
     * 프로젝트 대시보드 목록 조회
     - /api/projects/dashboard (GET)
     - 대시보드 화면에 표시할 프로젝트의 상세 정보 및 진행률 목록 반환
     */
    @GetMapping("/dashboard")
    public ResponseEntity<List<ProjectDetailResponseDTO>> getProjectDashboardApi() {
        log.info("=== ProjectAPIController getProjectDashboardApi ===");

        // 서비스 계층 호출
        List<ProjectDetailResponseDTO> projectList = projectService.getAllProjectsWithDetails();

        log.info("APIController - {}건의 데이터를 JSON으로 반환합니다.", projectList.size());

        // HTTP 200 상태 코드와 함께 데이터 반환
        return ResponseEntity.ok(projectList);
    }

    /*
     * 신규 프로젝트 생성
     - /api/projects (POST)
     - 프로젝트 기본 정보를 등록하고 생성자를 멤버로 자동 추가
     */
    @PostMapping
    public ResponseEntity<Long> create(@RequestBody ProjectRequestDTO dto) {
        log.info("=== ProjectAPIController create ===");
        log.info("받은 데이터 확인 - 시작일: {}, 마감일: {}", dto.getStartsOn(), dto.getEndsOn());
        Long projectId = projectService.createProject(dto);
        return ResponseEntity.ok(projectId);
    }

    /*
     * 프로젝트 삭제 (취소)
     - /api/projects/{id} (DELETE)
     - 프로젝트를 물리적으로 삭제하지 않고 상태를 'CANCELED'로 변경
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        log.info("=== ProjectAPIController delete ===");
        log.info("=== ProjectAPIController delete ID: {} ===", id);
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    /*
     * 프로젝트 진행 상태 변경
     - /api/projects/{id}/status (PATCH)
     - 프로젝트의 현재 상태(준비, 진행, 완료 등)를 업데이트
     */
    @PatchMapping(value = "/{id}/status", consumes = "application/json")
    public ResponseEntity<Void> updateStatus(@PathVariable("id") Long id, @RequestBody java.util.Map<String, String> statusMap) {
        log.info("=== ProjectAPIController updateStatus ===");
        String newStatus = statusMap.get("status");
        projectService.updateProjectStatus(id, newStatus);
        return ResponseEntity.ok().build();
    }

}
