package office_mate_2605.management.audit_log.policy;

import office_mate_2605.management.audit_log.domain.AuditAction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 감사 로그 대상 API 정책을 중앙에서 관리하는 Registry. (작성자: 서주연)
 *
 * <p>감사 로그로 남겨야 하는 인증, 직원 관리, 재직 상태 변경, 계정 보안, 감사 로그 내보내기 API를 등록한다.
 * AuditLogAspect는 현재 요청의 메서드와 URI를 기준으로 이 Registry에서 정책을 찾아 감사 여부를 판단한다.</p>
 */
@Component
public class AuditLogPolicyRegistry {

    // 감사 로그를 남길 API 목록을 이곳에서 중앙 관리
    private final List<AuditLogPolicy> policies = List.of(
            // 로그인은 Controller가 아닌 Security Filter/Handler에서 감사 로그를 직접 남기므로 정책 목록에서 제외
            // APILoginSuccessHandler / APILoginFailureHandler + AuditLoginLogService
            // 로그아웃
            AuditLogPolicy.of("POST", "/api/auth/logout", AuditAction.LOGOUT, "AUTH"),
            // 비밀번호 변경
            AuditLogPolicy.of("POST", "/api/auth/change-password", AuditAction.UPDATE, "AUTH"),
            // 비밀번호 재설정 임시 비밀번호 발급 요청
            AuditLogPolicy.of("POST", "/api/auth/password-reset/request", AuditAction.UPDATE, "AUTH"),
            // 임시 비밀번호 검증 후 새 비밀번호 확정
            AuditLogPolicy.of("POST", "/api/auth/password-reset/confirm", AuditAction.UPDATE, "AUTH"),
            // 직원 등록
            AuditLogPolicy.of("POST", "/api/management/employees", AuditAction.CREATE, "EMPLOYEE"),
            // 직원 기본 정보 수정
            AuditLogPolicy.of("PUT", "/api/management/employees/{employeeNo}", AuditAction.UPDATE, "EMPLOYEE", "employeeNo"),
            // 직원 관리 정보 또는 권한 변경
            AuditLogPolicy.of("PATCH", "/api/management/employees/{employeeNo}/management", AuditAction.PERMISSION_CHANGE, "EMPLOYEE", "employeeNo"),
            // 직원 퇴사 처리
            AuditLogPolicy.of("PATCH", "/api/management/employees/{employeeNo}/resign", AuditAction.STATUS_CHANGE, "EMPLOYEE", "employeeNo"),
            // 로그인 전 복직 신청
            AuditLogPolicy.of("POST", "/api/auth/return-from-leave/request", AuditAction.STATUS_CHANGE, "EMPLOYEE_STATUS_REQUEST"),
            // 본인 휴직/퇴사 신청
            AuditLogPolicy.of("POST", "/api/my/status-requests", AuditAction.STATUS_CHANGE, "EMPLOYEE_STATUS_REQUEST"),
            // 휴직/퇴사/복직 신청 승인
            AuditLogPolicy.of("PATCH", "/api/management/status-requests/{requestId}/approve", AuditAction.STATUS_CHANGE, "EMPLOYEE_STATUS_REQUEST", "requestId"),
            // 휴직/퇴사/복직 신청 반려
            AuditLogPolicy.of("PATCH", "/api/management/status-requests/{requestId}/reject", AuditAction.STATUS_CHANGE, "EMPLOYEE_STATUS_REQUEST", "requestId"),
            // 직원 관리 화면의 계정 잠금 해제
            AuditLogPolicy.of("PATCH", "/api/management/employees/{employeeNo}/unlock", AuditAction.UPDATE, "ACCOUNT_SECURITY", "employeeNo"),
            // 직원 논리 삭제
            AuditLogPolicy.of("DELETE", "/api/management/employees/{employeeNo}", AuditAction.DELETE, "EMPLOYEE", "employeeNo"),
            // 계정 보안 관리 화면의 잠금 해제
            AuditLogPolicy.of("PATCH", "/api/management/account-security/{employeeNo}/unlock", AuditAction.UPDATE, "ACCOUNT_SECURITY", "employeeNo"),
            // 감사 로그 CSV 다운로드
            AuditLogPolicy.of("GET", "/api/management/audit-logs/export.csv", AuditAction.EXPORT, "AUDIT_LOG")
    );

    // 현재 요청에 해당하는 감사 정책을 찾음
    public Optional<AuditLogPolicy> find(String httpMethod, String requestUri) {
        return policies.stream()
                .filter(policy -> policy.matches(httpMethod, requestUri))
                .findFirst();
    }
}
