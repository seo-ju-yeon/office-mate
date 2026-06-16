package office_mate_2605.common.employee.dto;

import office_mate_2605.common.employee.domain.DepartmentCode;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.domain.EmploymentStatus;
import office_mate_2605.common.employee.domain.PositionCode;
import office_mate_2605.common.employee.domain.SystemRole;

import java.time.LocalDate;

/**
 * 직원 조회 결과를 반환하는 DTO. (작성자: 서주연)
 *
 * <p>직원 목록, 직원 단건 조회, 조직도, 마이페이지 등에서 필요한
 * 기본 인사 정보와 권한, 재직 상태, 임시 비밀번호 변경 필요 여부를 전달한다.
 * 비밀번호와 삭제 시각 같은 내부 보안/관리 필드는 응답하지 않는다.</p>
 */
public record EmployeeResponseDTO(

        String employeeNo,  // 사번
        String name,  // 직원 이름
        String email,  // 직원 이메일
        DepartmentCode department,  // 소속 부서
        PositionCode position,  // 직급
        SystemRole role,  // 시스템 역할
        EmploymentStatus status,  // 재직 상태 (ACTIVE, ON_LEAVE, RESIGNED)
        LocalDate joinedOn,  // 입사일
        LocalDate resignedOn,  // 퇴사일 (재직자는 null)
        boolean tempPasswordRequired  // 임시 비밀번호 변경 필요 여부
) {
    // Employee Entity를 직원 조회 응답 DTO로 변환하며 내부 관리 필드는 제외
    public static EmployeeResponseDTO from(Employee employee) {
        return new EmployeeResponseDTO(
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getRole(),
                employee.getStatus(),
                employee.getJoinedOn(),
                employee.getResignedOn(),
                employee.isTempPasswordRequired()
        );
    }
}
