package office_mate_2605.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 간략 정보 응답 DTO (작성자: 강수현)

 * <p> 프로젝트의 식별 ID, 이름, 현재 상태값만을 포함하는 경량 데이터 객체입니다.
 * 캘린더 필터나 선택 박스 등 프로젝트 목록을 간소하게 표시할 때 사용됩니다.</p>
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponseDTO {
    private Long id; // 프로젝트 ID
    private String name; // 프로젝트 이름
    private String status; // 프로젝트 상태
}
