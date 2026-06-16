package office_mate_2605.project.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 프로젝트 세부 업무 Entity (작성자: 강수현)

 * <p> 프로젝트 내 개별 태스크의 배정, 우선순위, 마감일 및 진척 상황을 관리합니다.
 * 진척도 변경에 따른 상태 자동 업데이트(TODO -> IN_PROGRESS -> DONE) 로직을 포함하고 있습니다. </p>
 */

@Entity
@Table(name = "project_task")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "assignee_no")
    private String assigneeNo;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "task_status") // 이 부분을 명시
    private TaskStatus status;

    @Column(name = "progress_rate", nullable = false)
    private int progressRate;

    @Column(nullable = false)
    private String priority;

    @Column(name = "assigned_by")
    private String assignedBy;

    @Column(name = "due_on")
    private LocalDate dueOn;

    @Column(name = "is_critical", nullable = false)
    private boolean isCritical;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    // 업무 정보 일괄 수정용
    public void updateInfo(String title, String description, String priority, LocalDate dueOn) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueOn = dueOn;
    }

    // 상태 변경 메서드 (상태 변경 시 완료 시간 기록 등 부가 로직 처리 가능)
    public void changeStatus(TaskStatus status) {
        this.status = status;
        if (status == TaskStatus.DONE) {
            this.completedAt = LocalDateTime.now();
            this.progressRate = 100;
        }
    }

    // 진척도 변경
    public void updateProgressRate(int progressRate) {
        if (progressRate < 0 || progressRate > 100) {
            throw new IllegalArgumentException("진척도는 0에서 100 사이여야 합니다.");
        }

        this.progressRate = progressRate;

        // 진척도가 100%가 되면 자동으로 완료 처리하는 로직 추가
        if (this.progressRate == 100) {
            this.status = TaskStatus.DONE;
            this.completedAt = LocalDateTime.now();
        } else if (this.progressRate > 0 && this.status == TaskStatus.TODO) {
            // 진척도가 0보다 크면 '진행 중'으로 상태 자동 변경
            this.status = TaskStatus.IN_PROGRESS;
        }
    }
}
