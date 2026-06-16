package office_mate_2605.common.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OfficeMate 화면 이동 요청을 처리하는 Controller.
 *
 * <p>로그인, 대시보드, 마이페이지, 직원 관리, 조직도, 감사 로그,
 * 채팅창 등 브라우저에서 접근하는 주요 URL을 Thymeleaf 템플릿으로 연결한다.</p>
 *
 * <p>HTML 화면 자체는 열 수 있게 두고, 실제 JWT 인증과 권한 검증은
 * 화면 로드 후 JavaScript의 API 호출 및 서버 인증 필터에서 처리한다.</p>
 */
@Log4j2
@Controller
@RequiredArgsConstructor
public class MainController {

    private static final String LOGIN_REQUIRED_ROLES = "USER,ADMIN,SUPER";

    @GetMapping("/")
    public String root() {
        log.info("--- 루트 경로 접속, 인트로 페이지로 이동 ---");
        return "intro/landing";
    }

    @GetMapping("/login")
    public String login() {
        log.info("--- 로그인 화면 접속 ---");
        return "login/login";
    }

    @GetMapping("/password-find")
    public String passwordFind() {
        log.info("--- 비밀번호 찾기 화면 접속 ---");
        return "login/password-find";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("--- 대시보드 화면 접속 ---");
        addLoginRequiredPageRoles(model);
        return "dashboard/dashboard";
    }

    @GetMapping("/password-change")
    public String passwordChange(Model model) {
        log.info("--- 비밀번호 변경 화면 접속 ---");
        addLoginRequiredPageRoles(model);
        return "auth/password-change";
    }

    @GetMapping("/password-reset")
    public String passwordReset() {
        log.info("--- 비밀번호 재설정 화면 접속 ---");
        return "auth/password-reset";
    }

    @GetMapping("/mypage")
    public String mypage(Model model) {
        log.info("--- 마이페이지 화면 접속 ---");
        addLoginRequiredPageRoles(model);
        return "mypage/mypage";
    }

    @GetMapping("/employees/register")
    public String employeeRegister(Model model) {
        log.info("--- 직원 등록 화면 접속 ---");
        // 직접 URL 접근 방어를 위해 레이아웃에 허용 role 정보를 전달함
        model.addAttribute("pageRoles", "ADMIN,SUPER");
        return "employee/register";
    }

    @GetMapping("/organization-chart")
    public String organizationChart(Model model) {
        log.info("--- 조직도 화면 접속 ---");
        addLoginRequiredPageRoles(model);
        return "employee/organization-chart";
    }

    @GetMapping("/account-security/manage")
    public String accountSecurityManage(Model model) {
        log.info("--- 계정 보안 관리 화면 접속 ---");
        // 직접 URL 접근 방어를 위해 레이아웃에 허용 role 정보를 전달함
        model.addAttribute("pageRoles", "ADMIN,SUPER");
        return "auth/account-security-manage";
    }

    @GetMapping("/audit-logs/manage")
    public String auditLogManage(Model model) {
        log.info("--- 감사 로그 관리 화면 접속 ---");
        // 직접 URL 접근 방어를 위해 레이아웃에 허용 role 정보를 전달함
        model.addAttribute("pageRoles", "SUPER");
        return "auth/audit-log-manage";
    }

    @GetMapping("/status-requests/manage")
    public String statusRequestManage(Model model) {
        log.info("--- 재직 상태 변경 신청 관리 화면 접속 ---");
        // 직접 URL 접근 방어를 위해 레이아웃에 허용 role 정보를 전달함
        model.addAttribute("pageRoles", "SUPER");
        return "employee/status-request-manage";
    }
    @GetMapping("/calendar")
    public String calendar(Model model) {
        log.info("--- 캘린더 메인 화면 접속 ---");
        addLoginRequiredPageRoles(model);
        return "calendar/calendar";
    }

    @GetMapping("/projects")
    public String projectDashboard(Model model) {
        log.info("--- 프로젝트 대시보드 화면 접속 ---");
        // 직접 URL 접근 방어를 위해 레이아웃에 허용 role 정보를 전달함
        model.addAttribute("pageRoles", "ADMIN,SUPER");
        return "project/project";
    }

    @GetMapping("/api/chat/room")
    public String chatRoom(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String targetNo,
            Model model
    ) {
        log.info("--- 채팅창 화면 접속");

        model.addAttribute("roomId", roomId);
        model.addAttribute("targetNo", targetNo);
        addLoginRequiredPageRoles(model);

        return "chat/chat";
    }

    private void addLoginRequiredPageRoles(Model model) {
        // 로그인한 모든 시스템 권한이 접근 가능한 화면임을 레이아웃에 전달함
        model.addAttribute("pageRoles", LOGIN_REQUIRED_ROLES);
    }
}
