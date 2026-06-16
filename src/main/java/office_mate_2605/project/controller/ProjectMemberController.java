package office_mate_2605.project.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.project.dto.EmployeeResponseDTO;
import office_mate_2605.project.dto.ProjectMemberResponseDTO;
import office_mate_2605.project.service.ProjectMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 프로젝트 참여 멤버 관리 API Controller (작성자: 강수현)

 * <p> 프로젝트별 참여 인원의 추가, 삭제 및 상세 목록 조회를 처리하며 알림 서비스와 연동합니다.
 * 전체 직원 목록 제공을 통해 프로젝트 멤버 구성 및 초대 프로세스를 지원하는 REST API를 제공합니다.</p>
 */

@Log4j2
@RestController
@RequestMapping("/api/projects")
@PreAuthorize("hasAnyRole('ADMIN','SUPER')")
@RequiredArgsConstructor
public class ProjectMemberController {
    private final ProjectMemberService projectMemberService;

    /*
     * 전체 직원 목록 조회
     - /api/projects/employees (GET)
     - 드롭다운 표시를 위해 부서 필터 조건에 맞는 전체 직원 리스트를 조회
     */
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeResponseDTO>> getAllEmployees(
            @RequestParam(value = "department", required = false) String department) {
        log.info("=== ProjectMemberController getAllEmployees ===");
        log.info("=== 직원 목록 조회 시작 (부서: {}) ===", department);
        List<EmployeeResponseDTO> employees = projectMemberService.getAllEmployees(department);
        return ResponseEntity.ok(employees);
    }

    /*
     * 프로젝트 참여 멤버 목록 조회
     - /api/projects/{projectId}/members (GET)
     - 특정 프로젝트에 소속된 멤버들의 상세 정보 조회
     */
    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<ProjectMemberResponseDTO>> getProjectMembers(@PathVariable Long projectId) {
        log.info("=== ProjectMemberController getProjectMembers ===");
        log.info("=== 프로젝트 멤버 조회 (ID: {}) ===", projectId);
        List<ProjectMemberResponseDTO> members = projectMemberService.getMembersByProjectId(projectId);
        return ResponseEntity.ok(members);
    }

    /*
     * 프로젝트 멤버 추가
     - /api/projects/{projectId}/members (POST)
     - 프로젝트에 새 멤버를 등록하고, 해당 인원에게 초대 알림 발송
     */
    @PostMapping("/{projectId}/members")
    public ResponseEntity<?> addMember(
            @PathVariable Long projectId,
            @RequestBody Map<String, String> payload) {

        String employeeNo = payload.get("employeeNo");

        try {
            // 이 메서드 실행 시 내부에서 프로젝트명 조회 + 멤버 추가 + 알림 생성이 한 번에 실행
            projectMemberService.addMember(projectId, employeeNo);
            return ResponseEntity.ok().body(Map.of("message", "멤버가 추가되었습니다."));
        } catch (RuntimeException e) {
            // "이미 등록된 멤버입니다" 등의 메시지를 프론트에 400 에러와 함께 전달
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /*
     * 프로젝트 멤버 삭제
     - /api/projects/{projectId}/members/{employeeNo} (DELETE)
     - 프로젝트 참여 명단에서 특정 사원을 제외 처리
     */
    @DeleteMapping("/{projectId}/members/{employeeNo}")
    public ResponseEntity<?> removeMember(
            @PathVariable Long projectId,
            @PathVariable String employeeNo) {
        log.info("=== ProjectMemberController removeMember ===");
        log.info("=== [Controller] 멤버 삭제 시도: 프로젝트={}, 사번={} ===", projectId, employeeNo);

        try {
            projectMemberService.removeMember(projectId, employeeNo);
            return ResponseEntity.ok().body(Map.of("message", "멤버가 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
