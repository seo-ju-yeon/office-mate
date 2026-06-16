package office_mate_2605.security.exception;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * refreshToken 재발급 과정의 실패를 JSON 응답으로 변환하는 커스텀 예외. (작성자: 서주연)
 *
 * <p>RefreshTokenFilter에서 accessToken/refreshToken 누락, 형식 오류,
 * 서명 오류, 만료, DB 미저장 또는 revoke 상태를 구분해 응답한다.
 * 필터 내부 예외 흐름에서 직접 HTTP 상태 코드와 JSON body를 작성한다.</p>
 */
@Log4j2
public class RefreshTokenException extends RuntimeException {
    // refresh token 재발급 과정에서 발생하는 에러를 다루기 위한 커스텀 예외 클래스

    public RefreshTokenException(ErrorCase errorCase) {
        // RuntimeException의 메시지로 에러 이름을 넣어두면 로그에서 원인을 빠르게 볼 수 있음
        super(errorCase.name());
        this.errorCase = errorCase;
    }

    private final ErrorCase errorCase;

    // 예외 응답(JSON)을 보내는 메서드
    public void sendResponseError(HttpServletResponse response) {
        log.info("--- RefreshTokenException.sendResponseError() 진입 errorCase={} ---", errorCase.name());

        // refresh token 재발급은 Filter에서 처리되므로 일반 ControllerAdvice가 잡지 못하는 경우가 존재함
        // 그래서 예외 객체가 직접 HTTP 상태 코드와 JSON 응답을 만들어 전달

        // 1. HTTP 상태 코드 설정
        // 예: NO_REFRESH는 400, BAD_REFRESH는 401처럼 enum에 정의된 상태 코드를 사용
        response.setStatus(errorCase.getStatus());

        // 2. 응답 타입을 JSON으로 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 3. 클라이언트에게 내려줄 에러 데이터를 Map에 담는다.
        // error: 에러 코드, message: 설명, time: 에러 발생 시각
        Map<String, Object> responseMap = Map.of(
                "error", errorCase.name(),
                "message", errorCase.getMsg(),
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

    public enum ErrorCase {
        // 각 에러는 HTTP 상태 코드와 메시지를 포함
        // accessToken이 요청 body에 없는 경우
        NO_ACCESS(400, "Access token is required"),
        // accessToken 형식이 이상하거나 서명이 맞지 않는 경우
        BAD_ACCESS(403, "Invalid access token"),
        // refreshToken이 요청 body에 없는 경우
        NO_REFRESH(400, "Refresh token is required"),
        // refreshToken 형식 자체가 잘못된 경우
        OLD_REFRESH(403, "Malformed refresh token"),
        // refreshToken 서명이 틀렸거나 만료된 경우
        BAD_REFRESH(401, "Invalid refresh token"),
        // refreshToken이 DB에 없거나 이미 폐기된 경우
        NOT_FOUND_REFRESH(401, "Refresh token is not stored or revoked");

        private final int status;  // HTTP 상태 코드가 들어감
        private final String msg;  // 설명 문구

        // 생성자
        ErrorCase(int status, String msg) {
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
