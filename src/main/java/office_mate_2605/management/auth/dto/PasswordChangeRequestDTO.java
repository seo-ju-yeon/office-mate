package office_mate_2605.management.auth.dto;

/**
 * 로그인 사용자의 비밀번호 변경 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>현재 비밀번호와 새 비밀번호를 JSON body로 받아
 * 본인 확인 후 DB 비밀번호를 BCrypt 해시로 변경하는 흐름에 사용한다.</p>
 */
public record PasswordChangeRequestDTO(
        // 현재 비밀번호 (본인 확인을 위해 사용)
        String currentPassword,

        // 새 비밀번호 (Service에서 길이 검사 후 BCrypt로 암호화해 저장)
        String newPassword
) {
}
