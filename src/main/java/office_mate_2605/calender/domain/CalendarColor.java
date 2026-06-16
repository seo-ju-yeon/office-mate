package office_mate_2605.calender.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 캘린더 일정 색상 관리 Enum (작성자: 강수현)

 * <p> 구글 캘린더 API의 색상 ID와 FullCalendar 라이브러리에서 사용할 CSS Hex 코드를 관리합니다.
 * ID를 기반으로 특정 색상 타입을 찾거나 기본값을 반환하는 편의 메서드를 제공합니다. </p>
 */

@Getter
@RequiredArgsConstructor
public enum CalendarColor {
    LAVENDER("1", "#a4bdfc"),
    SAGE("2", "#7ae38f"),
    GRAPE("3", "#bdadff"),
    FLAMINGO("4", "#ff887c"),
    BANANA("5", "#fbd75b"),
    TANGERINE("6", "#ffb878"),
    PEACOCK("7", "#46d6db"),
    GRAPHITE("8", "#e1e1e1"),
    BLUEBERRY("9", "#5484ed"), // 기본값
    BASIL("10", "#51b749"),
    TOMATO("11", "#dc182f");

    private final String id;     // 구글 API용 ID
    private final String hexCode; // FullCalendar CSS용 색상 코드

    // 클라이언트에서 보낸 ID(숫자형 문자열)로 Enum을 찾는 편의 메서드
    public static CalendarColor fromId(String id) {
        for (CalendarColor color : values()) {
            if (color.id.equals(id)) {
                return color;
            }
        }
        return BLUEBERRY; // 찾지 못할 경우 기본값 반환
    }
}
