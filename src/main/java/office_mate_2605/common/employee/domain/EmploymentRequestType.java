package office_mate_2605.common.employee.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 직원 재직 상태 변경 신청 유형을 정의하는 Enum. (작성자: 서주연)
 *
 * <p>휴직, 퇴사, 복직 신청 유형을 구분하며,
 * PostgreSQL employment_request_type type과 1:1로 매핑된다.
 * 화면 표시용 한글명을 함께 보관한다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum EmploymentRequestType {

    LEAVE("휴직 신청"),
    RESIGN("퇴사 신청"),
    RETURN_FROM_LEAVE("복직 신청");

    private final String displayName;
}
