package office_mate_2605.calender.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.calender.domain.CalendarColor;
import office_mate_2605.calender.domain.CalendarEvent;
import office_mate_2605.calender.domain.CalendarScope;
import office_mate_2605.calender.domain.RecurrenceType;
import office_mate_2605.calender.dto.CalendarDTO;
import office_mate_2605.calender.mapper.CalendarMapper;
import office_mate_2605.calender.repository.CalendarEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 일정 정보 관리 Service 구현체 (작성자: 강수현)
 *
 * <p> 개인, 부서, 프로젝트별 일정의 CRUD 처리와 반복 주기에 따른 일정 자동 생성 로직을 관리합니다.
 * JPA와 MyBatis를 연동하여 일정 데이터를 처리하며, 구글 캘린더 API를 통한 외부 동기화 기능을 제공합니다.</p>
 */

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class CalendarServiceImpl implements CalendarService {
    private final CalendarMapper calendarMapper;
    private final CalendarEventRepository calendarEventRepository;
    private final GoogleCalendarService googleCalendarService;

    // 스코프 기반 일정 리스트 조회
    // MyBatis를 활용하여 사번과 캘린더 범위에 맞는 일정 데이터들을 조회
    @Override
    public List<CalendarDTO> getEventsByScope(String employeeNo, CalendarScope scope, String start, String end) {
        log.info("--- CalendarServiceImpl getEventsByScope ---");
        // MyBatis Mapper를 호출하여 필터링된 DTO 리스트 반환
        return calendarMapper.findAllWithDetails(employeeNo, scope, start, end);
    }

    // 특정 일정 상세 조회
    // 매퍼를 통해 일정의 세부 속성값들을 DTO 형태로 반환
    @Override
    public CalendarDTO getEventDetail(Long id) {
        log.info("--- CalendarServiceImpl getEventDetail ---");
        log.info("일정 상세 조회 요청 - ID: {}", id);
        return calendarMapper.findDetailById(id);
    }

    // 개인 일정 전체 구글 전송
    // 사용자의 개인 일정 리스트를 추출하여 Google Calendar API를 통해 외부 연동
    @Override
    public void exportAllToGoogle(String employeeNo) {
        log.info("--- CalendarServiceImpl exportAllToGoogle ---");
        log.info("사번 {}의 모든 개인 일정을 구글 캘린더로 내보냅니다.", employeeNo);

        List<CalendarDTO> personalEvents = calendarMapper.findAllWithDetails(employeeNo, CalendarScope.PERSONAL, null, null);

        if (personalEvents == null || personalEvents.isEmpty()) {
            log.warn("내보낼 개인 일정이 없습니다.");
            return;
        }

        int successCount = 0;
        for (CalendarDTO dto : personalEvents) {
            try {
                // 2. GoogleCalendarService 호출
                googleCalendarService.insertEvent(
                        employeeNo,
                        dto.getTitle(),
                        dto.getDescription(),
                        dto.getStartsAt(),
                        dto.getEndsAt()
                );
                successCount++;
            } catch (Exception e) {
                log.error("일정 내보내기 실패 - 제목: {}, 에러: {}", dto.getTitle(), e.getMessage());
            }
        }
        log.info("구글 내보내기 완료: 총 {}건 중 {}건 성공", personalEvents.size(), successCount);
    }

    // 일정 등록 및 반복 일정 생성
    // 범주별 필수 정보를 체크한 후 일정을 저장하며, 반복 주기에 따라 추가 데이터를 생성
    @Override
    public Long registerEvent(CalendarDTO dto) {
        log.info("--- CalendarServiceImpl registerEvent ---");
        log.info("--- [POST] 새 일정 등록 | 제목: {} ---", dto.getTitle());

        // 1. 스코프별 필수 정보 보완 로직 추가
        if (dto.getScope() == CalendarScope.PROJECT) {
            // 프로젝트 일정인데 프로젝트 ID가 없다면 에러 처리 혹은 기본값 할당
            if (dto.getProjectId() == null) {
                throw new IllegalArgumentException("프로젝트 일정을 등록하려면 프로젝트 선택이 필수입니다.");
            }
        }

        // 2. 반복 설정 추출
        RecurrenceType rule = (dto.getRecurrence() != null && !dto.getRecurrence().isEmpty())
                ? RecurrenceType.valueOf(dto.getRecurrence().get(0))
                : null;
        int count = (dto.getCount() != null && dto.getCount() > 0) ? dto.getCount() : 1;

        // 3. 첫 번째 일정(원본) 생성
        CalendarEvent firstEvent = CalendarEvent.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .startsAt(dto.getStartsAt())
                .endsAt(dto.getEndsAt())
                .isAllDay(dto.isAllDay())
                .scope(dto.getScope())
                .ownerNo(dto.getOwnerNo())
                .department(dto.getDepartment())
                .projectId(dto.getProjectId())
                .createdBy(dto.getCreatedBy())
                .colorType(parseCalendarColor(dto.getColorId()))
                .recurrenceRule(rule)
                .recurrenceCount(count)
                .build();

        CalendarEvent savedFirst = calendarEventRepository.save(firstEvent);

        // 4. 반복 설정에 따른 추가 일정 생성 (기존 로직 동일)
        if (rule != null && count > 1) {
            for (int i = 1; i < count; i++) {
                OffsetDateTime nextStart = calculateNextTime(savedFirst.getStartsAt(), rule, i);
                OffsetDateTime nextEnd = calculateNextTime(savedFirst.getEndsAt(), rule, i);

                CalendarEvent recurringEvent = savedFirst.toBuilder()
                        .id(null)
                        .parentId(savedFirst.getId())
                        .startsAt(nextStart)
                        .endsAt(nextEnd)
                        .build();

                calendarEventRepository.save(recurringEvent);
            }
        }
        return savedFirst.getId();
    }

    // 일정 내용 및 설정 변경
    // JPA 더티 체킹을 활용하여 일정의 정보와 색상, 반복 속성 등을 수정
    @Override
    public void modifyEvent(CalendarDTO dto) {
        log.info("--- CalendarServiceImpl modifyEvent ---");
        log.info("--- [PUT] 일정 수정 요청 | ID: {} ---", dto.getId());

        CalendarEvent event = calendarEventRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 일정을 찾을 수 없습니다. ID: " + dto.getId()));

        // 엔티티 메서드를 통한 필드 업데이트 (Dirty Checking)
        event.changeTitle(dto.getTitle());
        event.changeContent(dto.getDescription(), dto.getStartsAt(), dto.getEndsAt());
        event.changeScopeSettings(dto.getScope());

        // 반복 설정 및 색상 업데이트
        RecurrenceType rule = (dto.getRecurrence() != null && !dto.getRecurrence().isEmpty())
                ? RecurrenceType.valueOf(dto.getRecurrence().get(0))
                : null;

        event.changeExtraSettings(
                parseCalendarColor(dto.getColorId()),
                rule,
                dto.getCount()
        );
    }

    // 일정 삭제 처리
    // 부모 ID를 참조하는 반복 일정들을 먼저 제거한 후 원본 일정을 삭제
    @Override
    public void removeEvent(Long id) {
        log.info("--- CalendarServiceImpl removeEvent ---");
        log.info("--- [DELETE] 일정 삭제 요청 | ID: {} ---", id);

        // 외래 키 제약 조건 해결을 위해 자식(반복 일정) 먼저 삭제 후 본인 삭제
        calendarEventRepository.deleteByParentId(id);
        calendarEventRepository.deleteById(id);
    }

    // 다음 반복 시간 계산
    // 일/주/월 단위 반복 주기에 따라 기준 시간으로부터 다음 일정 시간을 산출
    private OffsetDateTime calculateNextTime(OffsetDateTime baseTime, RecurrenceType rule, int amount) {
        return switch (rule) {
            case DAILY -> baseTime.plusDays(amount);
            case WEEKLY -> baseTime.plusWeeks(amount);
            case MONTHLY -> baseTime.plusMonths(amount);
            default -> baseTime;
        };
    }

    // 클라이언트의 색상 식별자(숫자 ID 또는 Enum명)를 안전하게 CalendarColor Enum으로 변환
    // MyBatis 조회 결과(Enum 문자열)와 수정 팝업 요청 데이터의 형식이 달라 발생하는 색상 유실 버그를 방지하는 역할
    private CalendarColor parseCalendarColor(String colorId) {
        if (colorId == null || colorId.trim().isEmpty()) {
            return CalendarColor.BLUEBERRY; // 기본값 안전장치
        }
        try {
            // 1. 숫자 ID 형태("9", "1")로 들어온 경우
            if (colorId.matches("\\d+")) {
                return CalendarColor.fromId(colorId);
            }
            // 2. Enum 문자열 형태("BLUEBERRY", "BASIL")로 들어온 경우
            return CalendarColor.valueOf(colorId.toUpperCase());
        } catch (Exception e) {
            log.error("색상 변환 실패 - 입력값: {}, 기본 색상으로 대체합니다.", colorId);
            return CalendarColor.BLUEBERRY; // 예외 발생 시 기본값 반환
        }
    }
}
