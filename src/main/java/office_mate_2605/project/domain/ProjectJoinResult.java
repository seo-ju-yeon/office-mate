package office_mate_2605.project.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 프로젝트 조인 결과 조회용 VO (작성자: 강수현)

 * <p> MyBatis를 이용한 다중 테이블 JOIN 결과(프로젝트, 담당자명, 부서명 등)를 담기 위한 객체입니다.
 * DB 엔티티 구조와 별개로 화면 출력에 최적화된 복합 데이터를 전달하는 역할을 합니다. </p>
 */

@Getter
@Setter
public class ProjectJoinResult {
    /*
    MyBatis에서 INNER JOIN 쿼리를 실행한 결과를 담기 위한 객체
     */
    private Long id;
    private String name;
    private String status;
    private String description;
    private Integer progressRate;
    private LocalDate startsOn;
    private LocalDate endsOn;

    // INNER JOIN을 통해 가져온 추가 데이터
    private String managerName;      // employee 테이블에서 가져온 매니저 이름
    private String departmentName;   // departments 테이블에서 가져온 부서명
}
