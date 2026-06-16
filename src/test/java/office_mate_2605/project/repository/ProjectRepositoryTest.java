package office_mate_2605.project.repository;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.project.domain.DepartmentCode;
import office_mate_2605.project.domain.Project;
import office_mate_2605.project.domain.ProjectStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Log4j2
@SpringBootTest
@Transactional// PgVectorConfig에서 만든 트랜잭션 매니저를 명시적으로 사용
class ProjectRepositoryTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    @Rollback(false)
    void insertAndSelectTest() {
        // 테스트용 프로젝트 객체 생성
        Project project = Project.builder()
                .name("사내 협업 플랫폼")
                .ownerDepartment(DepartmentCode.BACKEND) // Enum 사용
                .managerNo("ADMIN001")
                .status(ProjectStatus.IN_PROGRESS)
                .progressRate(15)
                .startsOn(LocalDate.now())
                .endsOn(LocalDate.now().plusDays(60))
                .build();

        // 리포지토리를 통한 저장 실행
        Project savedProject = projectRepository.save(project);

        // 결과 확인을 위한 로그 출력
        log.info("=========================================");
        log.info("DB 저장 완료 - ID: {}", savedProject.getId());
        log.info("프로젝트명: {}", savedProject.getName());
        log.info("주관부서: {}", savedProject.getOwnerDepartment());
        log.info("=========================================");
    }
}