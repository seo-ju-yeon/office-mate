package office_mate_2605.common.employee.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 직원 재직 상태를 정의하는 Enum. (작성자: 서주연)
 *
 * <p>재직, 휴직, 퇴사 상태를 구분하며,
 * PostgreSQL employment_status type과 1:1로 매핑된다.
 * 상태 판별을 쉽게 하기 위한 편의 메서드도 함께 제공한다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum EmploymentStatus {

    ACTIVE("재직"),  // 현재 재직 중인 상태
    ON_LEAVE("휴직"),  // 휴직 중인 상태
    RESIGNED("퇴사");  // 퇴사 처리된 상태

    private final String displayName;

    // 재직 중인 상태인지 판단
    public boolean isActive() {
        return this == ACTIVE;
    }

    // 퇴사 상태인지 판단
    public boolean isResigned() {
        return this == RESIGNED;
    }
}
