package office_mate_2605.chat.controller;

import lombok.RequiredArgsConstructor;
import office_mate_2605.chat.dto.AiSummaryRequestDTO;
import office_mate_2605.chat.service.AiSummaryService;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI REST 컨트롤러 (작성자: 박재경)
 * 채팅 메시지 요약 요청을 받아 AI 요약 서비스를 호출하는 컨트롤러 클래스다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {
    private final AiSummaryService aiSummaryService;

    /* 선택한 채팅 메시지를 AI로 요약한다. */
    @PostMapping("/summary/{roomId}")
    public ResponseEntity<?> summarize(
            @PathVariable Long roomId,
            @RequestBody AiSummaryRequestDTO requestDTO,
            @AuthenticationPrincipal EmployeePrincipal principal
    ) {
        // 인증 사용자가 없으면 요약 요청을 거부한다.
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            // 채팅방 번호와 사용자 사번을 기준으로 선택 메시지 요약을 수행한다.
            return ResponseEntity.ok(aiSummaryService.summarize(
                    roomId,
                    principal.getEmployeeNo(),
                    requestDTO.getMessageIds()
            ));
        } catch (IllegalArgumentException e) {
            // 선택 메시지가 없거나 유효하지 않은 요청이면 오류 메시지를 반환한다.
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
