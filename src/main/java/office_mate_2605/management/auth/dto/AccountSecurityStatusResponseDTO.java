package office_mate_2605.management.auth.dto;

import office_mate_2605.management.auth.domain.AccountSecurityStatus;

import java.time.LocalDateTime;

/**
 * 계정 보안 상태 단건 결과를 반환하는 DTO. (작성자: 서주연)
 *
 * <p>특정 직원의 로그인 실패 횟수, 잠금 여부, 마지막 실패 시각,
 * 잠금/해제 시각, 해제 관리자를 응답한다.
 * 직원 기본 정보와 로그인 보안 상태는 책임이 달라 EmployeeResponseDTO와 분리한다.</p>
 */
public record AccountSecurityStatusResponseDTO(

        String employeeNo,  // 보안 상태를 관리하는 직원 사번
        int loginFailCount,  // 연속 로그인 실패 횟수
        boolean accountLocked,  // 계정 잠금 여부
        LocalDateTime lastFailedAt,  // 마지막 로그인 실패 시각
        LocalDateTime lockedAt,  // 계정 잠금 시각
        LocalDateTime unlockedAt,  // 관리자 잠금 해제 시각
        String unlockedBy  // 잠금을 해제한 관리자 사번
) {
    // 계정 보안 상태 Entity를 단건 조회 응답 DTO로 변환하는 메서드
    // 잠금 해제 관리자가 없으면 unlockedBy는 null로 응답
    public static AccountSecurityStatusResponseDTO from(AccountSecurityStatus securityStatus) {
        return new AccountSecurityStatusResponseDTO(
                securityStatus.getEmployeeNo(),
                securityStatus.getLoginFailCount(),
                securityStatus.isAccountLocked(),
                securityStatus.getLastFailedAt(),
                securityStatus.getLockedAt(),
                securityStatus.getUnlockedAt(),
                securityStatus.getUnlockedBy() == null ? null : securityStatus.getUnlockedBy().getEmployeeNo()
        );
    }
}
