package office_mate_2605.management.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.management.auth.dto.AuthMeResponseDTO;
import office_mate_2605.management.auth.dto.AuthResultResponseDTO;
import office_mate_2605.management.auth.dto.PasswordChangeRequestDTO;
import office_mate_2605.management.auth.repository.RefreshTokenRepository;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.repository.EmployeeRepository;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 이후 인증 관련 비즈니스 규칙을 처리하는 Service. (작성자: 서주연)
 *
 * <p>현재 로그인 사용자 정보 조회, 로그아웃 시 refresh token 폐기,
 * 임시 비밀번호 또는 일반 비밀번호 변경을 담당한다.
 * 비밀번호 변경 시 기존 refresh token을 모두 revoke해 재발급을 차단한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class AuthService {
    // AuthService는 로그인 이후의 인증 관련 비즈니스 로직을 담당
    // DB 조회, 비밀번호 변경, refresh token 처리 (Controller는 HTTP 요청/응답만 처리)

    // 비밀번호 변경 대상 직원을 DB에서 조회하기 위해 사용
    private final EmployeeRepository employeeRepository;

    // 로그아웃 또는 비밀번호 변경 시 활성 refresh token을 폐기하기 위해 사용
    private final RefreshTokenRepository refreshTokenRepository;

    // 현재 비밀번호 검증과 새 비밀번호 BCrypt 암호화를 위해 사용
    private final PasswordEncoder passwordEncoder;

    // 현재 로그인 사용자 정보를 응답 DTO로 변환하는 메서드
    // TokenCheckFilter가 Authentication에 담아둔 EmployeePrincipal을 사용
    @Transactional(readOnly = true)
    public AuthMeResponseDTO getMe(EmployeePrincipal principal) {
        // 현재 로그인 사용자 정보는 TokenCheckFilter가 검증 후 EmployeePrincipal에 저장
        // DB를 다시 조회하지 않고 응답 DTO로 변환
        return AuthMeResponseDTO.from(principal);
    }

    // 현재 로그인 사용자의 refresh token을 폐기해 로그아웃 처리하는 메서드
    // access token은 stateless라 서버에서 직접 삭제하지 않음
    @Transactional
    public AuthResultResponseDTO logout(EmployeePrincipal principal) {
        // JWT access token은 서버에 저장하지 않으므로 직접 삭제 불가
        // 대신 refresh token을 폐기해 재발급 차단
        revokeActiveRefreshTokens(principal.getEmployeeNo());
        return new AuthResultResponseDTO("logout");
    }

    // 로그인 사용자의 비밀번호를 새 비밀번호로 변경하는 메서드
    // 변경 후 기존 refresh token을 모두 폐기해 재인증을 유도
    @Transactional
    public AuthResultResponseDTO changePassword(EmployeePrincipal principal, PasswordChangeRequestDTO request) {
        // 포트폴리오 단계에서는 최소 길이만 검사
        // 추후 운영 수준으로 가면 영문/숫자/특수문자 조합 검사도 추가 가능
        if (request.newPassword() == null || request.newPassword().length() < 4) {
//            throw new IllegalArgumentException("new_password_too_short");
            throw new IllegalArgumentException("새 비밀번호는 8자 이상이어야 합니다.");
        }

        // 비밀번호는 최신 DB 값을 기준으로 변경해야 하므로 employee 테이블에서 다시 조회
        Employee employee = employeeRepository.findByEmployeeNoAndDeletedAtIsNull(principal.getEmployeeNo())
//                .orElseThrow(() -> new IllegalArgumentException("employee_not_found"));
                .orElseThrow(() -> new IllegalArgumentException("직원 정보를 찾을 수 없습니다."));

        // 현재 비밀번호가 넘어온 경우 BCrypt matches로 원문과 해시를 비교
        // DB에는 평문 비밀번호가 없으므로 equals 비교 금지
        if (request.currentPassword() != null && !passwordEncoder.matches(request.currentPassword(), employee.getPassword())) {
//            throw new IllegalArgumentException("current_password_mismatch");
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        // Entity 도메인 메서드에서 password 변경과 temp_password_required=false 처리를 함께 수행
        employee.changePassword(passwordEncoder.encode(request.newPassword()));

        // 비밀번호 변경 후 기존 refresh token을 모두 폐기해 보안 강화
        revokeActiveRefreshTokens(employee.getEmployeeNo());
        return new AuthResultResponseDTO("password_changed");
    }

    // 직원의 활성 refresh token을 모두 폐기하는 메서드
    // revokedAt을 채워 이후 access token 재발급을 차단
    private void revokeActiveRefreshTokens(String employeeNo) {
        // 활성 refresh token을 조회한 뒤 각 Entity의 revoke() 메서드로 revokedAt 채움
        // @Transactional 안에서 Entity 값이 바뀌면 JPA가 변경 감지로 UPDATE 실행
        refreshTokenRepository.findByEmployeeEmployeeNoAndRevokedAtIsNull(employeeNo)
                .forEach(refreshToken -> refreshToken.revoke());
    }
}
