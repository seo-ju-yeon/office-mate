package office_mate_2605.dashboard.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.dashboard.domain.NotificationRefType;
import office_mate_2605.dashboard.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 사용자 알림 관리 관리 Service 구현체 (작성자: 강수현)

 * <p> MyBatis Mapper를 활용하여 알림 데이터를 데이터베이스에 기록하고, 사원별 미확인 알림 정보를 조회합니다.
 * 알림 생성 시 참조 타입과 ID를 포함하여 상세 페이지 이동이 가능하도록 데이터를 구성하며, 읽음 상태 업데이트 기능을 수행합니다.</p>
 */

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper notificationMapper;

    // 알림 데이터 삽입
    // 제목, 메시지, 참조 ID 등을 Map에 담아 MyBatis 매퍼를 통해 알림을 생성
    @Override
    @Transactional
    public void createNotification(String empNo, String title, String message, String refId, NotificationRefType refType) {
        log.info("--- NotificationServiceImpl createNotification ---");
        log.info("--- 알림 생성 시작: 대상자 {}, 타입 {} ---", empNo, refType);

        Map<String, Object> params = Map.of(
                "employeeNo", empNo,
                "title", title,
                "message", message,
                "refType", refType.name(), // Enum의 이름을 문자열로 전달
                "refId", refId
        );

        notificationMapper.insertNotification(params);
    }
}
