package office_mate_2605.calender.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 캘린더 일정 정보 Entity (작성자: 강수현)

 * <p> 일정의 제목, 시간, 장소 등 핵심 데이터와 반복 설정, 구글 캘린더 연동 ID를 저장합니다.
 * 개인, 팀, 프로젝트별 일정 범위를 구분하며 비즈니스 로직에 따른 필드 수정 메서드를 포함합니다. </p>
 */

@Entity
@Table(name = "calendar_event")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long parentId;
    private String ownerNo;
    private String department;
    private Long projectId;
    private String createdBy;

    @Column(nullable = false, length = 200)
    private String title;

    private String description;

    @Column(nullable = false)
    private OffsetDateTime startsAt;

    @Column(nullable = false)
    private OffsetDateTime endsAt;

    @Builder.Default
    private boolean isAllDay = false;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "calendar_scope")
    private CalendarScope scope;

    private String googleEventId;

    @Enumerated(EnumType.STRING)
//    @Column(name = "calendar_color", nullable = false)
    @Column(name = "color_type", nullable = false)
    @Builder.Default
    private CalendarColor colorType = CalendarColor.BLUEBERRY;

    @Enumerated(EnumType.STRING)
//    @Column(name = "recurrence_type")
    @Column(name = "recurrence_rule")
    private RecurrenceType recurrenceRule;

    @Column(name = "recurrence_count")
    private Integer recurrenceCount;

    /**
     * 제목 수정
     */
    public void changeTitle(String title) {
        if (title != null && !title.trim().isEmpty()) {
            this.title = title;
        }
    }

    /**
     * 내용 및 시간 정보 수정
     */
    public void changeContent(String description, OffsetDateTime startsAt, OffsetDateTime endsAt) {
        this.description = description;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    /**
     * 일정 범위 수정
     */
    public void changeScopeSettings(CalendarScope scope) {
        this.scope = scope;
    }

    /**
     * 색상 및 반복 설정 수정
     */
    public void changeExtraSettings(CalendarColor colorType, RecurrenceType rule, Integer count) {
        this.colorType = colorType != null ? colorType : this.colorType;
        this.recurrenceRule = rule;
        this.recurrenceCount = count;
    }

    /**
     * 구글 캘린더 ID 동기화
     */
    public void setGoogleEventId(String googleEventId) {
        this.googleEventId = googleEventId;
    }
}
