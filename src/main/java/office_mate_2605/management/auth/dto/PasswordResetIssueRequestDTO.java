package office_mate_2605.management.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 찾기 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>로그인 전 사용자가 입력한 사번과 이메일을 전달한다.
 * Service는 두 값이 employee 테이블의 정보와 일치하는지 확인한 뒤
 * 임시 비밀번호를 발급하고 메일로 발송한다.</p>
 */
public record PasswordResetIssueRequestDTO(
        // 회사에서 발급한 사번. 로그인 ID로 사용하는 값이다.
        @NotBlank(message = "사번은 필수입니다.")
        String employeeNo,

        // employee 테이블에 등록된 이메일과 일치해야 임시 비밀번호를 발급한다.
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email
) {
}
