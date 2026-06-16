package office_mate_2605.calender.mapper;

import office_mate_2605.calender.domain.CalendarScope;
import office_mate_2605.calender.dto.CalendarDTO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 일정 데이터 접근 관리 Mapper (작성자: 강수현)

 * <p> 사번과 일정 범위(개인, 팀, 프로젝트)를 기준으로 필터링된 일정 목록 및 상세 정보를 조회하며,
 * 사원의 부서 정보 확인 등 일정 서비스에 필요한 데이터베이스 연동 기능을 담당합니다.</p>
 */

@Mapper
public interface CalendarMapper {
     // 범위별 일정 목록 조회
     // 사번과 일정 범위(PERSONAL, TEAM, PROJECT)를 기준으로 필터링된 일정 리스트를 상세 정보와 함께 조회
     List<CalendarDTO> findAllWithDetails(
             @Param("employeeNo") String employeeNo,
             @Param("scope") CalendarScope scope,
             @Param("start") String start,
             @Param("end") String end
     );

    // 일정 상세 정보 조회
    // 일정 ID를 기반으로 작성자 이름, 부서명, 프로젝트명 등 조인된 상세 데이터를 포함하여 조회
    CalendarDTO findDetailById(@Param("id") Long id);
}
