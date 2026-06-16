package office_mate_2605.common.employee.repository;

import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.domain.EmploymentStatus;
import office_mate_2605.common.employee.domain.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 직원 Entity의 조회와 중복 검사를 담당하는 Repository. (작성자: 서주연)
 *
 * <p>사번/이메일 중복 확인, 논리 삭제되지 않은 직원 조회,
 * 조직도용 직원 목록 조회, ACTIVE SUPER 수 계산 같은 직원 관리 쿼리를 제공한다.
 * 직원 삭제는 물리 삭제가 아니라 deletedAt 기준의 논리 삭제 정책을 따른다.</p>
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    // 사번 및 이메일 중복 확인
    boolean existsByEmployeeNo(String employeeNo);
    boolean existsByEmail(String email);

    // 수정용 중복 체크
    // 본인을 제외한 다른 사람이 해당 이메일을 쓰는지 확인
    boolean existsByEmailAndEmployeeNoNot(String email, String employeeNo);

    // 현재 활성화된(ACTIVE) SUPER 관리자 수 계산 (마지막 관리자 삭제 방지용)
    long countByRoleAndStatusAndDeletedAtIsNull(SystemRole role, EmploymentStatus status);

    // 단건 조회, 삭제되지 않은 직원만 조회
    // 사번 기준
    Optional<Employee> findByEmployeeNoAndDeletedAtIsNull(String employeeNo);

    // 사번 + 이메일 기준
    Optional<Employee> findByEmployeeNoAndEmailAndDeletedAtIsNull(String employeeNo, String email);

    // 목록 조회, 삭제되지 않은 직원 전체를 사번 오름차순으로 조회
    // 나중에 직원 수가 늘어나면 Pageable 기반 페이징 조회로 전환할 예정이다.
    List<Employee> findByDeletedAtIsNullOrderByEmployeeNoAsc();

    // 조직도용, 부서(ASC) -> 직급(DESC) -> 사번(ASC) 순 정렬된 필터링 목록
    List<Employee> findByStatusInAndDeletedAtIsNullOrderByDepartmentAscPositionDescEmployeeNoAsc(
            List<EmploymentStatus> statuses
    );

    // 휴직, 퇴직자를 제외한 모든 직원 목록 조회
    // 채팅방 좌측 직원 목록을 띄울 때 사용
    List<Employee> findByStatusAndDeletedAtIsNullOrderByDepartmentAscPositionDescEmployeeNoAsc(
            EmploymentStatus status
    );

    // 퇴사하지 않은(deletedAt이 null인) 모든 직원 목록 조회
    // 채팅방 좌측 직원 목록을 띄울 때 사용
    List<Employee> findAllByDeletedAtIsNull();
}
