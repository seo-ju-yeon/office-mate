package office_mate_2605.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.dashboard.domain.NotificationRefType;
import office_mate_2605.dashboard.service.NotificationService;
import office_mate_2605.project.domain.ProjectMember;
import office_mate_2605.project.domain.ProjectMemberId;
import office_mate_2605.project.dto.EmployeeResponseDTO;
import office_mate_2605.project.dto.ProjectMemberResponseDTO;
import office_mate_2605.project.mapper.ProjectMapper;
import office_mate_2605.project.repository.ProjectMemberRepository;
import office_mate_2605.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 프로젝트 참여 멤버 관리 Service 구현체 (작성자: 강수현)

 * <p> 프로젝트 멤버 등록 시 중복 여부를 검증하고, 등록 완료 후 대상자에게 초대 알림을 발송합니다.
 * JPA를 통한 멤버 상태 관리와 MyBatis를 활용한 상세 정보 조회를 혼용하여 처리합니다.</p>
 */

@Log4j2
@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {
    private final ProjectMemberRepository projectMemberRepository;
    private final NotificationService notificationService;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    // 프로젝트 멤버 추가 및 초대 알림 전송
    // 중복 등록을 체크하고 멤버 정보를 저장한 뒤, 해당 직원에게 프로젝트 초대 알림을 발송
    @Override
    @Transactional
    public void addMember(Long projectId, String employeeNo) {
        log.info("--- ProjectMemberServiceImpl addMember ---");

        // 1. 중복 체크
        if (projectMemberRepository.existsByProjectIdAndEmployeeNo(projectId, employeeNo)) {
            throw new RuntimeException("이미 등록된 멤버입니다.");
        }

        // 2. 프로젝트 정보 조회 (프로젝트명을 가져오기 위함)
        String projectName = projectRepository.findById(projectId)
                .map(project -> project.getName())
                .orElse("알 수 없는 프로젝트");

        // 3. 멤버 저장
        ProjectMember member = new ProjectMember(projectId, employeeNo);
        projectMemberRepository.save(member);

        // 4. 알림 생성 (메시지에 프로젝트명 포함)
        notificationService.createNotification(
                employeeNo,
                "새 프로젝트 초대",
                "[" + projectName + "] 프로젝트의 멤버로 초대되었습니다.", // ✨ 프로젝트명 포함
                String.valueOf(projectId),
                NotificationRefType.PROJECT
        );

        log.info("--- [Notification] [{}] 프로젝트 초대 알림 전송 완료 ---", projectName);
    }

    // 프로젝트 멤버 삭제
    // 복합 키(ProjectMemberId)를 사용하여 프로젝트에서 특정 사원을 삭제
    @Override
    public void removeMember(Long projectId, String employeeNo) {
        log.info("--- ProjectMemberServiceImpl removeMember ---");
        ProjectMemberId id = new ProjectMemberId(projectId, employeeNo);
        projectMemberRepository.deleteById(id);
    }

    // 프로젝트별 참여 멤버 상세 조회
    // MyBatis Mapper를 호출하여 JOIN된 멤버의 상세 정보(이름, 직급 등)를 조회
    @Override
    public List<ProjectMemberResponseDTO> getMembersByProjectId(Long projectId) {
        log.info("--- ProjectMemberServiceImpl getMembersByProjectId ---");
        // MyBatis 매퍼를 호출해서 JOIN된 상세 정보를 가져옵니다.
        return projectMapper.selectProjectMemberDetails(projectId);
    }

    // 부서별 전체 직원 목록 조회
    // 특정 부서에 소속된 전체 직원 정보를 조회하며, 필터가 없을 시 전체를 반환
    @Override
    public List<EmployeeResponseDTO> getAllEmployees(String department) {
        log.info("--- [Service] Fetching all employees with filter: {} ---", department);
        // Mapper의 @Param("department")와 매칭됩니다.
        return projectMapper.selectAllEmployeesWithDept(department);
    }
}
