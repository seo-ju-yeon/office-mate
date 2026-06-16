package office_mate_2605.project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.board.service.BoardService;
import office_mate_2605.project.domain.DepartmentCode;
import office_mate_2605.project.domain.Project;
import office_mate_2605.project.domain.ProjectJoinResult;
import office_mate_2605.project.domain.ProjectStatus;
import office_mate_2605.project.dto.ProjectDetailResponseDTO;
import office_mate_2605.project.dto.ProjectRequestDTO;
import office_mate_2605.project.mapper.ProjectMapper;
import office_mate_2605.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 프로젝트 기본 정보 및 생명주기 관리 Service 구현체 (작성자: 강수현)

 * <p> 신규 프로젝트 생성 시 관리자를 멤버로 자동 등록하며, 삭제 요청 시 데이터를 보존하는 논리 삭제(CANCELED)를 수행합니다.
 * MyBatis 조인 결과를 활용해 대시보드용 상세 DTO를 구성하고 프로젝트의 현재 진행 상태를 동기화합니다.</p>
 */

@Log4j2
@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {
    private final ProjectMapper projectMapper;
    private final ProjectRepository projectRepository;
    private final BoardService boardService;

    // 상세 정보를 포함한 전체 프로젝트 리스트 조회
    // MyBatis 조인 결과를 DTO로 변환하여 반환하며 트랜잭션 내에서 처리
    @Override
    @Transactional(readOnly = true)
    public List<ProjectDetailResponseDTO> getAllProjectsWithDetails() {
        log.info("--- ProjectServiceImpl getAllProjectsWithDetails ---");

        // 1. DB에서 조인된 결과 가져오기
        List<ProjectJoinResult> rawResults = projectMapper.selectProjectsWithDetails();

        // 🔍 [방어 코드 추가] 만약 MyBatis에서 가져온 원본 리스트가 null이거나 비어있다면 즉시 빈 리스트 반환
        if (rawResults == null || rawResults.isEmpty()) {
            log.warn("MyBatis 조회 결과가 존재하지 않습니다. (rawResults is empty)");
            return List.of();
        }

        // 2. 반환할 DTO 리스트 준비
        List<ProjectDetailResponseDTO> dtoList = new ArrayList<>();

        // 3. 하나씩 변환 후 리스트에 추가
        for (ProjectJoinResult result : rawResults) {
            // 결과 객체가 null인 경우 예외 방지
            if (result == null) continue;

            ProjectDetailResponseDTO dto = ProjectDetailResponseDTO.builder()
                    .id(result.getId())
                    .name(result.getName())
                    .status(result.getStatus())
                    .description(result.getDescription())
                    .progressRate(result.getProgressRate())
                    .startsOn(result.getStartsOn()) // 프론트엔드엔 변동된 스크립트가 대응하므로 기존 필드 유지
                    .endsOn(result.getEndsOn())
                    .managerName(result.getManagerName())
                    .departmentName(result.getDepartmentName())
                    .build();
            dtoList.add(dto);
        }
        log.info("총 {}건의 프로젝트 DTO 변환 완료", dtoList.size());
        return dtoList;
    }

    // 프로젝트 신규 등록 및 관리자 멤버 설정
    // 프로젝트 엔티티를 저장하고, 관리자 사번을 멤버 테이블에 추가하여 권한을 부여
    @Override
    @Transactional
    public Long createProject(ProjectRequestDTO dto) {
        log.info("--- ProjectServiceImpl createProject ---");

        // 1. 프로젝트 엔티티 빌드 및 저장
        Project project = Project.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .ownerDepartment(DepartmentCode.valueOf(String.valueOf(dto.getOwnerDepartment())))
                .startsOn(dto.getStartsOn())
                .endsOn(dto.getEndsOn())
                .managerNo(dto.getManagerNo())
                .status(ProjectStatus.READY)
                .progressRate(0)
                .build();

        Project savedProject = projectRepository.save(project);

        projectRepository.flush();

        Long projectId = savedProject.getId();

        // 2. 관리자를 프로젝트 참여 인원(project_member)으로 등록
        try {
            // 이제 DB에 상위 프로젝트 데이터가 물리적으로 저장되었으므로 외래키 제약조건을 통과합니다.
            projectMapper.insertProjectMember(projectId, savedProject.getManagerNo());
            log.info("프로젝트(ID: {}) 멤버 테이블에 관리자({}) 등록 완료", projectId, savedProject.getManagerNo());
        } catch (Exception e) {
            log.error("관리자 자동 등록 중 에러 발생: {}", e.getMessage());
            // 디버깅 편의를 위해 진짜 원인이 된 예외 객체(e)를 래핑하여 던집니다.
            throw new RuntimeException("프로젝트 생성 중 멤버 등록 실패", e);
        }

        // 3. 프로젝트 전용 공지 게시판 자동 생성
        boardService.createProjectBoard(projectId, dto.getName());

        return projectId;
    }

    // 프로젝트 논리적 취소 처리
    // 프로젝트 ID를 조회하여 상태값만 'CANCELED'로 변경하여 보존
    @Override
    @Transactional
    public void deleteProject(Long id) {
        log.info("--- ProjectServiceImpl deleteProject ---");
        log.info("--- ProjectServiceImpl: 논리 삭제 실행 (ID: {}) ---", id);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다. ID: " + id));

        Project canceledProject = project.toBuilder()
                .status(ProjectStatus.CANCELED)
                .build();

        projectRepository.save(canceledProject);
        log.info("프로젝트 ID: {} 상태가 CANCELED로 변경되었습니다.", id);

        // 프로젝트 취소 시 게시판 논리 삭제
        boardService.deleteProjectBoard(id);
    }

    // 프로젝트 진행 상태 업데이트
    // 요청받은 상태명(문자열)을 Enum으로 변환하여 프로젝트의 현재 상태를 수정
    @Override
    @Transactional
    public void updateProjectStatus(Long id, String statusName) {
        log.info("--- ProjectServiceImpl updateProjectStatus ---");
        log.info("--- ProjectServiceImpl: 상태 변경 실행 (ID: {}, New Status: {}) ---", id, statusName);

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 프로젝트입니다."));

        ProjectStatus newStatus = ProjectStatus.valueOf(statusName.toUpperCase());

        Project updatedProject = project.toBuilder()
                .status(newStatus)
                .build();

        projectRepository.save(updatedProject);

        // DONE 또는 CANCELED 상태로 변경 시 게시판 논리 삭제
        if (newStatus == ProjectStatus.DONE || newStatus == ProjectStatus.CANCELED) {
            boardService.deleteProjectBoard(id);
        }
    }
}
