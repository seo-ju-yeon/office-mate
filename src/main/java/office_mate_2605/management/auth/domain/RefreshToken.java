package office_mate_2605.management.auth.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import office_mate_2605.common.employee.domain.Employee;

import java.time.LocalDateTime;

/**
 * JWT refresh token의 저장과 무효화 상태를 관리하는 Entity. (작성자: 서주연)
 *
 * <p>보안을 위해 refresh token 원문이 아닌 SHA-256 해시값만 저장하며,
 * 만료 시각과 로그아웃/비밀번호 변경/계정 비활성화 시 revokedAt을 기록한다.
 * accessToken 재발급 가능 여부를 판단하는 기준 데이터이다.</p>
 */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    // Refresh token PK
    // 자동 증가 값
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 토큰 소유자 직원
    // 직원 한 명이 여러 refresh token을 가질 수 있음
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_no", nullable = false)
    private Employee employee;

    // Refresh token SHA-256 해시값
    // 원문은 저장하지 않고 검증 시 요청 토큰을 해시한 뒤 이 값과 비교
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    // Refresh token 만료 시각
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // 로그아웃 또는 강제 만료 처리 시각
    // 유효한 토큰이면 null
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    // Refresh token 최초 저장용 빌더
    // 토큰 원문이 아니라 해시값과 만료 시각만 저장
    @Builder
    private RefreshToken(Employee employee, String tokenHash, LocalDateTime expiresAt) {
        this.employee = employee;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    // 토큰을 즉시 무효화하는 메서드
    // 로그아웃 또는 보안 이벤트 발생 시 호출
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    // 토큰이 유효한지 확인하는 메서드
    // revoke 되지 않았고 만료 시각이 현재보다 미래이면 유효
    public boolean isValid() {
        return this.revokedAt == null && LocalDateTime.now().isBefore(this.expiresAt);
    }

    // 토큰이 만료되었는지 확인하는 메서드
    // 현재 시각이 만료 시각보다 이후이면 만료
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
