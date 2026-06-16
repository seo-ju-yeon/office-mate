package office_mate_2605.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 프로젝트 업무 생성 및 수정 요청 DTO (작성자: 강수현)

 * <p> 업무 제목, 설명, 담당자, 우선순위, 마감일 등 업무 배정에 필요한 상세 데이터를 전달합니다.
 * 신규 업무 등록 및 기존 업무의 속성 정보를 변경하는 로직에 사용됩니다.</p>
 */


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTaskRequestDTO {
    private Long projectId;      // 어떤 프로젝트의 업무인지
    private String title;        // 업무 제목
    private String description;  // 업무 상세 설명
    private String assigneeNo;   // 담당자 사번 (employee 테이블 JOIN용)

    private String status;       // TODO, IN_PROGRESS, DONE
    private String priority;     // HIGH, NORMAL, LOW

    private Integer progressRate; // 진행률
    private LocalDate dueOn;      // 마감일
    private Boolean isCritical;   // 중요 여부

    private String assignedBy;    // 업무를 배정하는 사람 사번
}
