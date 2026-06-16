package office_mate_2605.project.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import office_mate_2605.project.domain.DepartmentCode;

import java.time.LocalDate;

/**
 * 프로젝트 생성 및 수정 요청 DTO (작성자: 강수현)

 * <p> 프로젝트명, 기간, 담당자, 주관 부서 등 프로젝트 설정을 위한 입력 데이터를 전달합니다.
 * 새로운 프로젝트를 기획하거나 기존 정보를 업데이트할 때 사용됩니다.</p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequestDTO {
    private String name; // 프로젝트 이름
    private String description; // 프로젝트 상세 설정

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate startsOn; // 프로젝트 시작일

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "Asia/Seoul")
    private LocalDate endsOn; // 프로젝트 종료일
    private String managerNo; // 프로젝트 담당자 사번
    private DepartmentCode ownerDepartment; // 주관 부서 코드
}
