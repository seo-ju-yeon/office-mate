package office_mate_2605.calender.repository;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.calender.domain.CalendarEvent;
import office_mate_2605.calender.domain.CalendarScope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class CalendarEventRepositoryTest {
    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Test
    void saveAndLogTest() {
        CalendarEvent event = CalendarEvent.builder()
                .title("로그 출력 테스트 회의")
                .description("DB 저장 및 로그 확인용")
                .startsAt(OffsetDateTime.now())
                .endsAt(OffsetDateTime.now().plusHours(2))
                .scope(CalendarScope.PERSONAL)
                .ownerNo("SUPER001")
                .createdBy("SUPER001")
                .isAllDay(false)
                .build();

        CalendarEvent savedEvent = calendarEventRepository.save(event);

        log.info("====================================================");
        log.info("원 데이터 ID: {}", event.getId()); // null (save 전이면 null일 수 있음)
        log.info("저장 후 ID: {}", savedEvent.getId());
        log.info("일정 제목: {}", savedEvent.getTitle());
        log.info("일정 범위: {}", savedEvent.getScope());
        log.info("시작 시간: {}", savedEvent.getStartsAt());
        log.info("종료 시간: {}", savedEvent.getEndsAt());
        log.info("====================================================");
    }

    @Test
    void findByOwnerNoTest() {
        calendarEventRepository.save(CalendarEvent.builder()
                .title("오전 회의")
                .startsAt(OffsetDateTime.now())
                .endsAt(OffsetDateTime.now().plusHours(1))
                .scope(CalendarScope.PERSONAL)
                .ownerNo("SUPER001")
                .createdBy("SUPER001")
                .build());

        calendarEventRepository.save(CalendarEvent.builder()
                .title("오후 업무")
                .startsAt(OffsetDateTime.now().plusHours(4))
                .endsAt(OffsetDateTime.now().plusHours(5))
                .scope(CalendarScope.PERSONAL)
                .ownerNo("SUPER001")
                .createdBy("SUPER001")
                .build());

        List<CalendarEvent> events = calendarEventRepository.findByOwnerNo("SUPER001");

        log.info("====================================================");
        log.info("사번 {} 의 일정 개수: {}", "SUPER001", events.size());
        events.forEach(e -> log.info("조회된 일정: {}", e.getTitle()));
        log.info("====================================================");
    }

    @Test
    void findByDateRangeTest() {
        OffsetDateTime now = OffsetDateTime.now();
        calendarEventRepository.save(CalendarEvent.builder()
                .title("오늘의 일정")
                .startsAt(now)
                .endsAt(now.plusHours(1))
                .scope(CalendarScope.PERSONAL)
                .ownerNo("SUPER001")
                .createdBy("SUPER001")
                .build());

        OffsetDateTime startOfToday = now.withHour(0).withMinute(0).withSecond(0);
        OffsetDateTime endOfToday = now.withHour(23).withMinute(59).withSecond(59);

        List<CalendarEvent> events = calendarEventRepository.findByStartsAtBetween(startOfToday, endOfToday);

        log.info("====================================================");
        log.info("검색 범위: {} ~ {}", startOfToday, endOfToday);
        log.info("범위 내 일정 개수: {}", events.size());
        log.info("====================================================");
    }
}