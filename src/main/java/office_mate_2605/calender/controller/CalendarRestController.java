package office_mate_2605.calender.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.calender.domain.CalendarScope;
import office_mate_2605.calender.dto.CalendarDTO;
import office_mate_2605.calender.service.CalendarService;
import office_mate_2605.calender.service.GoogleCalendarService;
import office_mate_2605.dashboard.service.DashboardService;
import office_mate_2605.project.dto.TaskResponseDTO;
import office_mate_2605.project.mapper.ProjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 일정 관리(Calendar) API Controller (작성자: 강수현)

 * <p>사내 개인, 팀, 프로젝트별 일정의 조회·등록·수정·삭제(CRUD) 기능을 제공하며,
 * 구글 캘린더 API 연동을 통해 일정 내보내기 및 OAuth2 인증 프로세스를 관리하는 역할을 수행합니다.</p>
 */

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Log4j2
public class CalendarRestController {
    private final CalendarService calendarService;
    private final GoogleCalendarService googleCalendarService;
    private final DashboardService dashboardService;
    private final ProjectMapper projectMapper;

    /*
     * 구글 인증 URL 생성
     - /api/calendar/google-auth-url (GET)
     - 구글 캘린더 연동을 위해 사용자를 구글 로그인 및 권한 승인 페이지로 리다이렉트할 URL 생성
     */
    @GetMapping("/google-auth-url")
    public ResponseEntity<String> getGoogleAuthUrl(@RequestParam("empNo") String empNo) throws Exception {
        String url = googleCalendarService.getAuthorizationUrl(empNo);
        return ResponseEntity.ok(url);
    }

    /*
     * 구글 인증 콜백 처리
     - /api/calendar/callback (GET)
     - 구글 인증 완료 후 전달된 코드를 사용하여 액세스 토큰을 발급받고 DB에 저장한 뒤 팝업창을 닫음
     */
    @GetMapping("/callback")
    public ResponseEntity<String> googleCallback(@RequestParam String code, @RequestParam String state) throws Exception {
        googleCalendarService.storeTokenInDb(code, state);

        // 부모 창에 성공 메시지를 보내고 창을 닫음 (COOP 에러 방지)
        return ResponseEntity.ok(
                "<script>" +
                        "  if (window.opener) {" +
                        "    window.opener.postMessage('google-link-success', '*');" +
                        "  }" +
                        "  window.close();" +
                        "</script>"
        );
    }

    /*
     * 개인 일정 전체 및 선택된 프로젝트 업무 구글 내보내기
     - /api/calendar/export/google/all (POST)
     */
    @PostMapping("/export/google/all")
    public ResponseEntity<Map<String, String>> exportAllToGoogle(
            @RequestParam("empNo") String empNo,
            @RequestParam(value = "projectIds", required = false) List<Long> projectIds) { // projectIds 파라미터 추가 수신
        try {
            // 1. 기존 개인 일정 내보내기 실행
            calendarService.exportAllToGoogle(empNo);

            // 2. 만약 프론트에서 체크된 프로젝트 ID 목록이 넘어왔다면 순회하며 내보내기 수행
            if (projectIds != null && !projectIds.isEmpty()) {
                for (Long projectId : projectIds) {
                    googleCalendarService.exportProjectTasksToGoogle(empNo, projectId);
                }
            }

            return ResponseEntity.ok(Map.of("message", "전체 내보내기 성공"));
        } catch (Exception e) {
            log.error("전체 내보내기 중 에러 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "에러 발생: " + e.getMessage()));
        }
    }

    /*
     * 특정 프로젝트 업무 일정만 구글 내보내기
     - /api/calendar/export/google/project (POST)
     */
    @PostMapping("/export/google/project")
    public ResponseEntity<Map<String, String>> exportProjectToGoogle(
            @RequestParam("empNo") String empNo,
            @RequestParam("projectId") Long projectId) {
        log.info("=== [POST] 프로젝트 업무 구글 내보내기 요청 | 사번: {}, 프로젝트ID: {} ===", empNo, projectId);

        try {
            // 해당 프로젝트의 업무들을 조회해서 구글 캘린더에 등록하는 비즈니스 로직 호출
            googleCalendarService.exportProjectTasksToGoogle(empNo, projectId);

            return ResponseEntity.ok(Map.of("message", "프로젝트 업무 내보내기 성공"));
        } catch (Exception e) {
            log.error("프로젝트 업무 내보내기 중 에러 발생: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /*
     * 특정 일정 단건 구글 내보내기
     - /api/calendar/export/google/{id} (POST)
     - 특정 일정 하나를 선택하여 구글 캘린더에 개별 등록
     */
    @PostMapping("/export/google/{id}")
    public ResponseEntity<String> exportOneToGoogle(
            @PathVariable("id") Long id,
            @RequestParam("empNo") String empNo) {
        log.info("=== [POST] 구글 캘린더 단건 내보내기 요청 | ID: {}, 사번: {} ===", id, empNo);

        try {
            CalendarDTO detail = calendarService.getEventDetail(id);
            googleCalendarService.insertEvent(
                    empNo,
                    detail.getTitle(),
                    detail.getDescription(),
                    detail.getStartsAt(),
                    detail.getEndsAt()
            );
            return ResponseEntity.ok("구글 캘린더에 등록되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /*
     * 캘린더 범위별 일정 목록 조회
     - /api/calendar/list/{scope} (GET)
     - 지정된 범위(PERSONAL, TEAM, PROJECT)에 해당하는 일정 리스트를 사번 기준으로 조회
     */
    @GetMapping("/list/{scope}")
    public ResponseEntity<List<CalendarDTO>> getEvents(
            @PathVariable("scope") CalendarScope scope,
            @RequestParam("empNo") String empNo,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end) {
        log.info("=== [GET] 일정 목록 조회 | Scope: {}, 사번: {}, 기간: {} ~ {} ===", scope, empNo, start, end);

        List<CalendarDTO> events = calendarService.getEventsByScope(empNo, scope, start, end);
        return ResponseEntity.ok(events);
    }

    /*
     * 일정 상세 정보 조회
     - /api/calendar/detail/{id} (GET)
     - 일정 ID를 통해 특정 일정의 제목, 시간, 장소 등 세부 데이터 조회
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<CalendarDTO> getEventDetail(@PathVariable("id") Long id) {
        log.info("=== CalendarRestController getEventDetail ===");
        log.info("=== [GET] 일정 상세 조회 (ID: {}) ===", id);

        CalendarDTO detail = calendarService.getEventDetail(id);
        return ResponseEntity.ok(detail);
    }

    /*
     * 신규 일정 추가
     - /api/calendar (POST)
     - 사용자가 입력한 일정 데이터를 바탕으로 새로운 일정을 등록하고 생성된 ID 반환
     */
    @PostMapping
    public ResponseEntity<Long> register(@RequestBody CalendarDTO dto) {
        log.info("=== CalendarRestController register ===");
        log.info("=== [POST] 신규 일정 등록 | 제목: {}, 색상ID: {}, 반복: {} ===",
                dto.getTitle(), dto.getColorId(), dto.getRecurrence());

        // 유효성 검사 (기본)
        if (dto.getTitle() == null || dto.getStartsAt() == null || dto.getEndsAt() == null) {
            return ResponseEntity.badRequest().build();
        }

        Long savedId = calendarService.registerEvent(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedId);
    }

    /*
     * 기존 일정 수정
     - /api/calendar (PUT)
     - 기존에 등록된 일정의 정보(제목, 시간, 색상 등) 갱신
     */
    @PutMapping
    public ResponseEntity<String> modify(@RequestBody CalendarDTO dto) {
        log.info("=== CalendarRestController modify ===");
        log.info("=== [PUT] 일정 수정 (ID: {}) | 데이터: {} ===", dto.getId(), dto);

        if (dto.getId() == null) {
            return ResponseEntity.badRequest().body("ID가 존재하지 않습니다.");
        }

        calendarService.modifyEvent(dto);
        return ResponseEntity.ok("success");
    }

    /*
     * 일정 삭제
     - /api/calendar/{id} (DELETE)
     - 일정 ID를 기준으로 해당 일정과 연관된 모든 데이터 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> remove(@PathVariable("id") Long id) {
        log.info("=== CalendarRestController remove ===");
        log.info("=== [DELETE] 일정 삭제 (ID: {}) ===", id);

        calendarService.removeEvent(id);
        return ResponseEntity.ok("success");
    }

    /*
     * 프로젝트 관리 - 특정 팀원의 전체 일정 조회 API (개인 일정 + 프로젝트 업무 통합 조회)
     - /api/calendar/events (GET)
     - 프론트엔드 FullCalendar 연동 규격에 맞춰 특정 팀원의 일정을 제공합니다.
     */
    @GetMapping("/events")
    public ResponseEntity<List<Map<String, Object>>> getMemberEvents(
            @RequestParam("empNo") String empNo,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end) {
        log.info("=== [GET] 프로젝트 팀원 종합 스케줄 조회 요청 | 사번: {} ===", empNo);

        List<Map<String, Object>> mappedEvents = new java.util.ArrayList<>();

        // 1. 개인 일정 연동 기존 - 캘린더 서비스 활용
        try {
            List<CalendarDTO> memberEvents = calendarService.getEventsByScope(empNo, CalendarScope.PERSONAL, start, end);
            if (memberEvents != null) {
                for (CalendarDTO dto : memberEvents) {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", "CAL_" + dto.getId());
                    map.put("title", "[개인] " + dto.getTitle());
                    map.put("description", dto.getDescription());
                    map.put("color", dto.getColorId() != null ? dto.getColorId() : "#4f46e5");

                    String startStr = String.valueOf(dto.getStartsAt());
                    String endStr = String.valueOf(dto.getEndsAt());
                    map.put("start", startStr); map.put("startAt", startStr);
                    map.put("end", endStr); map.put("endAt", endStr);
                    map.put("allDay", true);

                    mappedEvents.add(map);
                }
            }
        } catch (Exception e) {
            log.error("통합 일정 API - 개인 일정 로드 실패: ", e);
        }

        // 2. 프로젝트 업무 연동 (요구사항 반영 및 필드 검증 완료)
        try {
            List<TaskResponseDTO> projectTasks = dashboardService.getMyTasks(empNo);

            if (projectTasks != null) {
                for (TaskResponseDTO task : projectTasks) {
                    // 완료된(DONE) 업무는 달력을 혼잡하게 하므로 제외
                    if ("DONE".equals(task.getStatus())) continue;

                    Map<String, Object> map = new java.util.HashMap<>();

                    // ProjectTaskController 명세에 맞춰 task.getId() -> task.getTaskId()로 수정
                    map.put("id", "TASK_" + task.getId());

                    // 무슨 프로젝트의 업무인지 알 수 있도록 앞머리에 프로젝트명 추가
                    // DTO 객체 내의 프로젝트명 getter 메서드를 호출합니다. (ex: task.getProjectName())
                    String projectName = task.getProjectName() != null ? task.getProjectName() : "알 수 없는 프로젝트";
                    map.put("title", "[" + projectName + "] " + task.getTitle());

                    map.put("description", task.getDescription());

                    // 우선순위별 테마 컬러 스위칭
                    String flagColor = "#0ca5e9"; // 보통 (스카이 블루)
                    if ("HIGH".equals(task.getPriority())) {
                        flagColor = "#f43f5e"; // 높음 (로즈 Redmond)
                    } else if ("LOW".equals(task.getPriority())) {
                        flagColor = "#94a3b8"; // 낮음 (슬레이트 그레이)
                    }
                    map.put("color", flagColor);

                    // 명세에 맞춰 마감일 변수를 task.getDueOn() -> task.getDueDate()로 수정
                    String deadline = String.valueOf(task.getDueOn());
                    map.put("start", deadline); map.put("startAt", deadline);
                    map.put("end", deadline); map.put("endAt", deadline);
                    map.put("allDay", true);

                    mappedEvents.add(map);
                }
            }
        } catch (Exception e) {
            log.error("통합 일정 API - 프로젝트 업무 데이터 바인딩 실패: ", e);
        }

        return ResponseEntity.ok(mappedEvents);
    }

    /*
     * 프로젝트별 전체 일정 종합 조회 API
     - /api/calendar/project-events (GET)
     - 특정 프로젝트의 수동 등록 일정(PROJECT 스코프)과 소속된 모든 팀원의 업무(Task) 일정을 통합하여 반환합니다.
     */
    @GetMapping("/project-events")
    public ResponseEntity<List<Map<String, Object>>> getProjectAllEvents(
            @RequestParam("projectId") Long projectId,
            @RequestParam("empNo") String empNo,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end) {
        log.info("=== [GET] 프로젝트 전체 통합 달력 조회 | 프로젝트ID: {} ===", projectId);

        List<Map<String, Object>> mappedEvents = new java.util.ArrayList<>();

        // 1. 달력에 수동으로 등록한 '프로젝트 공유 일정' 가져오기
        try {
            List<CalendarDTO> projectEvents = calendarService.getEventsByScope(empNo, CalendarScope.PROJECT, start, end);
            if (projectEvents != null) {
                for (CalendarDTO dto : projectEvents) {
                    // 요청받은 프로젝트 ID와 일치하는 일정만 필터링
                    if (dto.getProjectId() != null && dto.getProjectId().equals(projectId)) {
                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", "CAL_" + dto.getId());
                        map.put("title", "[공유 일정] " + dto.getTitle());
                        map.put("description", dto.getDescription());
                        map.put("color", dto.getColorId() != null ? dto.getColorId() : "#10b981"); // 에메랄드 초록색

                        String startStr = String.valueOf(dto.getStartsAt());
                        String endStr = String.valueOf(dto.getEndsAt());
                        map.put("start", startStr); map.put("startAt", startStr);
                        map.put("end", endStr); map.put("endAt", endStr);
                        map.put("allDay", dto.isAllDay());

                        mappedEvents.add(map);
                    }
                }
            }
        } catch (Exception e) {
            log.error("프로젝트 공유 일정 로드 실패: ", e);
        }

        // 2. 해당 프로젝트에 소속된 '모든 팀원의 업무 마감일' 가져오기
        try {
            // ProjectMapper를 사용해 이 프로젝트의 모든 태스크를 가져옵니다.
            List<TaskResponseDTO> projectTasks = projectMapper.selectTasksWithAssignee(projectId);

            if (projectTasks != null) {
                for (TaskResponseDTO task : projectTasks) {
                    // 달력이 너무 지저분해지지 않도록 완료된(DONE) 업무는 뺍니다.
                    if ("DONE".equals(task.getStatus())) continue;

                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", "TASK_" + task.getId());

                    // 담당자 이름을 앞에 붙여서 누구의 업무인지 한눈에 보이게 설정
                    String assignee = task.getAssigneeName() != null ? task.getAssigneeName() : "미지정";
                    map.put("title", "[" + assignee + "] " + task.getTitle());
                    map.put("description", task.getDescription());

                    // 우선순위별 색상 지정
                    String flagColor = "#0ca5e9"; // 보통 (파란색)
                    if ("HIGH".equals(task.getPriority())) {
                        flagColor = "#f43f5e"; // 높음 (빨간색)
                    } else if ("LOW".equals(task.getPriority())) {
                        flagColor = "#94a3b8"; // 낮음 (회색)
                    }
                    map.put("color", flagColor);

                    String deadline = String.valueOf(task.getDueOn());
                    map.put("start", deadline); map.put("startAt", deadline);
                    map.put("end", deadline); map.put("endAt", deadline);
                    map.put("allDay", true);

                    mappedEvents.add(map);
                }
            }
        } catch (Exception e) {
            log.error("프로젝트 업무 일정 병합 실패: ", e);
        }

        return ResponseEntity.ok(mappedEvents);
    }


}
