package office_mate_2605.common.employee.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 직원 소속 부서 코드를 정의하는 Enum. (작성자: 서주연)
 *
 * <p>PostgreSQL department_code type과 1:1로 매핑되며,
 * 각 부서 코드가 화면에서 사용할 한글 표시명을 함께 가진다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum DepartmentCode {

    FRONTEND("프론트엔드팀"),
    BACKEND("백엔드팀"),
    MANAGEMENT_SUPPORT("경영지원팀"),
    MARKETING("마케팅팀"),
    DB_MANAGEMENT("DB관리팀");

    private final String displayName;
}
