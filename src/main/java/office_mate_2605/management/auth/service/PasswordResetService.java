package office_mate_2605.management.auth.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.mail.MailSenderService;
import office_mate_2605.management.auth.dto.AuthResultResponseDTO;
import office_mate_2605.management.auth.dto.PasswordResetConfirmRequestDTO;
import office_mate_2605.management.auth.dto.PasswordResetIssueRequestDTO;
import office_mate_2605.management.auth.repository.RefreshTokenRepository;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.time.Duration;

/**
 * 로그인 전 비밀번호 찾기와 재설정 흐름을 처리하는 Service. (작성자: 서주연)
 *
 * <p>사번과 이메일이 일치하는 직원에게 임시 비밀번호를 발급하고,
 * 임시 비밀번호 원문은 메일로만 전송하며 Redis에는 BCrypt 해시를 10분간 저장한다.
 * 재설정 확정 시 Redis 해시 검증 후 DB 비밀번호를 변경하고 기존 refresh token을 폐기한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    // 비밀번호 재설정 임시 비밀번호 유효 시간
    private static final Duration RESET_TTL = Duration.ofMinutes(10);

    // Redis에 저장할 비밀번호 재설정 key prefix
    private static final String RESET_KEY_PREFIX = "password-reset:";

    // 임시 비밀번호 생성에 사용할 문자 목록
    // 혼동하기 쉬운 일부 문자는 제외
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$";

    // 사번과 이메일이 일치하는 직원 조회 및 비밀번호 변경을 위해 사용
    private final EmployeeRepository employeeRepository;

    // 비밀번호 재설정 완료 후 기존 refresh token을 폐기하기 위해 사용
    private final RefreshTokenRepository refreshTokenRepository;

    // 임시 비밀번호 BCrypt 해시를 10분 동안 Redis에 저장하고 검증하기 위해 사용
    private final RedisTemplate<String, Object> redisTemplate;

    // 임시 비밀번호와 재설정 링크를 사용자 이메일로 발송하기 위해 사용
    private final MailSenderService mailSenderService;

    // 임시 비밀번호 해시 저장, 검증, 새 비밀번호 암호화를 위해 사용
    private final PasswordEncoder passwordEncoder;

    // 비밀번호 재설정 화면 URL
    // 설정값이 없으면 로컬 기본 URL 사용
    @Value("${myapp.password-reset-url:http://localhost:8080/password-reset}")
    private String passwordResetUrl;

    // 비밀번호 재설정 임시 비밀번호를 발급하고 메일로 안내하는 메서드
    // 임시 비밀번호 원문은 메일로만 보내고 Redis에는 BCrypt 해시만 저장
    public AuthResultResponseDTO issueTempPassword(PasswordResetIssueRequestDTO request) {
        log.info("--- PasswordResetService.issueTempPassword() 진입 employeeNo={} ---", request.employeeNo());

        // 사번과 이메일이 모두 일치하는 직원만 비밀번호 재설정 대상
        Employee employee = employeeRepository
                .findByEmployeeNoAndEmailAndDeletedAtIsNull(request.employeeNo().trim(), request.email().trim())
                .orElseThrow(() -> new IllegalArgumentException("입력한 사번과 이메일을 확인해주세요."));

        // 사용자에게 보낼 임시 비밀번호 원문 생성
        String tempPassword = generateTempPassword();

        // Redis에 저장할 직원별 비밀번호 재설정 key 생성
        String redisKey = createRedisKey(employee.getEmployeeNo());

        // Redis에는 임시 비밀번호 원문을 저장하지 않음
        // BCrypt 해시를 저장해 Redis 데이터가 노출되어도 바로 로그인/재설정에 사용할 수 없게 처리
        redisTemplate.opsForValue().set(redisKey, passwordEncoder.encode(tempPassword), RESET_TTL);

        try {
            // 메일에는 임시 비밀번호 원문과 재설정 화면 URL을 전달
            mailSenderService.sendPasswordResetMail(employee.getEmail(), tempPassword, passwordResetUrl);
        } catch (MessagingException | UnsupportedEncodingException e) {
            // 메일 발송 실패 시 Redis에 남긴 임시 비밀번호도 제거해 잘못된 상태 방지
            redisTemplate.delete(redisKey);
//            throw new IllegalStateException("password_reset_mail_failed", e);
            throw new IllegalStateException("임시 비밀번호 메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요.", e);
        }

//        return new AuthResultResponseDTO("password_reset_mail_sent");
        return new AuthResultResponseDTO("임시 비밀번호를 이메일로 발송했습니다.");
    }

    // 임시 비밀번호를 검증한 뒤 새 비밀번호로 재설정하는 메서드
    // 성공 시 Redis 임시 비밀번호와 기존 refresh token을 모두 폐기
    @Transactional
    public AuthResultResponseDTO confirmResetPassword(PasswordResetConfirmRequestDTO request) {
        log.info("--- PasswordResetService.confirmResetPassword() 진입 employeeNo={} ---", request.employeeNo());

        // 새 비밀번호는 최소 8자 이상이어야 함
        if (request.newPassword() == null || request.newPassword().length() < 8) {
//            throw new IllegalArgumentException("new_password_too_short");
            throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
        }

        // 요청 사번으로 Redis key 생성 후 저장된 임시 비밀번호 해시 조회
        String employeeNo = request.employeeNo().trim();
        String redisKey = createRedisKey(employeeNo);
        Object storedHash = redisTemplate.opsForValue().get(redisKey);

        // Redis에 값이 없으면 만료되었거나 발급 이력이 없는 상태
        if (storedHash == null) {
//            throw new IllegalArgumentException("temp_password_expired_or_not_found");
            throw new IllegalArgumentException("임시 비밀번호가 만료되었거나 발급 내역이 없습니다. 다시 발급받아주세요.");
        }

        // 사용자가 입력한 임시 비밀번호 원문과 Redis에 저장된 BCrypt 해시 비교
        if (!passwordEncoder.matches(request.tempPassword(), storedHash.toString())) {
//            throw new IllegalArgumentException("temp_password_mismatch");
            throw new IllegalArgumentException("임시 비밀번호가 일치하지 않습니다.");
        }

        // 비밀번호를 변경할 직원 조회
        Employee employee = employeeRepository.findByEmployeeNoAndDeletedAtIsNull(employeeNo)
//                .orElseThrow(() -> new IllegalArgumentException("employee_not_found"));
                .orElseThrow(() -> new IllegalArgumentException("직원 정보를 찾을 수 없습니다."));

        // 새 비밀번호로 DB를 변경하고 임시 비밀번호 필요 상태 해제
        employee.changePassword(passwordEncoder.encode(request.newPassword()));

        // 비밀번호가 바뀌었으므로 기존 refresh token은 모두 폐기
        refreshTokenRepository.findByEmployeeEmployeeNoAndRevokedAtIsNull(employeeNo)
                .forEach(refreshToken -> refreshToken.revoke());

        // 재사용을 막기 위해 성공한 임시 비밀번호는 Redis에서 즉시 삭제
        redisTemplate.delete(redisKey);
//        return new AuthResultResponseDTO("password_reset_completed");
        return new AuthResultResponseDTO("비밀번호가 재설정되었습니다. 다시 로그인해주세요.");
    }

    // 직원별 비밀번호 재설정 Redis key를 생성하는 메서드
    // key 생성 규칙을 한 곳에서 관리해 오타로 다른 key를 조회하는 실수를 줄임
    private String createRedisKey(String employeeNo) {
        return RESET_KEY_PREFIX + employeeNo;
    }

    // 예측하기 어려운 임시 비밀번호를 생성하는 메서드
    // SecureRandom으로 지정된 문자 목록에서 10자 추출
    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        // TEMP_PASSWORD_CHARS에서 무작위 문자를 10번 선택해 임시 비밀번호 구성
        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(TEMP_PASSWORD_CHARS.length());
            password.append(TEMP_PASSWORD_CHARS.charAt(index));
        }

        return password.toString();
    }
}
