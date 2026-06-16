package office_mate_2605.calender.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import office_mate_2605.calender.domain.CalendarColor;
import office_mate_2605.calender.domain.CalendarEvent;
import office_mate_2605.calender.domain.CalendarScope;
import office_mate_2605.calender.domain.RecurrenceType;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 일정 데이터 DTO (작성자: 강수현)

 * <p> 캘린더 서비스 내의 일정 정보를 계층 간 전달하기 위해 사용되며,
 * FullCalendar 라이브러리와의 호환성을 위한 색상 변환 로직 및 엔티티(Entity)와의 상호 변환 기능을 포함합니다.</p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDTO {
    private Long id; // 일정 고유 식별자
    private String googleEventId; // 구글 캘린더 이벤트 ID (연동용)
    private Long parentId; // 부모 일정 ID (반복 일정의 원본 식별자)

    private String title; // 일정 제목
    private String description; // 일정 상세 설명

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime startsAt; // 일정 시작 일시

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime endsAt; // 일정 종료 일시

    private boolean isAllDay; // 종일 일정 여부

    private CalendarScope scope; // 일정 공개 범위
    private String ownerNo; // 일정 소유자 사원 번호
    private String ownerName; // 일정 소유자 이름 (표시용)
    private String department; // 소속 부서 코드
    private String departmentName; // 소속 부서 이름 (표시용)
    private Long projectId; // 연관 프로젝트 식별자
    private String projectName; // 연관 프로젝트 이름 (표시용)
    private String createdBy; // 일정 등록자 사원 번호
    private String colorId; // 일정 색상 식별자 (클라이언트 전달 ID)

    // 일정 색상 코드 반환
    // FullCalendar 호환을 위해 colorId(ID 또는 Enum명)를 16진수 색상 코드(Hex Code)로 변환하여 반환
    @JsonProperty("color")          // FullCalendar 호환을 위해 필드명 "color"로 직렬화
    public String getColor() {      // 조회 시 Enum의 Hex 코드를 반환
        if (this.colorId == null) return null;
        try {
            // colorId가 숫자("9")라면 매핑, Enum 문자열("BLUEBERRY")이라면 valueOf로 처리
            if (this.colorId.matches("\\d+")) {
                return CalendarColor.fromId(this.colorId).getHexCode();
            }
            return CalendarColor.valueOf(this.colorId).getHexCode();
        } catch (Exception e) {
            return "#5484ed"; // 기본 블루베리 색상
        }
    }

    private List<String> recurrence; // 반복 규칙 (DAILY, WEEKLY 등)
    private Integer count; // 반복 횟수

    // DTO를 엔티티로 변환
    // 저장 및 수정을 위해 DTO의 필드값과 Enum 타입(반복 규칙, 색상 등)을 CalendarEvent 엔티티 객체로 맵핑
    public CalendarEvent toEntity() {
        // 반복 규칙 리스트의 첫 번째 값을 Enum으로 변환
        RecurrenceType rule = (recurrence != null && !recurrence.isEmpty())
                ? RecurrenceType.valueOf(recurrence.get(0))
                : null;

        return CalendarEvent.builder()
                .id(this.id)
                .parentId(this.parentId)
                .ownerNo(this.ownerNo)
                .department(this.department)
                .projectId(this.projectId)
                .createdBy(this.createdBy)
                .title(this.title)
                .description(this.description)
                .startsAt(this.startsAt)
                .endsAt(this.endsAt)
                .isAllDay(this.isAllDay)
                .scope(this.scope)
                .googleEventId(this.googleEventId)
                .colorType(CalendarColor.fromId(this.colorId)) // 숫자 ID로 Enum 매핑
                .recurrenceRule(rule)
                .recurrenceCount(this.count)
                .build();
    }

    // 엔티티를 DTO로 변환
    // DB에서 조회한 CalendarEvent 엔티티 정보를 API 응답에 적합한 CalendarDTO 형태로 변환
    public static CalendarDTO fromEntity(CalendarEvent entity) {
        return CalendarDTO.builder()
                .id(entity.getId())
                .parentId(entity.getParentId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startsAt(entity.getStartsAt())
                .endsAt(entity.getEndsAt())
                .isAllDay(entity.isAllDay())
                .scope(entity.getScope())
                .ownerNo(entity.getOwnerNo())
                .googleEventId(entity.getGoogleEventId())
                .colorId(entity.getColorType() != null ? entity.getColorType().getId() : "9")
                .recurrence(entity.getRecurrenceRule() != null
                        ? Collections.singletonList(entity.getRecurrenceRule().name())
                        : null)
                .count(entity.getRecurrenceCount())
                .build();
    }
}
