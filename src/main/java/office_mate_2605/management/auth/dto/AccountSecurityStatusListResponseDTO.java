package office_mate_2605.management.auth.dto;

import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.management.auth.domain.AccountSecurityStatus;

import java.time.LocalDateTime;

/**
 * 계정 보안 관리 목록 조회 결과를 반환하는 DTO. (작성자: 서주연)
 *
 * <p>관리 화면 목록에서 직원 식별 정보, 부서/직급/권한 표시명,
 * 로그인 실패 횟수, 계정 잠금 상태, 잠금/해제 이력을 함께 보여주기 위한 응답 형태이다.</p>
 */
public record AccountSecurityStatusListResponseDTO(

        String employeeNo,  // 보안 상태를 관리하는 직원 사번
        String name,  // 직원 이름
        String department,  // 부서 코드
        String departmentName,  // 화면에 보여줄 부서명
        String position,  // 직급 코드
        String positionName,  // 화면에 보여줄 직급명
        String role,  // 시스템 역할 코드
        String roleName,  // 화면에 보여줄 시스템 역할명
        int loginFailCount,  // 연속 로그인 실패 횟수
        boolean accountLocked,  // 계정 잠금 여부
        LocalDateTime lastFailedAt,  // 마지막 로그인 실패 시각
        LocalDateTime lockedAt,  // 계정 잠금 시각
        LocalDateTime unlockedAt,  // 관리자 잠금 해제 시각
        String unlockedBy  // 잠금을 해제한 관리자 사번
) {
    // 직원 기본 정보와 계정 보안 상태를 계정 보안 관리 목록 응답 DTO로 변환하는 메서드
    // 보안 상태가 아직 없으면 로그인 실패 횟수는 0, 잠금/실패/해제 정보는 기본값으로 처리
    public static AccountSecurityStatusListResponseDTO from(Employee employee, AccountSecurityStatus securityStatus) {
        return new AccountSecurityStatusListResponseDTO(
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getDepartment().name(),
                employee.getDepartment().getDisplayName(),
                employee.getPosition().name(),
                employee.getPosition().getDisplayName(),
                employee.getRole().name(),
                employee.getRole().getDisplayName(),
                securityStatus == null ? 0 : securityStatus.getLoginFailCount(),
                securityStatus != null && securityStatus.isAccountLocked(),
                securityStatus == null ? null : securityStatus.getLastFailedAt(),
                securityStatus == null ? null : securityStatus.getLockedAt(),
                securityStatus == null ? null : securityStatus.getUnlockedAt(),
                securityStatus == null || securityStatus.getUnlockedBy() == null
                        ? null
                        : securityStatus.getUnlockedBy().getEmployeeNo()
        );
    }
}
