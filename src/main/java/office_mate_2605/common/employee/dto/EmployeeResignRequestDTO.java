package office_mate_2605.common.employee.dto;

import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * 직원 퇴사 처리 요청 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>관리자가 특정 직원을 퇴사 상태로 변경할 때 퇴사일을 전달한다.
 * 퇴사일을 비워 보내면 Service에서 오늘 날짜를 기본값으로 처리한다.</p>
 */
public record EmployeeResignRequestDTO(
        // 퇴사일 (오늘 또는 과거 날짜만 허용)
        @PastOrPresent(message = "퇴사일은 오늘 또는 과거 날짜여야 합니다.")
        LocalDate resignedOn
) {
}
