package office_mate_2605.dashboard.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.Map;

/**
 * 사용자 알림 데이터 접근 관리 Mapper(작성자: 강수현)

 * <p> 사원별 미확인 알림 조회, 알림 읽음 처리 및 신규 알림 생성을 위한 데이터베이스 연동을 담당합니다.
 * 업무 배정이나 프로젝트 초대와 같은 시스템 이벤트 발생 시 알림 이력을 기록하고 관리하는 역할을 수행합니다.</p>
 */

@Mapper
public interface NotificationMapper {
    // 최신 미확인 알림 조회
    // 사원 번호를 기반으로 읽지 않은 알림 중 가장 최근의 데이터를 1건 조회
    Map<String, Object> selectUnreadTaskNotification(@Param("empNo") String empNo);

    // 알림 읽음 상태 업데이트
    // 알림 ID를 통해 해당 알림의 확인 여부(is_read)를 true로 변경
    int updateNotificationRead(@Param("id") Long id);

    // 알림 데이터 삽입
    // 업무 배정이나 초대 등 이벤트 발생 시 알림 정보(제목, 메시지, 참조 타입 등) 저장
    int insertNotification(Map<String, Object> params);
}
