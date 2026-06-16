package office_mate_2605.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.domain.EmployeeRole;
import office_mate_2605.common.employee.repository.EmployeeRoleRepository;
import office_mate_2605.management.auth.service.AccountSecurityStatusService;
import office_mate_2605.security.dto.EmployeePrincipal;
import office_mate_2605.security.repository.APIUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Spring Security 로그인 검증에 사용할 직원 정보를 조회하는 Service. (작성자: 서주연)
 *
 * <p>로그인 화면에서 입력한 사번으로 Employee를 조회하고,
 * employee.role과 employee_roles의 권한 정보를 Spring Security GrantedAuthority로 변환한다.
 * 계정 잠금 상태도 함께 조회해 EmployeePrincipal을 생성한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class APIUserDetailsService implements UserDetailsService {
    // UserDetailsService: Spring Security가 로그인 검증 중 사용자 정보를 조회할 때 사용하는 표준 인터페이스
    // 사용자가 employeeNo/password로 로그인하면, 먼저 loadUserByUsername(employeeNo)가 호출됨
    // 이 클래스는 DB에서 직원 정보를 조회한 뒤 Spring Security가 이해할 수 있는 UserDetails로 변환함

    private final APIUserRepository apiUserRepository;  // employee 단일 테이블을 JPA로 조회
    private final EmployeeRoleRepository employeeRoleRepository;  // mployee_roles 단일 테이블에서 추가 역할을 조회
    private final AccountSecurityStatusService accountSecurityStatusService;  // 로그인 실패/잠금 상태 조회

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("--- APIUserDetailsService.loadUserByUsername() 진입 username={} ---", username);

        // username : 로그인 화면에서 입력한 사번

        // findByEmployeeNoAndDeletedAtIsNull: employee_no = username AND deleted_at IS NULL 조건으로 조회함
        Employee employee = apiUserRepository.findByEmployeeNoAndDeletedAtIsNull(username)
                .orElseThrow(() -> {
                    // 사용자가 없으면 Spring Security가 로그인 실패로 처리함
                    return new UsernameNotFoundException("employee_not_found");
                });
        log.info("검색된 직원: {}", employee);

        // DB에서 조회한 직원 정보를 Spring Security가 사용할 로그인 사용자 정보로 바꿈
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();  // authorities: 로그인한 사용자가 가진 권한 목록
        // employee 테이블의 기본 역할을 Spring Security 권한으로 바꾸는 코드
        authorities.add(new SimpleGrantedAuthority(employee.getRole().toGrantedAuthority()));
        // employee_roles 테이블에 들어 있는 추가 권한을 가져와서 합치는 코드
        authorities.addAll(loadAdditionalAuthorities(employee.getEmployeeNo()));

        // 로그인 보안 상태는 employee 테이블과 분리되어 있으므로 별도 Service로 잠금 여부를 조회
        boolean accountLocked = accountSecurityStatusService.isAccountLocked(employee.getEmployeeNo());

        EmployeePrincipal employeePrincipal = new EmployeePrincipal(
                employee.getEmployeeNo(),
                employee.getPassword(),
                employee.getName(),
                employee.getDepartment().name(),  // 부서 enum 이름
                employee.getPosition().name(),  // 직급 enum 이름
                employee.getRole().name(),  // 기본 시스템 역할 enum
                employee.getStatus().name(),  // 재직 상태 enum
                employee.isTempPasswordRequired(),
                accountLocked,
                authorities  // security 권한 목록
        );
        log.info("로그인 주체 생성. 사번:{}, 이름:{}, 부서:{}, 직급:{}, 기본 시스템 역할:{}, 재직: {}, 임시비번:{}, 계정잠금:{}",
                employee.getEmployeeNo(), employee.getName(), employee.getDepartment().name(), employee.getPosition().name(),
                employee.getRole().name(), employee.getStatus().name(), employee.isTempPasswordRequired(), accountLocked);
        return employeePrincipal;
    }

    private Set<GrantedAuthority> loadAdditionalAuthorities(String employeeNo) {
        log.info("--- APIUserDetailsService.loadAdditionalAuthorities() 진입 employeeNo={} ---", employeeNo);

        // employee.role은 기본 역할이고, employee_roles는 추가 역할
        // 예를 들어 기본 USER이지만, ADMIN 역할을 추가 부여할 수 있다.
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        // findByIdEmployeeNo : EmployeeRoleId 안의 employeeNo 값을 기준으로 조회
        for (EmployeeRole employeeRole : employeeRoleRepository.findByIdEmployeeNo(employeeNo)) {
            authorities.add(new SimpleGrantedAuthority(employeeRole.getId().getRoleCode().toGrantedAuthority()));
        }
        return authorities;
    }
}
