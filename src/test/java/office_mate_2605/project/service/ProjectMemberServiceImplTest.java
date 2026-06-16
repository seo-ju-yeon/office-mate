package office_mate_2605.project.service;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.project.domain.ProjectMember;
import office_mate_2605.project.domain.ProjectMemberId;
import office_mate_2605.project.dto.EmployeeResponseDTO;
import office_mate_2605.project.dto.ProjectMemberResponseDTO;
import office_mate_2605.project.repository.ProjectMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class ProjectMemberServiceImplTest {
    @Autowired
    private ProjectMemberService projectMemberService;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Test
    void addAndGetMemberDetailsLogTest() {
        Long projectId = 1L;
        String employeeNo = "BE001";

        log.info("==================================================");

        try {
            projectMemberService.addMember(projectId, employeeNo);
            log.info("멤버 추가 완료: 프로젝트={}, 사번={}", projectId, employeeNo);
        } catch (Exception e) {
            log.error("멤버 추가 실패: {}", e.getMessage());
        }

        log.info("상세 정보 조회를 시작합니다...");
        List<ProjectMemberResponseDTO> members = projectMemberService.getMembersByProjectId(projectId);

        if (members != null && !members.isEmpty()) {
            log.info("총 {}명의 멤버가 검색되었습니다.", members.size());

            members.forEach(m -> {
                log.info("--------------------------------------------------");
                log.info("  프로젝트 ID : {}", m.getProjectId());
                log.info("  사번         : {}", m.getEmployeeNo());
                log.info("  성함 (JOIN)  : {}", m.getEmployeeName());
                log.info("  부서 (JOIN)  : {}", m.getDepartmentName());
                log.info("  참여일       : {}", m.getJoinedAt());
            });
        } else {
            log.warn("검색된 멤버가 없습니다. SQL이나 DB 데이터를 확인하세요.");
        }
        log.info("==================================================");
    }

    @Test
    void removeMemberLogTest() {
        Long projectId = 1L;
        String employeeNo = "BE001";

        // 삭제 전 추가
//        projectMemberService.addMember(projectId, employeeNo);

        // 삭제 실행
        projectMemberService.removeMember(projectId, employeeNo);
        log.info("Project={}, Employee={}", projectId, employeeNo);

        // 결과 확인
        List<ProjectMemberResponseDTO> members = projectMemberService.getMembersByProjectId(projectId);
        log.info("삭제 후 멤버 수: {}명", members.size());
    }

    @Test
    void getAllEmployeesLogTest() {
        // 1. 전체 조회 테스트 (필터 없음)
        log.info("========== 전체 직원 목록 조회 시작 ==========");
        List<EmployeeResponseDTO> allEmployees = projectMemberService.getAllEmployees(null);

        if (allEmployees != null && !allEmployees.isEmpty()) {
            log.info("조회된 전체 직원 수: {}명", allEmployees.size());
            allEmployees.forEach(e -> {
                log.info("  [사번: {}] 이름: {}, 부서: {} ({})",
                        e.getEmployeeNo(), e.getEmployeeName(), e.getDepartmentName(), e.getDepartmentCode());
            });
        } else {
            log.warn("조회된 직원이 없습니다. DB의 employee 테이블을 확인하세요.");
        }

        // 2. 부서 필터링 조회 테스트
        String filterDept = "BACKEND"; // Enum DepartmentCode 중 하나
        log.info("========== 부서 필터링 조회 시작: {} ==========", filterDept);

        List<EmployeeResponseDTO> filteredEmployees = projectMemberService.getAllEmployees(filterDept);

        if (filteredEmployees != null && !filteredEmployees.isEmpty()) {
            log.info("{} 부서 직원 수: {}명", filterDept, filteredEmployees.size());
            filteredEmployees.forEach(e -> {
                log.info("  -> 필터링 결과: {} (사번: {})", e.getEmployeeName(), e.getEmployeeNo());
            });
        } else {
            log.warn("{} 부서에 소속된 직원이 없거나, 타입 캐스팅(::text) 확인이 필요합니다.", filterDept);
        }
        log.info("==================================================");
    }


}