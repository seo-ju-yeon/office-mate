package office_mate_2605.calender.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 일정 반복 주기 설정 Enum (작성자: 강수현)

 * <p> 일정이 매일, 매주, 매달 단위로 반복되는지 정의하는 상수를 관리합니다.
 * 반복 일정 생성 로직에서 다음 일정의 날짜를 계산하는 기준 데이터로 활용됩니다. </p>
 */

@Getter
@RequiredArgsConstructor
public enum RecurrenceType {
    DAILY("매일"),
    WEEKLY("매주"),
    MONTHLY("매달");

    private final String description;
}
