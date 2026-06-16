package office_mate_2605.common.employee.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 직급별 순서 기준을 관리하는 Entity. (작성자: 서주연)
 *
 * <p>PostgreSQL position_code enum 값을 PK로 사용하며,
 * 숫자가 클수록 높은 직급이 되도록 rank_order를 보관한다.
 * 직원 권한 판단이나 직급 비교가 필요할 때 기준 데이터로 사용한다.</p>
 */
@Entity
@Table(name = "position_rank")
@Getter
@NoArgsConstructor
public class PositionRank {
    // 직급 코드, position_code enum 값을 PK로 사용
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private PositionCode position;

    // 직급 순서, 숫자가 클수록 높은 직급
    // STAFF=1, ASSISTANT_MANAGER=2, MANAGER=3
    // GENERAL_MANAGER=4, DEPUTY_GENERAL_MANAGER=5, CEO=6
    @Column(name = "rank_order", nullable = false, unique = true)
    private Short rankOrder;
}
