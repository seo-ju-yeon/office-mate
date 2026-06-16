package office_mate_2605.common.employee.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OfficeMate 시스템 전역 권한을 정의하는 Enum. (작성자: 서주연)
 *
 * <p>일반 사용자, 관리자, 최고 관리자 권한을 구분하며,
 * PostgreSQL system_role type과 1:1로 매핑된다.
 * Spring Security GrantedAuthority로 변환할 때 ROLE_ prefix를 붙여 사용한다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum SystemRole {

    USER("일반 사용자"),
    ADMIN("관리자"),
    SUPER("최고 관리자");

    private final String displayName;

    // Spring Security 권한 문자열로 변환
    public String toGrantedAuthority() {
        return "ROLE_" + this.name();
    }
}
