package office_mate_2605.calender.service;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.calender.domain.CalendarScope;
import office_mate_2605.calender.dto.CalendarDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class CalendarServiceImplTest {
    @Autowired
    private CalendarService calendarService;
    private final String TEST_EMP_NO = "SUPER001";

    @Test
    void calendarServiceLogTest() {
        log.info("==================== 등록 테스트 ====================");
        CalendarDTO registerDTO = CalendarDTO.builder()
                .title("로그 확인용 일정")
                .description("설명입니다.")
                .startsAt(OffsetDateTime.now())
                .endsAt(OffsetDateTime.now().plusHours(1))
                .scope(CalendarScope.PERSONAL)
                .ownerNo(TEST_EMP_NO)
                .createdBy(TEST_EMP_NO)
                .build();

        Long savedId = calendarService.registerEvent(registerDTO);
        log.info("등록된 일정 ID: {}", savedId);


        log.info("==================== 목록 조회 테스트 ====================");
        List<CalendarDTO> list = calendarService.getEventsByScope(TEST_EMP_NO, CalendarScope.PERSONAL, null, null);
        log.info("조회된 일정 개수: {}", list.size());
        list.forEach(dto -> log.info("조회 결과 -> 제목: {}, 범위: {}", dto.getTitle(), dto.getScope()));


        log.info("==================== 상세 조회 및 수정 테스트 ====================");
        CalendarDTO detail = calendarService.getEventDetail(savedId);
        if (detail != null) {
            log.info("수정 전 제목: {}", detail.getTitle());

            CalendarDTO modifyDTO = CalendarDTO.builder()
                    .id(savedId)
                    .title("수정 완료")
                    .description("수정된 설명")
                    .startsAt(detail.getStartsAt())
                    .endsAt(detail.getEndsAt())
                    .scope(CalendarScope.PERSONAL)
                    .build();

            calendarService.modifyEvent(modifyDTO);

            CalendarDTO modifiedDetail = calendarService.getEventDetail(savedId);
            log.info("수정 후 제목: {}", modifiedDetail.getTitle());
        }


        log.info("==================== 삭제 테스트 ====================");
        calendarService.removeEvent(savedId);
        CalendarDTO deletedDetail = calendarService.getEventDetail(savedId);
        log.info("삭제 후 조회 결과(null이어야 함): {}", deletedDetail);
        log.info("==========================================================");
    }

    @Test
    void scopeFilteringLogTest() {
        log.info("===== 프로젝트 일정 등록 =====");
        calendarService.registerEvent(CalendarDTO.builder()
                .title("필터링 테스트용 프로젝트")
                .scope(CalendarScope.PROJECT)
                .projectId(1L)
                .ownerNo(TEST_EMP_NO)
                .createdBy(TEST_EMP_NO)
                .startsAt(OffsetDateTime.now())
                .endsAt(OffsetDateTime.now().plusHours(1))
                .build());

        log.info("===== PERSONAL 스코프로 조회 결과 =====");
        List<CalendarDTO> personalList = calendarService.getEventsByScope(TEST_EMP_NO, CalendarScope.PERSONAL, null, null);

        boolean found = personalList.stream().anyMatch(e -> e.getTitle().contains("프로젝트"));
        log.info("개인 일정 목록에 프로젝트 일정이 포함되었는가? : {}", found ? "YES (에러)" : "NO (정상)");
    }
}