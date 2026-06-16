package office_mate_2605.calender.repository;

import office_mate_2605.calender.domain.GoogleCalendarLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 구글 캘린더 연동 정보 관리 Repository (작성자: 강수현)

 * <p> 사원별 구글 캘린더 계정 연동 상태와 인증 토큰 정보를 데이터베이스에서 관리합니다.
 * 사번을 식별자로 하여 연동 데이터를 조회, 등록 및 수정하는 역할을 수행합니다.</p>
 */

@Repository
public interface GoogleCalendarLinkRepository extends JpaRepository<GoogleCalendarLink, String> {
    /*
     * 기본 JpaRepository 메서드 활용
     - findById(String empNo): 사원 번호를 PK로 하여 구글 캘린더 연동 정보(토큰 등) 조회
     - save(GoogleCalendarLink entity): 구글 캘린더 연동 정보를 등록하거나 수정
     */
}
