package office_mate_2605.project.domain;

/**
 * 프로젝트 진행 상태 Enum (작성자: 강수현)

 * <p> 프로젝트의 기획부터 완료 및 취소까지의 단계별 상태(READY, IN_PROGRESS, DONE 등)를 정의합니다.
 * 대시보드 통계 산출 및 프로젝트 흐름 제어의 기준값으로 사용됩니다. </p>
 */

public enum ProjectStatus {
    READY,          // 준비
    IN_PROGRESS,    // 진행 중
    DELAYED,        // 지연
    ON_HOLD,        // 보류
    DONE,           // 완료
    CANCELED        // 취소
}
