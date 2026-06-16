package office_mate_2605.common.employee.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import office_mate_2605.common.employee.domain.EmploymentRequestType;

/**
 * 직원 재직 상태 변경 신청 생성 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>마이페이지에서 로그인 사용자가 휴직 또는 퇴사 신청을 생성할 때
 * 신청 유형과 신청 사유를 JSON 요청으로 전달한다.
 * 실제 신청 가능 여부와 중복 신청 검증은 Service에서 처리한다.</p>
 */
public record EmployeeStatusRequestCreateRequestDTO(

        // 신청 유형
        // LEAVE는 휴직, RESIGN은 퇴사, RETURN_FROM_LEAVE는 복직 신청
        @NotNull(message = "신청 유형은 필수입니다.")
        EmploymentRequestType requestType,

        // 신청 사유
        // DB는 text 타입이지만 화면/API 입력을 무제한으로 두면 관리 화면 가독성이 떨어지므로 DTO에서 길이를 제한
        @Size(max = 2000, message = "신청 사유는 2000자 이하여야 합니다.")
        String reason
) {
}
