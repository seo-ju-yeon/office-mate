package office_mate_2605.dashboard.service;

import office_mate_2605.dashboard.domain.NotificationRefType;

import java.util.Map;

/**
 * 사용자 알림 관리 관리 Service (작성자: 강수현)

 * <p> 시스템 내에서 발생하는 업무 배정, 프로젝트 초대 등 주요 이벤트에 대한 알림 생성 및 조회 기능을 정의합니다.
 * 알림의 읽음 처리 및 미확인 알림 추출 등 사용자 알림 경험과 관련된 핵심 비즈니스 명세를 포함합니다.</p>
 */

public interface NotificationService {
    // 신규 알림 생성
    // 업무 배정이나 프로젝트 초대 등 이벤트 발생 시 대상 사원에게 알림 데이터를 저장합니다.
    void createNotification(String empNo, String title, String message, String refId, NotificationRefType refType);
}
