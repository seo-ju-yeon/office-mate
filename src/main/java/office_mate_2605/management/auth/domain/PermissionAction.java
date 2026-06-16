package office_mate_2605.management.auth.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 권한 정책에서 사용할 행위 유형을 정의하는 Enum. (작성자: 서주연)
 *
 * <p>생성, 조회, 수정, 삭제, 관리, 내보내기 같은 권한 행위를 표현한다.
 * 현재 RBAC 화면은 제거되었지만 권한 행위 표현이 필요한 기능에서 재사용할 수 있다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum PermissionAction {

    CREATE("생성"),
    READ("조회"),
    UPDATE("수정"),
    DELETE("삭제"),
    MANAGE("관리"),
    EXPORT("내보내기");

    private final String displayName;
}
