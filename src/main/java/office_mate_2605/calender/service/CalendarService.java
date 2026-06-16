package office_mate_2605.calender.service;

import office_mate_2605.calender.domain.CalendarScope;
import office_mate_2605.calender.dto.CalendarDTO;

import java.util.List;

/**
 * 일정 정보 관리 Service (작성자: 강수현)

 * <p> 개인, 팀, 프로젝트 범위의 일정 CRUD(조회, 등록, 수정, 삭제) 및 반복 일정 생성 로직을 처리합니다.
 * 사용자의 일정을 구글 캘린더와 연동하여 외부 서비스로 동기화하는 비즈니스 로직을 담당합니다.</p>
 */

public interface CalendarService {

    // 범위별 일정 목록 조회
    // 개인(PERSONAL), 부서(TEAM), 프로젝트(PROJECT) 등 지정된 범위에 해당하는 일정들을 조회
    List<CalendarDTO> getEventsByScope(String employeeNo, CalendarScope scope, String start, String end);

    // 일정 상세 정보 조회
    // 특정 일정의 고유 ID를 통해 제목, 내용, 일시 등 상세 데이터를 조회
    CalendarDTO getEventDetail(Long id);

    // 신규 일정 등록
    // 새로운 일정을 생성하고, 반복 설정이 있을 경우 하위 일정들을 함께 등록
    Long registerEvent(CalendarDTO dto);

    // 일정 정보 수정
    // 기존 일정의 제목, 시간, 반복 설정 등을 업데이트
    void modifyEvent(CalendarDTO dto);

    // 일정 삭제
    // 특정 일정과 해당 일정의 모든 반복(자식) 일정들을 삭제
    void removeEvent(Long id);

    // 구글 캘린더 연동 내보내기
    // 사용자의 개인 일정을 구글 캘린더 서버로 전송하여 동기화
    void exportAllToGoogle(String employeeNo);

}
