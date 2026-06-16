package office_mate_2605.common.employee.repository;

import office_mate_2605.common.employee.domain.EmployeeStatusRequest;
import office_mate_2605.common.employee.domain.EmploymentRequestType;
import office_mate_2605.common.employee.domain.RequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 직원 재직 상태 변경 신청 Entity의 조회와 중복 검사를 담당하는 Repository. (작성자: 서주연)
 *
 * <p>휴직, 퇴사, 복직 신청의 PENDING 중복 여부를 확인하고,
 * 본인 신청 이력, SUPER 승인 대기 목록, 승인/반려 직전의 최종 신청 상태를 조회한다.
 * EntityGraph를 사용해 신청자와 결재자 정보를 함께 조회하여 N+1 문제를 줄인다.</p>
 */
@Repository
public interface EmployeeStatusRequestRepository extends JpaRepository<EmployeeStatusRequest, Long> {

    /* 직원 상태 변경 신청서 전용 레포지토리 */

    // 신청 중복 방지 체크 (한 직원이 휴직과 퇴사 신청을 동시에 올리면 승인 기준이 모호해지기 때문)
    boolean existsByEmployeeEmployeeNoAndStatus(String employeeNo, RequestStatus status);

    // 내 신청 이력 조회 (최신순 Desc)
    // EntityGraph를 통해 신청자(employee)와 결재자(decidedBy) 정보를 한 방에 쿼리로 가져옴 (N+1 문제 해결)
    @EntityGraph(attributePaths = {"employee", "decidedBy"})
    List<EmployeeStatusRequest> findByEmployeeEmployeeNoOrderByRequestedAtDesc(String employeeNo);

    // 관리자용 결재 대기 목록 (오래된 순 Asc)
    @EntityGraph(attributePaths = {"employee", "decidedBy"})
    List<EmployeeStatusRequest> findByStatusOrderByRequestedAtAsc(RequestStatus status);

    // 결재 직전 최종 확인
    // 승인/반려 버튼을 누른 순간에도 아직 PENDING 상태인 신청만 조회
    // 이미 다른 SUPER가 처리한 신청을 다시 처리하지 못하게 막는 1차 방어로직
    @EntityGraph(attributePaths = {"employee", "decidedBy"})
    Optional<EmployeeStatusRequest> findByIdAndStatus(Long id, RequestStatus status);
}
