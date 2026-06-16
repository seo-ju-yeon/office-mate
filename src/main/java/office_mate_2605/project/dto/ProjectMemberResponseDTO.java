package office_mate_2605.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 프로젝트 멤버 추가 요청 DTO (작성자: 강수현)

 * <p> 특정 프로젝트에 새로운 멤버를 할당하기 위한 프로젝트 ID와 사원 번호를 전달합니다.
 * 멤버 초대 및 참여 로직 수행 시 필요한 최소 정보를 담고 있습니다.</p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponseDTO {
    private Long projectId; // 소속 프로젝트 ID
    private String employeeNo; // 참여 중인 사원 사번
    private String employeeName; // 참여 중인 사원 이름
    private String departmentName; // 참여 중인 사원 부서 이름
    private LocalDateTime joinedAt; // 프로젝트 참여 일시
}
