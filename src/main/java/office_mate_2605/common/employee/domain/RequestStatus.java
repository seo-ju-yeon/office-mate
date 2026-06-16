package office_mate_2605.common.employee.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 재직 상태 변경 신청의 처리 상태를 정의하는 Enum. (작성자: 서주연)
 *
 * <p>승인 대기, 승인 완료, 반려 상태를 구분하며,
 * PostgreSQL request_status type과 1:1로 매핑된다.
 * 승인/반려 처리 전인지 확인하는 편의 메서드도 제공한다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum RequestStatus {

    PENDING("승인 대기"),
    APPROVED("승인 완료"),
    REJECTED("반려");

    private final String displayName;

    // 승인 대기 상태인지 판단
    public boolean isPending() {
        return this == PENDING;
    }
}
