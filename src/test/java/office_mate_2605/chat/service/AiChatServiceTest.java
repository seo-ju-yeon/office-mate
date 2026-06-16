package office_mate_2605.chat.service;

import office_mate_2605.calender.domain.CalendarEvent;
import office_mate_2605.calender.domain.CalendarScope;
import office_mate_2605.calender.repository.CalendarEventRepository;
import office_mate_2605.project.domain.Project;
import office_mate_2605.project.domain.ProjectMember;
import office_mate_2605.project.domain.ProjectStatus;
import office_mate_2605.project.domain.ProjectTask;
import office_mate_2605.project.domain.TaskStatus;
import office_mate_2605.project.repository.ProjectMemberRepository;
import office_mate_2605.project.repository.ProjectRepository;
import office_mate_2605.project.repository.ProjectTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private CalendarEventRepository calendarEventRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectTaskRepository projectTaskRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @InjectMocks
    private AiChatService aiChatService;

    @Test
    void generateAiResponse_전체_일정_프로젝트_업무를_프롬프트에_담아_AI응답을_반환한다() {
        CalendarEvent event = CalendarEvent.builder()
                .id(1L)
                .title("주간 회의")
                .description("개발팀 주간 업무 공유")
                .startsAt(OffsetDateTime.parse("2026-05-14T10:00:00+09:00"))
                .endsAt(OffsetDateTime.parse("2026-05-14T11:00:00+09:00"))
                .scope(CalendarScope.TEAM)
                .department("BACKEND")
                .createdBy("SUPER001")
                .build();
        Project project = Project.builder()
                .id(10L)
                .name("ERP 개선")
                .description("사내 ERP UX 개선")
                .managerNo("SUPER001")
                .status(ProjectStatus.IN_PROGRESS)
                .progressRate(40)
                .startsOn(LocalDate.parse("2026-05-01"))
                .endsOn(LocalDate.parse("2026-06-30"))
                .build();
        ProjectTask task = ProjectTask.builder()
                .id(100L)
                .projectId(10L)
                .title("채팅 AI 연동")
                .description("AI 답변 컨텍스트 확대")
                .assigneeNo("SUPER001")
                .status(TaskStatus.IN_PROGRESS)
                .progressRate(50)
                .priority("HIGH")
                .dueOn(LocalDate.parse("2026-05-20"))
                .build();
        ProjectMember member = new ProjectMember(10L, "SUPER001");

        when(calendarEventRepository.findAll(Sort.by(Sort.Direction.ASC, "startsAt", "id")))
                .thenReturn(List.of(event));
        when(projectRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .thenReturn(List.of(project));
        when(projectTaskRepository.findAll(Sort.by(Sort.Direction.ASC, "projectId", "dueOn", "id")))
                .thenReturn(List.of(task));
        when(projectMemberRepository.findAll(Sort.by(Sort.Direction.ASC, "projectId", "employeeNo")))
                .thenReturn(List.of(member));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("오늘 오전 10시에 주간 회의가 있습니다.");

        String answer = aiChatService.generateAiResponse("오늘 일정 알려줘", "SUPER001");

        assertThat(answer).isEqualTo("오늘 오전 10시에 주간 회의가 있습니다.");
        verify(calendarEventRepository).findAll(Sort.by(Sort.Direction.ASC, "startsAt", "id"));
        verify(projectRepository).findAll(Sort.by(Sort.Direction.ASC, "id"));
        verify(projectTaskRepository).findAll(Sort.by(Sort.Direction.ASC, "projectId", "dueOn", "id"));
        verify(projectMemberRepository).findAll(Sort.by(Sort.Direction.ASC, "projectId", "employeeNo"));
        verify(requestSpec).user("오늘 일정 알려줘");

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).system(systemPromptCaptor.capture());

        assertThat(systemPromptCaptor.getValue())
                .contains("calendar_event")
                .contains("주간 회의")
                .contains("개발팀 주간 업무 공유")
                .contains("2026-05-14 10:00")
                .contains("TEAM")
                .contains("BACKEND")
                .contains("project")
                .contains("ERP 개선")
                .contains("project_task")
                .contains("채팅 AI 연동")
                .contains("project_member")
                .contains("SUPER001")
                .contains("[기준일]")
                .contains("날짜 차이가 가장 작은 일정만 답변해라")
                .contains("일정 질문에서는 project, project_task, project_member를 별도 항목으로 나열하지 말고");
    }

    @Test
    void generateAiResponse_등록된_업무데이터가_없어도_AI응답을_반환한다() {
        when(calendarEventRepository.findAll(Sort.by(Sort.Direction.ASC, "startsAt", "id")))
                .thenReturn(List.of());
        when(projectRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .thenReturn(List.of());
        when(projectTaskRepository.findAll(Sort.by(Sort.Direction.ASC, "projectId", "dueOn", "id")))
                .thenReturn(List.of());
        when(projectMemberRepository.findAll(Sort.by(Sort.Direction.ASC, "projectId", "employeeNo")))
                .thenReturn(List.of());
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("조회 가능한 일정이 없습니다.");

        String answer = aiChatService.generateAiResponse("내 일정 있어?", "SUPER001");

        assertThat(answer).isEqualTo("조회 가능한 일정이 없습니다.");

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).system(systemPromptCaptor.capture());

        assertThat(systemPromptCaptor.getValue())
                .contains("등록된 일정이 없습니다.")
                .contains("등록된 프로젝트가 없습니다.")
                .contains("등록된 프로젝트 업무가 없습니다.")
                .contains("등록된 프로젝트 멤버가 없습니다.");
    }
}
