package office_mate_2605.project.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 프로젝트 멤버 복합키 식별자 클래스 (작성자: 강수현)

 * <p> 프로젝트 ID와 사원 번호를 조합하여 ProjectMember 엔티티의 유일성을 보장합니다.
 * JPA의 IdClass 설정을 통해 복합키 조회를 지원합니다. </p>
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberId implements Serializable {
    private Long projectId;
    private String employeeNo;
}
