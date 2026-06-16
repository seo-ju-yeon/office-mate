package office_mate_2605.common.employee.dto;

import jakarta.validation.constraints.Size;

/**
 * 직원 재직 상태 변경 신청 승인/반려 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>SUPER가 휴직, 퇴사, 복직 신청을 승인하거나 반려할 때
 * 처리 코멘트를 전달한다. 승인/반려 여부는 URL로 분리하고,
 * 이 DTO는 결재자가 남기는 설명만 담당한다.</p>
 */
public record EmployeeStatusRequestDecisionRequestDTO(

        // 승인/반려 사유 또는 안내 문구, 선택 입력
        @Size(max = 2000, message = "처리 코멘트는 2000자 이하여야 합니다.")
        String decisionComment
) {
}
