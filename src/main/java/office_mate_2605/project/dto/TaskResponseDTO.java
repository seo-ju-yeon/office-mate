package office_mate_2605.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import office_mate_2605.project.domain.TaskStatus;

import java.time.LocalDate;

/**
 * 프로젝트 업무 상세 응답 DTO (작성자: 강수현)

 * <p> 업무의 진행 상태, 진척률뿐만 아니라 담당자 및 프로젝트 정보 등 조인된 상세 데이터를 전달합니다.
 * 업무 리스트 조회, 상세 팝업, 내 업무 현황 대시보드 등을 구성하는 데 사용됩니다.</p>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDTO {
    private Long id; // 업무 ID
    private Long projectId; // 소속 프로젝트 ID
    private String title; // 업무 제목
    private String description; // 업무 상세 내용
    private TaskStatus status; // 업무 상태
    private int progressRate; // 개별 진행률
    private String priority; // 우선 순위
    private boolean isCritical; // 핵심 업무 여부
    private LocalDate dueOn; // 마감일

    // JOIN을 통해 가져올 추가 정보
    private String assigneeName; // 담당자 이름
    private String departmentName; // 담당자 부서 이름
    private String projectName; // 소속 프로젝트 이름
}
