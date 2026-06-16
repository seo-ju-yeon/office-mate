package office_mate_2605.dashboard.mapper;

import office_mate_2605.project.dto.TaskResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

/**
 * 대시보드 데이터 접근 관리 Mapper(작성자: 강수현)

 * <p> 사번을 기반으로 사용자의 업무 통계(진행 중, 마감 임박, 지연 건수) 및 할당된 업무 리스트를 조회합니다.
 * MyBatis를 통해 대시보드 화면 구성에 필요한 복합적인 업무 현황 데이터를 데이터베이스에서 추출하는 역할을 수행합니다.</p>
 */

@Mapper
public interface DashboardMapper {
    // 내 업무 통계 조회
    // 사원 번호를 기준으로 진행 중인 업무, 오늘 마감 업무, 지연된 업무의 개수 집계
    Map<String, Long> getTaskStatistics(@Param("empNo") String empNo);

    // 내 업무 리스트 조회
    // 본인에게 할당된 업무 중 완료되지 않은 리스트를 프로젝트 정보와 함께 상세 조회
    List<TaskResponseDTO> getMyTaskList(@Param("empNo") String empNo);
}
