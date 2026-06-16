package office_mate_2605.management.auth.repository;

import office_mate_2605.management.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JWT refresh token Entity의 조회와 저장을 담당하는 Repository. (작성자: 서주연)
 *
 * <p>로그아웃, 비밀번호 변경, 휴직/퇴사 처리 시 아직 revokedAt이 없는
 * 활성 refresh token을 조회해 revoke 처리할 때 사용한다.</p>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // employee.employeeNo가 employeeNo와 같고, revokedAt이 NULL인 refresh token 목록을 찾음
    // 로그아웃이나 비밀번호 변경 시 아직 살아있는 refresh token을 모두 폐기하기 위해 사용
    List<RefreshToken> findByEmployeeEmployeeNoAndRevokedAtIsNull(String employeeNo);
}
