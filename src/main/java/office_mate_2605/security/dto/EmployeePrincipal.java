package office_mate_2605.security.dto;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Spring SecurityContext에 저장되는 OfficeMate 로그인 사용자 객체. (작성자: 서주연)
 *
 * <p>Spring Security 기본 User에 사번, 이름, 부서, 직급, 시스템 역할,
 * 재직 상태, 임시 비밀번호 변경 필요 여부, 계정 잠금 여부를 추가로 담는다.
 * Controller와 Filter는 이 객체를 통해 현재 로그인 사용자의 인증 정보를 확인한다.</p>
 */
@Getter
public class EmployeePrincipal extends User {
    // EmployeePrincipal: Spring Security의 기본 User + 현 프로잭트의 직원 정보를 추가한 클래스
    // -> SecurityContext에 저장되는 로그인 사용자 정보 객체

    private final String employeeNo;  // 로그인 ID로 사용하는 직원 사번
    private final String name;  // 직원 이름
    private final String department;  // 부서 코드 문자열
    private final String position;  // 직급 코드 문자열
    private final String role;  // 시스템 권한 코드 문자열
    private final String status;  // 재직 상태 코드 문자열
    // 임시 비밀번호 변경 필요 여부
    // true이면 비밀번호 변경 완료 전까지 주요 API 접근 제한
    private final boolean tempPasswordRequired;
    // 계정 잠금 여부
    // true이면 Spring Security가 로그인 성공을 차단
    private final boolean accountLocked;

    public EmployeePrincipal(
            String employeeNo,
            String password,
            String name,
            String department,
            String position,
            String role,
            String status,
            boolean tempPasswordRequired,
            boolean accountLocked,
            Collection<? extends GrantedAuthority> authorities
    ) {
        // 부모 User의 username은 로그인 ID인 사번으로 사용
        // enabled는 true로 두고, 휴직/퇴사 계정 차단은 로그인 성공 후 Handler와 TokenCheckFilter에서 처리
        // accountNonLocked에는 accountLocked의 반대값을 넣어 잠긴 계정의 로그인을 Spring Security 단계에서 차단
        super(employeeNo, password, true, true, true, !accountLocked, authorities);
        this.employeeNo = employeeNo;
        this.name = name;
        this.department = department;
        this.position = position;
        this.role = role;
        this.status = status;
        this.tempPasswordRequired = tempPasswordRequired;
        this.accountLocked = accountLocked;  // 계정 잠금 상태
    }
}
