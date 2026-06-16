package office_mate_2605.management.auth.repository;

import office_mate_2605.management.auth.domain.AccountSecurityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 직원 계정 보안 상태 Entity의 조회와 저장을 담당하는 Repository. (작성자: 서주연)
 *
 * <p>account_security_status 테이블을 통해 로그인 실패 횟수,
 * 계정 잠금 여부, 잠금/해제 이력을 조회하고 저장한다.
 * 관리자 화면에서 잠긴 계정만 필터링할 때 사용하는 조회 메서드도 제공한다.</p>
 */
@Repository
public interface AccountSecurityStatusRepository extends JpaRepository<AccountSecurityStatus, String> {

    // 현재 잠금 상태인 계정 목록을 찾음
    // 관리자 화면에서 잠긴 계정만 따로 보여줄 때 사용할 수 있다.
    List<AccountSecurityStatus> findByAccountLockedTrue();
}
