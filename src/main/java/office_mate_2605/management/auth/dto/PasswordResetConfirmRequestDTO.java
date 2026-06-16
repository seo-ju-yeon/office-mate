package office_mate_2605.management.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 재설정 확정 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>메일로 받은 임시 비밀번호와 새 비밀번호를 함께 전달한다.
 * Redis에 저장된 임시 비밀번호 해시와 일치할 때만 DB 비밀번호를 새 비밀번호 해시로 변경한다.</p>
 */
public record PasswordResetConfirmRequestDTO(
        // 비밀번호를 재설정할 직원 사번
        @NotBlank(message = "사번은 필수입니다.")
        String employeeNo,

        // 메일로 발급받은 임시 비밀번호
        @NotBlank(message = "임시 비밀번호는 필수입니다.")
        String tempPassword,

        // 최종적으로 사용할 새 비밀번호
        @NotBlank(message = "새 비밀번호는 필수입니다.")
        String newPassword
) {
}
