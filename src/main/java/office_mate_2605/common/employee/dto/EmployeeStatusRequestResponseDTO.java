package office_mate_2605.common.employee.dto;

import office_mate_2605.common.employee.domain.DepartmentCode;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.domain.EmployeeStatusRequest;
import office_mate_2605.common.employee.domain.EmploymentRequestType;
import office_mate_2605.common.employee.domain.EmploymentStatus;
import office_mate_2605.common.employee.domain.PositionCode;
import office_mate_2605.common.employee.domain.RequestStatus;

import java.time.LocalDateTime;

/**
 * 직원 재직 상태 변경 신청 조회 결과를 반환하는 DTO. (작성자: 서주연)
 *
 * <p>휴직, 퇴사, 복직 신청의 신청자 정보, 신청 유형, 사유,
 * 처리 상태, 결재자, 처리 코멘트를 화면에 필요한 형태로 평평하게 전달한다.
 * Entity의 Lazy Loading 구조나 내부 도메인 관계가 API 응답에 직접 노출되지 않도록 한다.</p>
 */
public record EmployeeStatusRequestResponseDTO(

        Long id,  // 신청 PK
        String employeeNo,  // 신청자 사번
        String employeeName,  // 신청자 이름
        DepartmentCode department,  // 신청자 부서
        PositionCode position,  // 신청자 직급
        EmploymentStatus employeeStatus,  // 신청자의 현재 재직 상태
        EmploymentRequestType requestType,  // 신청 유형 (LEAVE, RESIGN, RETURN_FROM_LEAVE)
        String reason,  // 신청 사유
        LocalDateTime requestedAt,  // 신청 시각
        RequestStatus status,  // 처리 상태 (PENDING, APPROVED, REJECTED)
        String decidedByNo,  // 처리자 사번 (아직 처리 전이면 null)
        String decidedByName,  // 처리자 이름 (아직 처리 전이면 null)
        LocalDateTime decidedAt,  // 처리 시각 (아직 처리 전이면 null)
        String decisionComment  // 처리 코멘트
) {
    // EmployeeStatusRequest Entity를 신청 이력/관리 화면 응답 DTO로 변환
    public static EmployeeStatusRequestResponseDTO from(EmployeeStatusRequest request) {
        Employee employee = request.getEmployee();
        Employee decidedBy = request.getDecidedBy();

        return new EmployeeStatusRequestResponseDTO(
                request.getId(),
                employee.getEmployeeNo(),
                employee.getName(),
                employee.getDepartment(),
                employee.getPosition(),
                employee.getStatus(),
                request.getRequestType(),
                request.getReason(),
                request.getRequestedAt(),
                request.getStatus(),
                decidedBy == null ? null : decidedBy.getEmployeeNo(),
                decidedBy == null ? null : decidedBy.getName(),
                request.getDecidedAt(),
                request.getDecisionComment()
        );
    }
}
