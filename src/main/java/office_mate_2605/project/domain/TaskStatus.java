package office_mate_2605.project.domain;

/**
 * 세부 업무 상태 Enum (작성자: 강수현)

 * <p> 개별 업무의 수행 단계(할 일, 진행 중, 완료)를 정의합니다.
 * 업무의 실시간 처리 현황을 파악하고 대시보드 통계에 반영하기 위한 식별자로 사용됩니다. </p>
 */

public enum TaskStatus {
    TODO,           // 할 일
    IN_PROGRESS,    // 진행 중
    DONE            // 완료
}
