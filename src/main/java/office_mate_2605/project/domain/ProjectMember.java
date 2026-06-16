package office_mate_2605.project.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 프로젝트 참여 멤버 Entity (작성자: 강수현)

 * <p> 프로젝트와 사원 간의 다대다(N:M) 관계를 매핑하며 멤버별 프로젝트 참여 일시를 기록합니다.
 * 복합키 구조를 사용하여 특정 프로젝트 내 동일 사원의 중복 참여를 방지합니다. </p>
 */

@Entity
@Table(name = "project_member")
@IdClass(ProjectMemberId.class) // 복합키 클래스 지정
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(value = AuditingEntityListener.class) // joined_at 자동 생성을 위해
public class ProjectMember {
    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Id
    @Column(name = "employee_no", length = 30)
    private String employeeNo;

    @CreatedDate
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    public ProjectMember(Long projectId, String employeeNo) {
        this.projectId = projectId;
        this.employeeNo = employeeNo;
        this.joinedAt = LocalDateTime.now();
    }
}
