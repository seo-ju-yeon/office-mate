package office_mate_2605.common.employee.dto;

import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import office_mate_2605.common.employee.domain.DepartmentCode;
import office_mate_2605.common.employee.domain.EmploymentStatus;
import office_mate_2605.common.employee.domain.PositionCode;
import office_mate_2605.common.employee.domain.SystemRole;

import java.time.LocalDate;

/**
 * SUPER 권한자의 직원 관리 수정 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>직원 이름, 부서, 직급 같은 기본 정보와 시스템 권한,
 * 재직 상태, 퇴사일을 함께 수정할 때 사용한다.
 * role/status 변경은 보안 영향도가 크므로 일반 수정 DTO와 분리해 SUPER 전용 흐름에서만 처리한다.</p>
 */
public record EmployeeManagementUpdateRequestDTO(
        @NotBlank(message = "직원 이름은 필수입니다.")
        @Size(max = 50, message = "직원 이름은 50자 이하여야 합니다.")
        String name,  // 이름

        @NotNull(message = "부서는 필수입니다.")
        DepartmentCode department,  // 부서

        @NotNull(message = "직급은 필수입니다.")
        PositionCode position,  // 직급

        @NotNull(message = "시스템 권한은 필수입니다.")
        SystemRole role,  // 시스템 권한

        @NotNull(message = "재직 상태는 필수입니다.")
        EmploymentStatus status,  // 재직 상태

        @PastOrPresent(message = "퇴사일은 오늘 또는 과거 날짜여야 합니다.")
        LocalDate resignedOn  // 퇴사일
) {
}
