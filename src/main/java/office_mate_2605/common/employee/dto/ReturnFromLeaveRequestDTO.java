package office_mate_2605.common.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인 전 복직 신청 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>휴직 상태의 직원은 일반 로그인이 차단되어 accessToken을 받을 수 없으므로,
 * 사번과 비밀번호, 복직 사유를 함께 전달해 로그인 전 복직 신청을 생성한다.
 * 본인 확인과 신청 가능 여부 검증은 Service에서 처리한다.</p>
 */
public record ReturnFromLeaveRequestDTO(

        // 복직을 신청하는 직원 사번, 서버에서 대문자로 정규화
        @NotBlank(message = "사번은 필수입니다.")
        @Size(max = 30, message = "사번은 30자 이하여야 합니다.")
        String employeeNo,

        // 현재 비밀번호 (휴직 상태여도 본인 확인을 위해)
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,

        // 복직 신청 사유
        @Size(max = 2000, message = "신청 사유는 2000자 이하여야 합니다.")
        String reason
) {
}
