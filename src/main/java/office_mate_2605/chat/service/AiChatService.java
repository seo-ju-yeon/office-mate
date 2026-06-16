package office_mate_2605.chat.service;

import lombok.RequiredArgsConstructor;
import office_mate_2605.calender.domain.CalendarEvent;
import office_mate_2605.calender.repository.CalendarEventRepository;
import office_mate_2605.common.employee.domain.Employee;
import office_mate_2605.common.employee.repository.EmployeeRepository;
import office_mate_2605.project.domain.Project;
import office_mate_2605.project.domain.ProjectMember;
import office_mate_2605.project.domain.ProjectTask;
import office_mate_2605.project.repository.ProjectMemberRepository;
import office_mate_2605.project.repository.ProjectRepository;
import office_mate_2605.project.repository.ProjectTaskRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 채팅 답변 서비스 (작성자: 박재경)
 * 일정과 프로젝트 데이터를 기반으로 사용자의 AI 질문에 답변을 생성하는 서비스 클래스다.
 */
@Service
@RequiredArgsConstructor
public class AiChatService {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ChatClient chatClient;
    private final CalendarEventRepository calendarEventRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final EmployeeRepository employeeRepository;

    /* @AI로 입력된 질문을 업무 데이터 기반 답변으로 변환한다. */
    public String generateAiResponse(String senderNo, String question) {
        LocalDate today = LocalDate.now(KOREA_ZONE);

        // 1. 발신자 정보 및 직원 정보 조회
        Employee loginUser = employeeRepository.findByEmployeeNoAndDeletedAtIsNull(senderNo).orElse(null);
        List<Employee> allEmployees = employeeRepository.findAll(Sort.by(Sort.Direction.ASC, "employeeNo"));
        Map<String, Employee> employeeMap = allEmployees.stream()
                .collect(Collectors.toMap(Employee::getEmployeeNo, e -> e, (p1, p2) -> p1));

        String currentUserContext = "정보 없음";
        if (loginUser != null) {
            currentUserContext = String.format("- 사원번호: %s\n- 이름: %s\n- 부서: %s\n- 직급: %s",
                    loginUser.getEmployeeNo(), loginUser.getName(),
                    loginUser.getDepartment() != null ? loginUser.getDepartment() : "없음",
                    loginUser.getPosition() != null ? loginUser.getPosition() : "없음"
            );
        }

        // 2. 질문의 대상자(Target) 분석 및 특정
        String targetNo = (loginUser != null) ? loginUser.getEmployeeNo() : null;
        String targetName = (loginUser != null) ? loginUser.getName() : null;
        boolean isCompanyWide = false;

        boolean hasCompanyWideKeyword = question.contains("전체") || question.contains("모든") || question.contains("우리 회사")
                || question.contains("프론트엔드") || question.contains("백엔드")
                || question.contains("경영지원") || question.contains("마케팅") || question.contains("DB관리");

        boolean foundOtherEmployee = false;
        for (Employee emp : allEmployees) {
            if (!emp.getEmployeeNo().equals(senderNo) && question.contains(emp.getName())) {
                targetNo = emp.getEmployeeNo();
                targetName = emp.getName();
                foundOtherEmployee = true;
                break;
            }
        }

        if (hasCompanyWideKeyword && !foundOtherEmployee) {
            isCompanyWide = true;
        }

        // 3. 질문 도메인(분야) 판별
        boolean isProjectDomain = question.contains("프로젝트") || question.contains("pjt");
        boolean isTaskDomain = question.contains("업무") || question.contains("태스크") || question.contains("할 일") || question.contains("할일");
        boolean isCalendarDomain = question.contains("일정") || question.contains("캘린더");
        boolean isEmployeeDomain = question.contains("직원") || question.contains("사원") || question.contains("멤버 목록") || question.contains("조직도");

        // 명시적인 도메인이 없거나 '완료' 키워드가 있을 때 유연하게 도메인 오픈
        if (!isProjectDomain && !isTaskDomain && !isCalendarDomain && !isEmployeeDomain) {
            isProjectDomain = true;
        }

        List<Project> filteredProjects = new ArrayList<>();
        List<ProjectTask> filteredTasks = new ArrayList<>();
        List<CalendarEvent> filteredEvents = new ArrayList<>();
        List<ProjectMember> filteredMembers = new ArrayList<>();

        // 프로젝트 도메인 백엔드 필터링 로직
        if (isProjectDomain || question.contains("프로젝트")) {
            List<Project> sourceProjects = projectRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
            List<ProjectMember> sourceMembers = projectMemberRepository.findAll();

            boolean wantsDoneOnly = question.contains("완료된 프로젝트") || (question.contains("완료") && !question.contains("업무"));
            boolean wantsSpecificStatus = question.contains("준비") || question.contains("보류") || question.contains("지연") || question.contains("취소") || question.contains("전체");

            if (isCompanyWide) {
                filteredProjects = sourceProjects;
            } else if (targetNo != null) {
                final String tNo = targetNo;
                Set<Long> joinedProjectIds = sourceMembers.stream()
                        .filter(m -> tNo.equals(m.getEmployeeNo()))
                        .map(ProjectMember::getProjectId)
                        .collect(Collectors.toSet());

                filteredProjects = sourceProjects.stream()
                        .filter(p -> tNo.equals(p.getManagerNo()) || joinedProjectIds.contains(p.getId()))
                        .collect(Collectors.toList());
            }

            // 명시적인 특수 상태 조회가 아니라면 기본적으로 진행 중(IN_PROGRESS)과 완료(DONE) 프로젝트를 모두 노출
            if (wantsDoneOnly) {
                filteredProjects = filteredProjects.stream()
                        .filter(p -> p.getStatus() != null && "DONE".equals(p.getStatus().name()))
                        .collect(Collectors.toList());
            } else if (!wantsSpecificStatus) {
                filteredProjects = filteredProjects.stream()
                        .filter(p -> p.getStatus() != null &&
                                ("IN_PROGRESS".equals(p.getStatus().name()) || "DONE".equals(p.getStatus().name())))
                        .collect(Collectors.toList());
            }

            Set<Long> finalPids = filteredProjects.stream().map(Project::getId).collect(Collectors.toSet());
            filteredMembers = sourceMembers.stream()
                    .filter(m -> finalPids.contains(m.getProjectId()))
                    .collect(Collectors.toList());
        }

        // 업무(Task) 도메인 백엔드 필터링 로직
        if (isTaskDomain || question.contains("업무") || question.contains("태스크") || question.contains("할 일")) {
            List<ProjectTask> sourceTasks = projectTaskRepository.findAll(Sort.by(Sort.Direction.ASC, "projectId", "dueOn", "id"));
            boolean wantsDoneTasks = question.contains("완료") || question.contains("끝난");
            boolean wantsAllTasks = question.contains("전체") || question.contains("모든");

            if (isCompanyWide) {
                filteredTasks = sourceTasks;
            } else if (targetNo != null) {
                final String tNo = targetNo;
                filteredTasks = sourceTasks.stream()
                        .filter(t -> tNo.equals(t.getAssigneeNo()))
                        .collect(Collectors.toList());
            }

            // 완료된 업무 요청 시 DONE만 필터링, 평소에는 미완료 업무만 필터링하도록 이중화 분리 완비
            if (wantsDoneTasks) {
                filteredTasks = filteredTasks.stream()
                        .filter(t -> t.getStatus() != null && "DONE".equals(t.getStatus().name()))
                        .collect(Collectors.toList());
            } else if (!wantsAllTasks) {
                filteredTasks = filteredTasks.stream()
                        .filter(t -> t.getStatus() != null && !"DONE".equals(t.getStatus().name()))
                        .collect(Collectors.toList());
            }
        }

        // 일정(Calendar) 도메인 백엔드 필터링 로직
        if (isCalendarDomain) {
            List<CalendarEvent> sourceEvents = calendarEventRepository.findAll(Sort.by(Sort.Direction.ASC, "startsAt", "id"));
            if (isCompanyWide) {
                filteredEvents = sourceEvents.stream()
                        .filter(e -> e.getOwnerNo() != null && !e.getOwnerNo().isBlank())
                        .collect(Collectors.toList());
            } else if (targetNo != null) {
                final String tNo = targetNo;
                filteredEvents = sourceEvents.stream()
                        .filter(e -> tNo.equals(e.getOwnerNo()))
                        .collect(Collectors.toList());
            }
        }

        // 4. 필터링된 데이터셋 변환
        String calendarContext = filteredEvents.isEmpty() ? (isCalendarDomain ? "조건에 부합하는 일정 데이터가 전혀 없습니다." : "조회 항목 아님")
                : filteredEvents.stream().map(event -> formatCalendarEvent(event, employeeMap)).collect(Collectors.joining("\n"));

        String projectContext = filteredProjects.isEmpty() ? (isProjectDomain ? "조건에 부합하는 프로젝트 데이터가 전혀 없습니다." : "조회 항목 아님")
                : filteredProjects.stream().map(project -> formatProject(project, employeeMap)).collect(Collectors.joining("\n"));

        String projectTaskContext = filteredTasks.isEmpty() ? (isTaskDomain ? "조건에 부합하는 업무 데이터가 전혀 없습니다." : "조회 항목 아님")
                : filteredTasks.stream().map(task -> formatProjectTask(task, employeeMap)).collect(Collectors.joining("\n"));

        String projectMemberContext = filteredMembers.isEmpty() ? (isProjectDomain ? "조건에 부합하는 프로젝트 멤버 데이터가 전혀 없습니다." : "조회 항목 아님")
                : filteredMembers.stream().map(member -> formatProjectMember(member, employeeMap)).collect(Collectors.joining("\n"));

        String employeeContext = allEmployees.isEmpty() ? "등록된 직원 정보가 없습니다."
                : allEmployees.stream()
                  .filter(e -> e.getStatus() != null && "ACTIVE".equals(e.getStatus().name()))
                  .map(this::formatEmployee)
                  .collect(Collectors.joining("\n"));

        // 5. 프롬프트 규칙 주입 및 답변 생성
        return chatClient.prompt()
                .options(ChatOptions.builder().temperature(0.0).build())
                .system("""
                        너는 사내 협업 시스템 'Office Mate'의 철저한 규칙 기반 데이터 출력 매퍼다.
                        반드시 제공된 하단의 데이터 컨텍스트 내용만을 유일한 근거로 삼아 팩트만을 답변해라. 절대 유추하거나 유실된 데이터를 임의로 채워 넣지 마라.
                        답변 전체에서 마크다운 강조 기호('**')는 가독성을 해치므로 절대로 사용하지 마라.
                        내부 데이터 식별값인 사원번호(예: 사번=2026...)는 답변 본문에 절대로 노출하지 마라.
                        출력 시 줄바꿈과 숫자 글머리 기호(1., 2.)를 활용하여 균일하고 단정한 포맷으로 가독성 있게 나열해라.
                        
                        [1. 도메인 격리 및 프로젝트 참여 매핑 대원칙]
                        사용자의 질문 영역(도메인)에 따라 제공된 데이터를 기반으로 성실하게 답변해라.
                        - '프로젝트' 관련 질문 시: [project]와 [project_member] 데이터를 유기적으로 조합하여, [질문 대상 사원]이 '관리자'이거나 참여자 목록에 있는 프로젝트의 이름, 상태, 진행률, 관리자 정보를 정확히 출력해라.
                        - '업무' 관련 질문 시: [project_task] 데이터를 바탕으로 업무 제목, 담당자, 상태, 우선순위, 마감일을 매핑해 깔끔하게 출력해라.
                        - '일정' 관련 질문 시: [calendar_event] 데이터를 바탕으로 일정 제목, 시간, 소유자만 출력해라.
                        - '직원/사원' 관련 질문 시: [employee] 데이터를 바탕으로 사원들의 이름, 부서, 직급만 출력해라.
                        
                        [2. 데이터 유무 판별 조항]
                        - 백엔드 컨텍스트 영역에 "조건에 부합하는 데이터가 전혀 없습니다"라고 명시되어 있는 도메인에 한해서만 "해당 조건에 맞는 항목이 현재 존재하지 않습니다."라고 명확하게 답변해라.
                        - 데이터가 한 줄이라도 존재한다면 절대로 누락시키지 말고 포맷에 맞춰 성실하게 리스트업해라.
                        
                        [3. 출력 포맷 균일화 약정]
                        질문하는 사원이나 질문의 어조, 형태에 상관없이 동일 도메인 질문군에 대해서는 언제나 완전히 일치하는 항목(포맷 및 상세도)으로 통일성 있게 출력해라.
                        
                        [기준일]
                        %s
                        
                        [현재 로그인한 사용자]
                        %s
                        
                        [질문 대상 사원]
                        %s
                        
                        [calendar_event]
                        %s
                        
                        [project]
                        %s
                        
                        [project_task]
                        %s
                        
                        [project_member]
                        %s
                        
                        [employee]
                        %s
                        """.formatted(
                        today,
                        currentUserContext,
                        (targetName != null ? targetName : "본인"),
                        calendarContext,
                        projectContext,
                        projectTaskContext,
                        projectMemberContext,
                        employeeContext
                ))
                .user(question)
                .call()
                .content();
    }

    /* 일정 엔티티를 AI 프롬프트용 한 줄 문자열로 변환한다. */
    private String formatCalendarEvent(CalendarEvent event, Map<String, Employee> employeeMap) {
        return "- id=%d, 제목=%s, 설명=%s, 시작=%s, 종료=%s, 종일=%s, 범위=%s, 소유자=%s, 부서=%s, 프로젝트=%s, 반복=%s"
                .formatted(
                        event.getId(),
                        nullToDash(event.getTitle()),
                        nullToDash(event.getDescription()),
                        event.getStartsAt().format(DATE_TIME_FORMATTER),
                        event.getEndsAt().format(DATE_TIME_FORMATTER),
                        event.isAllDay() ? "예" : "아니오",
                        event.getScope() != null ? event.getScope().name() : "-",
                        resolveEmployeeName(event.getOwnerNo(), employeeMap),
                        nullToDash(event.getDepartment()),
                        event.getProjectId() != null ? event.getProjectId() : "-",
                        event.getRecurrenceRule() != null ? event.getRecurrenceRule().name() : "-"
                );
    }

    /* 프로젝트 엔티티를 AI 프롬프트용 한 줄 문자열로 변환한다. */
    private String formatProject(Project project, Map<String, Employee> employeeMap) {
        String koreanStatus = "-";
        if (project.getStatus() != null) {
            koreanStatus = switch (project.getStatus().name()) {
                case "READY" -> "준비";
                case "IN_PROGRESS" -> "진행 중";
                case "DELAYED" -> "지연";
                case "ON_HOLD" -> "보류";
                case "DONE" -> "완료";
                case "CANCELED" -> "취소";
                default -> project.getStatus().name();
            };
        }

        return "- id=%d, 이름=%s, 설명=%s, 주관부서=%s, 관리자=%s, 상태=%s, 진행률=%s%%, 시작일=%s, 종료일=%s"
                .formatted(
                        project.getId(),
                        nullToDash(project.getName()),
                        nullToDash(project.getDescription()),
                        project.getOwnerDepartment() != null ? project.getOwnerDepartment().name() : "-",
                        resolveEmployeeName(project.getManagerNo(), employeeMap),
                        koreanStatus,
                        project.getProgressRate() != null ? project.getProgressRate() : 0,
                        project.getStartsOn() != null ? project.getStartsOn() : "-",
                        project.getEndsOn() != null ? project.getEndsOn() : "-"
                );
    }

    /* 프로젝트 업무 엔티티를 AI 프롬프트용 한 줄 문자열로 변환한다. */
    private String formatProjectTask(ProjectTask task, Map<String, Employee> employeeMap) {
        String koreanStatus = "-";
        if (task.getStatus() != null) {
            koreanStatus = switch (task.getStatus().name()) {
                case "TODO" -> "할 일";
                case "IN_PROGRESS" -> "진행 중";
                case "DONE" -> "완료";
                default -> task.getStatus().name();
            };
        }

        return "- id=%d, 프로젝트ID=%d, 제목=%s, 설명=%s, 담당자=%s, 상태=%s, 진행률=%d%%, 우선순위=%s, 배정자=%s, 마감일=%s, 중요업무=%s, 완료시각=%s"
                .formatted(
                        task.getId(),
                        task.getProjectId(),
                        nullToDash(task.getTitle()),
                        nullToDash(task.getDescription()),
                        resolveEmployeeName(task.getAssigneeNo(), employeeMap),
                        koreanStatus,
                        task.getProgressRate(),
                        nullToDash(task.getPriority()),
                        resolveEmployeeName(task.getAssignedBy(), employeeMap),
                        task.getDueOn() != null ? task.getDueOn() : "-",
                        task.isCritical() ? "예" : "아니오",
                        task.getCompletedAt() != null ? task.getCompletedAt() : "-"
                );
    }

    /* 프로젝트 멤버 엔티티를 AI 프롬프트용 한 줄 문자열로 변환한다. */
    private String formatProjectMember(ProjectMember member, Map<String, Employee> employeeMap) {
        return "- 프로젝트ID=%d, 참여자=%s, 참여일시=%s"
                .formatted(
                        member.getProjectId(),
                        resolveEmployeeName(member.getEmployeeNo(), employeeMap),
                        member.getJoinedAt() != null ? member.getJoinedAt() : "-"
                );
    }

    /* 직원 엔티티를 AI 프롬프트용 한 줄 문자열로 변환한다. */
    private String formatEmployee(Employee employee) {
        return "- 사번=%s, 이름=%s, 부서=%s, 직급=%s, 상태=%s"
                .formatted(
                        nullToDash(employee.getEmployeeNo()),
                        nullToDash(employee.getName()),
                        employee.getDepartment() != null ? employee.getDepartment().getDisplayName() : "-",
                        employee.getPosition() != null ? employee.getPosition().getDisplayName() : "-",
                        employee.getStatus() != null ? employee.getStatus().name() : "ACTIVE"
                );
    }

    /* employeeNo을 이름으로 변환한다. */
    private String resolveEmployeeName(String employeeNo, Map<String, Employee> employeeMap) {
        if (employeeNo == null || employeeNo.isBlank()) {
            return "-";
        }
        Employee employee = employeeMap.get(employeeNo);
        if (employee == null) {
            return employeeNo;
        }
        return employee.getName();
    }

    /* null 또는 빈 문자열을 대시로 치환한다. */
    private String nullToDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }
}