package office_mate_2605.dashboard.domain;

/**
 * 알림 참조 대상 타입 구분 Enum (작성자: 강수현)

 * <p> 알림이 발생한 원본 소스의 종류(프로젝트, 업무, 일정, 채팅 등)를 정의합니다.
 * 알림 클릭 시 해당 도메인의 상세 페이지로 올바르게 이동하기 위한 식별 정보로 활용됩니다. </p>
 */

public enum NotificationRefType {
    PROJECT,            // 프로젝트
    PROJECT_TASK,       // 프로젝트 업무
}
