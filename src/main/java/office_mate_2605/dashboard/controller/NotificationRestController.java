package office_mate_2605.dashboard.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.dashboard.mapper.NotificationMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 사용자 알림 관리 API Controller (작성자: 강수현)

 * <p>시스템 내에서 발생한 업무 배정, 프로젝트 초대 등의 알림 데이터를 조회하고 읽음 상태를 관리합니다.
 * 실시간 알림 팝업 및 대시보드 알림 기능을 위한 미확인 알림 데이터 수신 API를 제공합니다.</p>
 */

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Log4j2
public class NotificationRestController {
    private final NotificationMapper notificationMapper;

    /*
     * 읽지 않은 최신 알림 조회
     - /api/notifications/unread (GET)
     - 특정 사원의 읽지 않은 알림 중 가장 최근 데이터 1건 조회
     */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadNotification(@RequestParam("empNo") String empNo) {
        log.info("=== NotificationRestController getUnreadNotification ===");
        log.info("Checking unread notification for employee: {}", empNo);

        Map<String, Object> notification = notificationMapper.selectUnreadTaskNotification(empNo);

        // 알림이 없으면 204 No Content를 보내서 프론트엔드에서 처리하기 쉽게 함
        if (notification == null || notification.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(notification);
    }

    /*
     * 알림 읽음 상태 변경
     - /api/notifications/{id}/read (PATCH)
     - 특정 알림의 식별자(ID)를 통해 해당 알림을 확인 완료(읽음) 상태로 변경
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") Long id) {
        log.info("=== NotificationRestController markAsRead ===");
        log.info("Marking notification {} as read", id);

        notificationMapper.updateNotificationRead(id);
        return ResponseEntity.ok().build();
    }
}
