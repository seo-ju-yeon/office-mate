package office_mate_2605.security.exception;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * accessToken 검증 과정의 실패를 JSON 응답으로 변환하는 커스텀 예외. (작성자: 서주연)
 *
 * <p>TokenCheckFilter에서 Authorization 헤더 누락, 토큰 형식 오류,
 * 서명 오류, 만료, 임시 비밀번호 상태, 비활성 계정, 잠금 계정 같은 상황을 표현한다.
 * Filter 예외는 ControllerAdvice가 잡기 어렵기 때문에 예외 객체가 직접 응답을 작성한다.</p>
 */
@Log4j2
public class AccessTokenException extends RuntimeException {
    // JWT 처리 중 발생할 수 있는 에러를 다루기 위한 커스텀 예외 클래스

    public AccessTokenException(TOKEN_ERROR tokenError) {
        // RuntimeException의 메시지로 에러 이름을 넣어두면 로그에서 원인을 빠르게 볼 수 있음
        super(tokenError.name());
        this.tokenError = tokenError;
    }

    private final TOKEN_ERROR tokenError;

    // 예외 응답(JSON)을 보내는 메서드
    public void sendResponseError(HttpServletResponse response) {
        log.info("--- AccessTokenException.sendResponseError() 진입 tokenError={} ---", tokenError.name());

        // Filter 안에서 예외가 발생하면 일반 ControllerAdvice가 잡지 못하는 경우가 존재함
        // 그래서 예외 객체가 직접 HTTP 상태 코드와 JSON 응답을 만들어 전달

        // 1. HTTP 상태 코드 설정
        // 예: EXPIRED는 401, MALFORM은 403처럼 enum에 정의된 상태 코드를 사용
        response.setStatus(tokenError.getStatus());

        // 2. 응답 타입을 JSON으로 설정
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");

        // 3. 클라이언트에게 내려줄 에러 데이터를 Map에 담는다.
        // error: 에러 코드, message: 설명, time: 에러 발생 시각
        Map<String, Object> responseMap = Map.of(
                "error", tokenError.name(),
                "message", tokenError.getMsg(),
                "time", new Date()
        );

        // 4. Java Map 객체를 JSON 문자열로 변환한다.
        Gson gson = new Gson();
        String responseStr = gson.toJson(responseMap);

        // 5. JSON 문자열을 실제 HTTP 응답 body에 작성
        try {
            response.getWriter().println(responseStr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public enum TOKEN_ERROR {
        // 각 에러는 HTTP 상태 코드와 메시지를 포함
        // 토큰을 담은 Authorization 헤더가 없는 경우
        UNACCEPT(401, "Authorization header is required"),
        // Authorization 헤더가 Bearer 타입이 아닌 경우
        BADTYPE(401, "Bearer token type is required"),
        // 토큰 형식 자체가 잘못됨
        MALFORM(403, "Malformed access token"),
        // 서명이 위조됨
        BADSIGN(403, "Invalid access token signature"),
        // 토큰이 만료됨
        EXPIRED(401, "Expired access token"),
        // 블랙리스트 처리된 토큰
        BLACKLISTED(401, "Blacklisted access token"),
        // 임시 비밀번호 상태라서 비밀번호 변경이 필요한 경우
        TEMP_PASSWORD_REQUIRED(423, "Temporary password must be changed"),
        // 휴직/퇴사 등으로 계정 상태가 바뀌어 기존 토큰을 더 이상 사용할 수 없는 경우
        ACCOUNT_INACTIVE(423, "계정 상태가 변경되어 로그아웃됩니다."),
        // 로그인 실패 5회 이상으로 계정이 잠긴 경우
        ACCOUNT_LOCKED(423, "로그인 실패 5회 이상으로 계정이 잠겼습니다. 관리자에게 문의해주세요.");

        private final int status;  // HTTP 상태 코드가 들어감
        private final String msg;  // 설명 문구

        // 생성자
        TOKEN_ERROR(int status, String msg) {
            this.status = status;
            this.msg = msg;
        }

        // Getter
        public int getStatus() {
            return this.status;
        }

        // Getter
        public String getMsg() {
            return this.msg;
        }
    }
}
