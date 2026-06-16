package office_mate_2605.calender.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.calender.domain.GoogleCalendarLink;
import office_mate_2605.calender.repository.GoogleCalendarLinkRepository;
import office_mate_2605.project.domain.Project;
import office_mate_2605.project.domain.ProjectTask;
import office_mate_2605.project.repository.ProjectRepository;
import office_mate_2605.project.repository.ProjectTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * 구글 캘린더 연동 관리 Service (작성자: 강수현)

 * <p> OAuth2.0을 기반으로 구글 인증 플로우를 관리하고, 사용자별 액세스 토큰의 저장 및 자동 갱신을 처리합니다.
 * 시스템 내 일정을 구글 캘린더 API 규격으로 변환하여 외부 서버로의 데이터 전송 및 동기화 기능을 제공합니다.</p>
 */


@Service
@Log4j2
@RequiredArgsConstructor
@Transactional
public class GoogleCalendarService {
    private static final String APPLICATION_NAME = "Office Mate";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
    private final GoogleCalendarLinkRepository linkRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectRepository projectRepository;

    // 구글 인증 플로우 생성
    // credentials.json 파일을 로드하여 Google OAuth2 인증을 위한 기본 설정 생성
    private GoogleAuthorizationCodeFlow getFlow() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        InputStream in = GoogleCalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        return new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();
    }

    // 구글 캘린더 서비스 객체 획득
    // DB에 저장된 토큰 정보를 확인하고, 필요 시 갱신하여 API 호출용 서비스 인스턴스 반환
    private Calendar getCalendarService(String empNo) throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = getFlow();

        // DB에서 해당 직원의 연동 정보 조회
        GoogleCalendarLink link = linkRepository.findById(empNo)
                .orElseThrow(() -> new RuntimeException("구글 캘린더 연동이 필요합니다."));

        // DB 정보를 바탕으로 Credential 객체 생성
        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(link.getAccessTokenEncrypted()); // 필요 시 복호화
        tokenResponse.setRefreshToken(link.getRefreshTokenEncrypted()); // 필요 시 복호화
        tokenResponse.setExpiresInSeconds((link.getTokenExpiresAt().toEpochSecond() - OffsetDateTime.now().toEpochSecond()));

        Credential credential = flow.createAndStoreCredential(tokenResponse, empNo);

        // 토큰 만료 시 자동으로 Refresh Token을 사용하여 갱신하고 DB에 저장하는 인터셉터 추가 가능
        if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() <= 60) {
            credential.refreshToken();
            // 갱신된 토큰 DB 업데이트
            link.updateToken(credential.getAccessToken(), OffsetDateTime.now().plusSeconds(credential.getExpiresInSeconds()));
            linkRepository.save(link);
        }

        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    // 구글 인증 URL 생성
    // 사용자가 구글 로그인 및 권한 승인을 진행할 수 있는 최초 연동 페이지 주소 생성
    public String getAuthorizationUrl(String empNo) throws IOException, GeneralSecurityException {
        return getFlow().newAuthorizationUrl()
                .setRedirectUri("http://localhost:8080/api/calendar/callback") // 유지
                .setState(empNo)
                .set("prompt", "consent")
                .build();
    }

    // OAuth 인증 토큰 DB 저장
    // 구글로부터 받은 인증 코드를 토큰으로 교환하여 사용자별 연동 정보를 DB에 영구 저장
    @Transactional
    public void storeTokenInDb(String code, String empNo) throws IOException, GeneralSecurityException {
        GoogleAuthorizationCodeFlow flow = getFlow();

        TokenResponse response = flow.newTokenRequest(code)
                .setRedirectUri("http://localhost:8080/api/calendar/callback") // 유지
                .execute();

        // DB 엔티티 생성 및 저장
        GoogleCalendarLink link = GoogleCalendarLink.builder()
                .employeeNo(empNo)
                .googleCalendarId("primary") // 기본 캘린더 사용
                .accessTokenEncrypted(response.getAccessToken()) // 필요 시 암호화
                .refreshTokenEncrypted(response.getRefreshToken()) // 필요 시 암호화
                .tokenExpiresAt(OffsetDateTime.now().plusSeconds(response.getExpiresInSeconds()))
                .build();

        linkRepository.save(link);
    }

    // 구글 캘린더 일정 삽입
    // 시스템의 일정을 구글 캘린더 API 규격에 맞춰 변환한 뒤 실제 구글 서버에 등록
    public Event insertEvent(String empNo, String summary, String description,
                             OffsetDateTime startAt, OffsetDateTime endAt) throws IOException, GeneralSecurityException {

        Calendar service = getCalendarService(empNo);

        Event event = new Event()
                .setSummary(summary)
                .setDescription(description);

        // OffsetDateTime -> 구글 DateTime 변환
        DateTime startDateTime = new DateTime(startAt.toInstant().toEpochMilli());
        DateTime endDateTime = new DateTime(endAt.toInstant().toEpochMilli());

        event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone("Asia/Seoul"));
        event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone("Asia/Seoul"));

        return service.events().insert("primary", event).execute();
    }

    // 구글 캘린더 일정 삽입
    // 프로젝트 업무 일정을 구글 캘린더로 일괄 내보내기 비즈니스 로직
    @Transactional(readOnly = true)
    public void exportProjectTasksToGoogle(String empNo, Long projectId) throws Exception {
        log.info("=== [Service] 프로젝트 업무 구글 내보내기 실행 ===");

        // 루프 돌기 전, 프로젝트 이름을 한 번만 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 프로젝트입니다."));
        String projectName = project.getName();

        List<ProjectTask> tasks = projectTaskRepository.findByProjectId(projectId);
        if (tasks == null || tasks.isEmpty()) return;

        for (ProjectTask task : tasks) {
            // 미리 조회해둔 프로젝트 이름과 업무명 조합
            String summary = "[" + projectName + "] " + task.getTitle();

            String description = task.getDescription() != null ? task.getDescription() : "오피스메이트 프로젝트 업무";

            OffsetDateTime startAt = task.getDueOn() != null
                    ? task.getDueOn().atStartOfDay().atOffset(ZoneOffset.of("+09:00"))
                    : OffsetDateTime.now();
            OffsetDateTime endAt = startAt.plusHours(1);

            try {
                insertEvent(empNo, summary, description, startAt, endAt);
            } catch (Exception e) {
                log.error("등록 실패: {}", task.getTitle());
            }
        }
    }
}
