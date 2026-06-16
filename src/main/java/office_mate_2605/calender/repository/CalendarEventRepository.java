package office_mate_2605.calender.repository;

import office_mate_2605.calender.domain.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 일정 정보 관리 Repository (작성자: 강수현)

 * <p> 캘린더 일정 엔티티에 대한 데이터베이스 접근을 담당하며, 사원별 일정 조회 및 기간별 필터링 기능을 제공합니다.
 * 반복 일정의 일괄 삭제와 같은 일정 데이터의 영속성 관리 및 데이터 조작 기능을 수행합니다.</p>
 */

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    // 직원별 일정 목록 조회
    // 사원 번호(ownerNo)를 기준으로 해당 직원이 소유한 모든 일정 리스트 반환
    List<CalendarEvent> findByOwnerNo(String ownerNo);

    // 간 기반 일정 조회
    // 특정 시작 일시와 종료 일시 사이에 포함되는 모든 일정 리스트 조회
    List<CalendarEvent> findByStartsAtBetween(OffsetDateTime start, OffsetDateTime end);

    // 반복 일정(하위 일정) 일괄 삭제
    // 부모 일정 ID(parentId)를 참조하고 있는 모든 반복 일정 데이터 삭제
    @Modifying
    @Query("delete from CalendarEvent c where c.parentId = :parentId")
    void deleteByParentId(@Param("parentId") Long parentId);
}
