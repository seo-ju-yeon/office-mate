package office_mate_2605.calender.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 캘린더 일정 범위 구분 Enum (작성자: 강수현)

 * <p> 일정의 성격에 따라 개인(PERSONAL), 부서(TEAM), 프로젝트(PROJECT)로 분류합니다.
 * 문자열 입력값을 기반으로 적절한 일정 범위 상수를 매핑하는 기능을 수행합니다. </p>
 */

@Getter
@RequiredArgsConstructor
public enum CalendarScope {
    PERSONAL("개인 일정"),
    PROJECT("프로젝트 일정");

    private final String description;

    public static CalendarScope fromString(String text) {
        for (CalendarScope scope : CalendarScope.values()) {
            if (scope.name().equalsIgnoreCase(text)) {
                return scope;
            }
        }
        throw new IllegalArgumentException("일치하는 일정 범위가 없습니다: " + text);
    }
}
