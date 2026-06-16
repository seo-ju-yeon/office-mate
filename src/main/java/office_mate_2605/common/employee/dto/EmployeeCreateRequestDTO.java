package office_mate_2605.common.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import office_mate_2605.common.employee.domain.DepartmentCode;
import office_mate_2605.common.employee.domain.PositionCode;
import office_mate_2605.common.employee.domain.SystemRole;

import java.time.LocalDate;

/**
 * 신규 직원 등록 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>관리자 또는 SUPER가 직원 등록 화면에서 입력한 사번, 이름, 이메일,
 * 부서, 직급, 시스템 권한, 입사일, 사번/이메일 중복확인 여부를 JSON 요청으로 전달한다.
 * password, status, deletedAt처럼 클라이언트가 직접 수정하면 안 되는 필드는 받지 않는다.</p>
 */
public record EmployeeCreateRequestDTO(

        // 회사에서 발급한 사번. 로그인 ID로 사용되며 생성 후 변경하지 않음
        @NotBlank(message = "사번은 필수입니다.")
        @Size(max = 30, message = "사번은 30자 이하여야 합니다.")
        String employeeNo,

        // 직원 실명. 사내 화면, 감사 로그, 게시글 작성자 표시에 사용함
        @NotBlank(message = "직원 이름은 필수입니다.")
        @Size(max = 50, message = "직원 이름은 50자 이하여야 합니다.")
        String name,

        // 직원 이메일. 알림, 비밀번호 복구, Google Calendar 연동 등에 사용함
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 120, message = "이메일은 120자 이하여야 합니다.")
        String email,

        // 소속 부서. PostgreSQL department_code enum과 동일한 값만 받을 수 있음
        @NotNull(message = "부서는 필수입니다.")
        DepartmentCode department,

        // 직급. PostgreSQL position_code enum과 동일한 값만 받을 수 있음
        @NotNull(message = "직급은 필수입니다.")
        PositionCode position,

        // 시스템 역할 (null이면 Service에서 USER로 처리)
        SystemRole role,

        // 입사일 (null이면 Service에서 오늘 날짜로 처리)
        @PastOrPresent(message = "입사일은 오늘 또는 과거 날짜여야 합니다.")
        LocalDate joinedOn,

        // 사번 중복확인 완료 여부
        // 화면 검증 우회를 막기 위해 서버에서도 true 여부를 확인
        // 실제 중복 여부는 EmployeeService에서에서 DB로 다시 검증
        Boolean employeeNoDuplicateChecked,

        // 이메일 중복확인 완료 여부
        // 화면 검증 우회를 막기 위해 서버에서도 true 여부를 확인
        // 실제 중복 여부는 EmployeeService에서 DB로 다시 검증
        Boolean emailDuplicateChecked
) {
}
