package office_mate_2605.common.employee.dto;

import office_mate_2605.common.employee.domain.DepartmentCode;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.domain.EmploymentStatus;
import office_mate_2605.common.employee.domain.PositionCode;
import office_mate_2605.common.employee.domain.SystemRole;

import java.time.LocalDate;

/**
 * 신규 직원 등록 결과를 반환하는 DTO. (작성자: 서주연)
 *
 * <p>등록된 직원의 기본 인사 정보와 시스템 권한, 재직 상태,
 * 임시 비밀번호 변경 필요 여부를 화면에 전달한다.
 * 보안상 비밀번호 해시는 절대 응답하지 않고 초기 비밀번호 안내 문구만 제공한다.</p>
 */
public record EmployeeCreateResponseDTO(

        String employeeNo, // 생성된 직원 사번
        String name, // 생성된 직원 이름
        String email, // 생성된 직원 이메일
        DepartmentCode department, // 생성된 직원 부서
        PositionCode position, // 생성된 직원 직급
        SystemRole role, // 생성된 직원 시스템 역할
        EmploymentStatus status, // 최초 생성 시 ACTIVE가 기본값
        LocalDate joinedOn, // 입사일
        boolean tempPasswordRequired, // true이면 최초 로그인 후 비밀번호 변경이 필요함
        String initialPasswordGuide  // 초기 비밀번호 안내 문구 (실제 비밀번호 해시는 응답하지 않음)
) {
    // Employee Entity를 직원 등록 응답 DTO로 변환하며 비밀번호 해시는 제외
    public static EmployeeCreateResponseDTO from(Employee employee) {
        return new EmployeeCreateResponseDTO(
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getEmail(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getRole(),
                employee.getStatus(),
                employee.getJoinedOn(),
                employee.isTempPasswordRequired(),
                "초기 비밀번호는 1111입니다. 최초 로그인 후 반드시 변경해야 합니다."
        );
    }
}
