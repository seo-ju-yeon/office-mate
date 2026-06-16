package office_mate_2605.management.auth.dto;

/**
 * 인증 관련 단순 처리 결과를 반환하는 DTO. (작성자: 서주연)
 *
 * <p>logout, password_changed, password_reset_completed처럼
 * 별도 데이터 없이 처리 결과 문자열만 내려줄 때 사용한다.</p>
 */
public record AuthResultResponseDTO(
        // 처리 결과 문자열. 예: "logout", "password_changed"
        String result
) {
}
