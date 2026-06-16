package office_mate_2605.calender.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 구글 캘린더 연동 정보 Entity (작성자: 강수현)

 * <p> 사원별 구글 캘린더 API 접근을 위한 액세스 토큰, 리프레시 토큰 및 만료 시간을 관리합니다.
 * 토큰 갱신 시 최신 정보를 업데이트하여 지속적인 API 연동 상태를 유지하는 역할을 합니다. </p>
 */

@Entity
@Table(name = "google_calendar_link")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoogleCalendarLink {
    @Id
    @Column(name = "employee_no")
    private String employeeNo;

    @Column(name = "google_calendar_id")
    private String googleCalendarId;

    @Column(name = "access_token_encrypted")
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted")
    private String refreshTokenEncrypted;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    // 토큰 갱신 시 업데이트를 위한 메서드
    public void updateToken(String accessToken, OffsetDateTime expiresAt) {
        this.accessTokenEncrypted = accessToken; // 실제 서비스 시 암호화 로직 추가 권장
        this.tokenExpiresAt = expiresAt;
    }
}
