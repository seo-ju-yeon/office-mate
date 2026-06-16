package office_mate_2605.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import office_mate_2605.project.domain.DepartmentCode;

/**
 * 직원 정보 응답 DTO (작성자: 강수현)

 * <p> 부서 코드 및 부서명을 포함한 직원의 기본 인적 사항을 전달합니다.
 * 프로젝트 멤버 초대 시 직원 검색 결과나 드롭다운 목록을 구성하는 데 사용됩니다.</p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponseDTO {
    private String employeeNo;      // 사원 번호
    private String employeeName;    // 직원 이름
    private DepartmentCode departmentCode;  // 부서 코드
    private String departmentName;  // 부서 이름
}
