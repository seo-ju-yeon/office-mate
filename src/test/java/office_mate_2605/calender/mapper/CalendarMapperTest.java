package office_mate_2605.calender.mapper;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.calender.domain.CalendarScope;
import office_mate_2605.calender.dto.CalendarDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class CalendarMapperTest {
    @Autowired
    private CalendarMapper calendarMapper;
    private final String TEST_EMP_NO = "SUPER001";

    @Test
    void findByScopeTest() {
        // 1. 개인 일정 조회
        log.info(">>>>>> [PERSONAL] 캘린더 조회 시작 <<<<<<");
        List<CalendarDTO> personalEvents = calendarMapper.findAllWithDetails(TEST_EMP_NO, CalendarScope.PERSONAL);
        log.info("개인 일정 개수: {}", personalEvents.size());
        personalEvents.forEach(e -> log.info("결과 - 제목: {}, 타입: {}", e.getTitle(), e.getScope()));

        // 2. 팀 일정 조회
        log.info(">>>>>> [TEAM] 캘린더 조회 시작 <<<<<<");
        List<CalendarDTO> teamEvents = calendarMapper.findAllWithDetails(TEST_EMP_NO, CalendarScope.TEAM);
        log.info("팀 일정 개수: {}", teamEvents.size());
        teamEvents.forEach(e -> log.info("결과 - 제목: {}, 부서명: {}", e.getTitle(), e.getDepartmentName()));

        // 3. 프로젝트 일정 조회
        log.info(">>>>>> [PROJECT] 캘린더 조회 시작 <<<<<<");
        List<CalendarDTO> projectEvents = calendarMapper.findAllWithDetails(TEST_EMP_NO, CalendarScope.PROJECT);
        log.info("프로젝트 일정 개수: {}", projectEvents.size());
        projectEvents.forEach(e -> log.info("결과 - 제목: {}, 프로젝트명: {}", e.getTitle(), e.getProjectName()));
    }
    @Test
    void findDetailByIdTest() {
        List<CalendarDTO> list = calendarMapper.findAllWithDetails(TEST_EMP_NO, CalendarScope.PERSONAL);

        if (!list.isEmpty()) {
            Long targetId = list.get(0).getId();
            log.info(">>>>>> 상세 조회 테스트 시작! 대상 ID: {} <<<<<<", targetId);

            CalendarDTO detail = calendarMapper.findDetailById(targetId);

            log.info("==================== [상세 데이터 결과] ====================");
            if (detail != null) {
                log.info("일정 ID    : {}", detail.getId());
                log.info("일정 제목  : {}", detail.getTitle());
                log.info("상세 설명  : {}", detail.getDescription());
                log.info("소유자 이름: {}", detail.getOwnerName());
                log.info("부서명     : {}", detail.getDepartmentName());
                log.info("프로젝트명 : {}", detail.getProjectName());
                log.info("구글 연동ID: {}", detail.getGoogleEventId());
                log.info("시작 시간  : {}", detail.getStartsAt());
                log.info("종료 시간  : {}", detail.getEndsAt());
            } else {
                log.error("조회 실패: 해당 ID({})의 데이터를 찾을 수 없습니다.", targetId);
            }
            log.info("==========================================================");
        } else {
            log.warn("테스트할 데이터가 없습니다. 먼저 DB에 일정을 등록해 주세요.");
        }
    }

}