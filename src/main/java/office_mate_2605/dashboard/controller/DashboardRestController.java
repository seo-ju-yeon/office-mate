package office_mate_2605.dashboard.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 대시보드 데이터 관리 API Controller (작성자: 강수현)

 * <p>사용자의 업무 통계(진행 중, 마감 임박, 지연 건수) 조회 및 개별 업무의 진척도 수정을 처리합니다.
 * 대시보드 화면 구성에 필요한 데이터 전송과 실시간 업무 상태 변경을 위한 REST API를 제공합니다.</p>
 */

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Log4j2
class DashboardRestController {
    private final DashboardService dashboardService;

    /*
     * 대시보드 통계 데이터 조회
     - /api/dashboard/stats (GET)
     - 사원 번호를 기반으로 진행 중, 오늘 마감, 지연 업무의 건수를 조회하여 반환
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats(@RequestParam("empNo") String empNo) {
        log.info("=== [GET] 대시보드 통계 조회 | 사번: {} ===", empNo);
        Map<String, Long> stats = dashboardService.getDashboardStats(empNo);
        return ResponseEntity.ok(stats);
    }

    /*
     * 업무 진척도 및 상태 수정
     - /api/dashboard/tasks/{taskId} (PATCH)
     - 특정 업무의 진척률과 상태를 수정하며, 비즈니스 로직에 따라 상태를 자동 업데이트
     */
    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, String>> updateTaskProgress(
            @PathVariable("taskId") Long taskId, // @PathVariable 이름을 명시적으로 지정
            @RequestBody Map<String, Object> params) {

        log.info("=== [PATCH] 업무 수정 요청 | ID: {}, 데이터: {} ===", taskId, params);

        try {
            // JSON 바디에서 값 추출
            String status = (String) params.get("status");
            int progressRate = Integer.parseInt(params.get("progressRate").toString());

            // 서비스 호출하여 비즈니스 로직 실행
            dashboardService.updateTaskProgress(taskId, status, progressRate);

            return ResponseEntity.ok(Map.of("message", "업무가 성공적으로 수정되었습니다."));
        } catch (Exception e) {
            log.error("업무 수정 중 오류 발생: ", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "수정 실패: " + e.getMessage()));
        }
    }
}
