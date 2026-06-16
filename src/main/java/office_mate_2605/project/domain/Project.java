package office_mate_2605.project.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDate;

/**
 * 프로젝트 정보 Entity (작성자: 강수현)

 * <p> 프로젝트의 명칭, 기간, 주관 부서 및 현재 진행 상태와 진척률 정보를 관리합니다.
 * 프로젝트의 생명주기(준비, 진행, 완료 등)를 추적하는 핵심 데이터 모델입니다. </p>
 */

@Entity
@Table(name = "project")
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    /*
    project 테이블과 1:1 매핑되는 엔티티 클래스
    */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_department")
    @JdbcType(PostgreSQLEnumJdbcType.class) // PostgreSQL의 Enum 타입을 직접 사용하도록 지정
    private DepartmentCode ownerDepartment;

    @Column(name = "manager_no", length = 30)
    private String managerNo;

    @Column(nullable = false)
    @Builder.Default  // 빌더 사용 시 "READY"가 기본값으로 적용됩니다.
    @Enumerated(EnumType.STRING) // DB에는 문자열로 저장되도록 설정
    private ProjectStatus status = ProjectStatus.READY;

    @Column(name = "progress_rate", nullable = false)
    @Builder.Default  // 빌더 사용 시 0이 기본값으로 적용됩니다.
    private Integer progressRate = 0;

    @Column(name = "starts_on")
    private LocalDate startsOn;

    @Column(name = "ends_on")
    private LocalDate endsOn;
}
