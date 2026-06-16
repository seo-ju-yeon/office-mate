package office_mate_2605.management.auth.dto;

import office_mate_2605.security.dto.EmployeePrincipal;

/**
 * 현재 로그인 사용자 정보를 반환하는 DTO. (작성자: 서주연)
 *
 * <p>/api/auth/me API가 SecurityContext의 EmployeePrincipal에서 꺼낸 사번, 이름,
 * 부서, 직급, 시스템 권한, 임시 비밀번호 변경 필요 여부를 화면에 전달한다.
 * Entity나 Principal 전체를 그대로 노출하지 않기 위해 별도 응답 DTO로 감싼다.</p>
 */
public record AuthMeResponseDTO(

        String employeeNo,  // 로그인 ID로 사용하는 사번
        String name,  // 직원 실명
        String department,  // 부서 코드 문자열 (예: BACKEND)
        String position,  // 직급 코드 문자열 (예: STAFF)
        String role,  // 시스템 역할 문자열 (예: USER, ADMIN, SUPER)
        boolean tempPasswordRequired  // true이면 최초 로그인 후 비밀번호 변경 화면으로 보냄
) {
    // SecurityContext의 EmployeePrincipal을 현재 로그인 사용자 응답 DTO로 변환하는 메서드
    // 화면에 필요한 인증 사용자 정보만 골라 응답
    public static AuthMeResponseDTO from(EmployeePrincipal principal) {
        return new AuthMeResponseDTO(
                principal.getEmployeeNo(),
                principal.getName(),
                principal.getDepartment(),
                principal.getPosition(),
                principal.getRole(),
                principal.isTempPasswordRequired()
        );
    }
}
