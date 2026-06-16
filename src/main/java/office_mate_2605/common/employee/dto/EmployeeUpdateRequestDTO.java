package office_mate_2605.common.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import office_mate_2605.common.employee.domain.DepartmentCode;
import office_mate_2605.common.employee.domain.PositionCode;
import office_mate_2605.common.employee.domain.SystemRole;

/**
 * 직원 기본 정보 수정 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>직원 이름, 이메일, 부서, 직급 수정 요청을 전달한다.
 * null 값은 수정하지 않고 기존 값을 유지하며,
 * 시스템 역할 변경은 보안상 직원 관리 전용 DTO에서만 처리한다.</p>
 */
public record EmployeeUpdateRequestDTO(
        // 변경할 이름 (null이면 기존 이름을 유지)
        @Size(max = 50, message = "직원 이름은 50자 이하여야 합니다.")
        String name,

        // 변경할 이메일 (null이면 기존 이메일을 유지)
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 120, message = "이메일은 120자 이하여야 합니다.")
        String email,

        // 변경할 부서 (null이면 기존 부서를 유지)
        DepartmentCode department,

        // 변경할 직급 (null이면 기존 직급을 유지)
        PositionCode position,

        // 변경할 시스템 역할 (null이면 기존 역할을 유지)
        SystemRole role
) {
}
