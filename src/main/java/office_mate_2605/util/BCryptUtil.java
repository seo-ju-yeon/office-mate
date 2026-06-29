package office_mate_2605.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.bcrypt.BCrypt;

/**
 * BCrypt 기반 비밀번호 해싱과 검증을 제공하는 공통 Util. (작성자: 서주연)
 *
 * <p>평문 비밀번호를 BCrypt 해시로 변환하고, 사용자가 입력한 평문 비밀번호가
 * 저장된 해시와 일치하는지 검증한다. BCrypt 해시의 work factor를 확인하는 보조 기능도 제공한다.</p>
 *
 * <p>현재 프로젝트에서는 Spring Security PasswordEncoder를 주로 사용하지만,
 * BCrypt 동작 확인이나 단독 해시 테스트가 필요할 때 사용할 수 있는 유틸리티이다.</p>
 */
@Log4j2
public class BCryptUtil {

    // BCrypt 해싱 강도
    // 값이 높을수록 계산 비용이 커져 무차별 대입 공격 방어에 유리
    private static final int WORK_FACTOR = 12;

    // 평문 비밀번호를 BCrypt 해시로 변환하는 메서드
    // 저장소에는 평문이 아니라 반환된 해시값만 저장해야 함
    public static String hashPassword(String plainPassword) {
        // null 또는 빈 문자열은 비밀번호로 사용할 수 없으므로 차단
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 null이거나 빈 문자열일 수 없습니다.");
        }

        try {
            // WORK_FACTOR 기준으로 salt를 생성한 뒤 BCrypt 해시 생성
            String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt(WORK_FACTOR));
            log.debug("비밀번호 해싱 완료 (원본 길이: {}자)", plainPassword.length());
            return hashedPassword;

        } catch (Exception e) {
            // 해싱 실패 시 원인을 로그로 남기고 상위 흐름에 예외 전달
            log.error("비밀번호 해싱 중 오류 발생", e);
            throw new RuntimeException("비밀번호 암호화에 실패했습니다.", e);
        }
    }

    // 입력된 평문 비밀번호와 저장된 BCrypt 해시를 비교하는 메서드
    // 일치하면 true, 일치하지 않거나 검증할 수 없으면 false 반환
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        // 입력 비밀번호가 비어 있으면 검증 실패 처리
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            log.warn("비밀번호 검증 실패: 입력된 비밀번호가 null 또는 빈 문자열");
            return false;
        }

        // 저장된 해시가 비어 있으면 비교 대상이 없으므로 검증 실패 처리
        if (hashedPassword == null || hashedPassword.trim().isEmpty()) {
            log.warn("비밀번호 검증 실패: 저장된 해시가 null 또는 빈 문자열");
            return false;
        }

        try {
            // BCrypt.checkpw가 평문과 해시를 비교해 일치 여부 반환
            boolean matches = BCrypt.checkpw(plainPassword, hashedPassword);
            log.debug("비밀번호 검증 결과: {}", matches ? "일치" : "불일치");
            return matches;

        } catch (Exception e) {
            // 해시 형식 오류 등 검증 중 예외가 나면 인증 실패로 처리
            log.error("비밀번호 검증 중 오류 발생", e);
            return false;
        }
    }

    // BCrypt 해시의 work factor를 추출하는 메서드
    // 추출할 수 없으면 -1 반환
    public static int getWorkFactor(String hashedPassword) {
        // BCrypt 해시 최소 형식보다 짧으면 유효하지 않은 값으로 판단
        if (hashedPassword == null || hashedPassword.length() < 7) {
            return -1;
        }

        try {
            // BCrypt 해시 형식은 $2a$12$... 이며 세 번째 값이 work factor
            String[] parts = hashedPassword.split("\\$");
            if (parts.length >= 3) {
                return Integer.parseInt(parts[2]);
            }
        } catch (Exception e) {
            // 숫자 변환 실패 등 해시 형식이 맞지 않으면 -1 반환
            log.warn("Work factor 추출 실패", e);
        }
        return -1;
    }
}
