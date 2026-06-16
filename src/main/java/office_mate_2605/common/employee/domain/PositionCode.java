package office_mate_2605.common.employee.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 직원 직급 코드를 정의하는 Enum. (작성자: 서주연)
 *
 * <p>PostgreSQL position_code type과 1:1로 매핑되며,
 * 각 직급의 한글 표시명과 직급 순서 값을 함께 가진다.
 * position_rank 테이블의 rank_order 순서와 동일하게 선언해 직급 비교에 사용한다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum PositionCode {

    STAFF("사원", 1),
    ASSISTANT_MANAGER("대리", 2),
    MANAGER("과장", 3),
    GENERAL_MANAGER("부장", 4),
    DEPUTY_GENERAL_MANAGER("차장", 5),
    CEO("사장", 6);

    private final String displayName;

    // position_rank.rank_order 와 동일한 순서 값
    // 숫자가 클수록 더 높은 직급이며, 직급 비교에 사용
    private final int rankOrder;

    // 현재 직급이 비교 대상 직급보다 높은지 판단
    // other는 비교 대상 직급
    // 현재 직급의 rankOrder가 other보다 크면 true 반환
    public boolean isHigherThan(PositionCode other) {
        return this.rankOrder > other.rankOrder;
    }
}
