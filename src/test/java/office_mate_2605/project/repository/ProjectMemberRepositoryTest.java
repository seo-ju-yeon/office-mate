package office_mate_2605.project.repository;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.project.domain.ProjectMember;
import office_mate_2605.project.domain.ProjectMemberId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class ProjectMemberRepositoryTest {
    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Test
    void saveAndFindMemberTest() {
        Long projectId = 1L;
        String employeeNo = "BE001";
        ProjectMember member = new ProjectMember(projectId, employeeNo);

        projectMemberRepository.save(member);
        projectMemberRepository.flush(); // DB에 반영

        ProjectMemberId id = new ProjectMemberId(projectId, employeeNo);
        Optional<ProjectMember> savedMember = projectMemberRepository.findById(id);

        if (savedMember.isPresent()) {
            log.info("멤버 조회 성공: ProjectID={}, EmployeeNo={}",
                    savedMember.get().getProjectId(), savedMember.get().getEmployeeNo());
            log.info("참여 시간: {}", savedMember.get().getJoinedAt());
        } else {
            log.error("멤버를 찾을 수 없습니다.");
        }
    }

    @Test
    void countByProjectIdTest() {
        Long projectId = 1L;

        long count = projectMemberRepository.countByProjectId(projectId);

        log.info("프로젝트(ID: {})의 총 참여 인원 수: {}명", projectId, count);
    }
}