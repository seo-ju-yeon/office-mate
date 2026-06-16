package office_mate_2605.project.dto;

import lombok.*;

import java.time.LocalDate;

/**
 * 프로젝트 상세 정보 응답 DTO (작성자: 강수현)

 * <p> 프로젝트의 기본 설정, 진행률, 기간 정보와 더불어 담당자 및 부서 정보를 포함합니다.
 * 프로젝트 대시보드나 상세 조회 페이지에서 종합적인 현황을 출력하는 데 사용됩니다.</p>
 */

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailResponseDTO {
    private Long id; // 프로젝트 ID
    private String name; // 프로젝트 이름
    private String status; // 프로젝트 상태
    private String description; // 프로젝트 상세 설명
    private Integer progressRate; // 프로젝트 전체 진행률
    private LocalDate startsOn; // 프로젝트 시작일
    private LocalDate endsOn; // 프로젝트 종료일

    // 조인으로 가져온 데이터
    private String managerName; // 프로젝트 담당자 이름
    private String departmentName; // 프로젝트 주관 부서 이름
}
