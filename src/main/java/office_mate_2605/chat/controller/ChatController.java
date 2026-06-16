package office_mate_2605.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.chat.dto.ChatMessageDTO;
import office_mate_2605.chat.dto.ChatRoomDetailDTO;
import office_mate_2605.chat.dto.DraftDTO;
import office_mate_2605.chat.service.ChatService;
import office_mate_2605.chat.service.DraftService;
import office_mate_2605.security.dto.EmployeePrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 채팅 REST 컨트롤러 (작성자: 박재경)
 * 채팅방 조회, 생성, 메시지 내역 조회, 임시저장 기능을 제공하는 컨트롤러 클래스다.
 */
@Log4j2
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final DraftService draftService;

    /* 로그인 사용자가 참여 중인 채팅방 목록을 조회한다. */
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDetailDTO>> getMyRooms(@AuthenticationPrincipal EmployeePrincipal principal) {
        // 인증 정보가 없으면 채팅방 목록 접근을 차단한다.
        if (principal == null) {
            log.warn("인증 정보가 없습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 로그인 사용자의 사번으로 참여 중인 채팅방을 조회한다.
        List<ChatRoomDetailDTO> rooms = chatService.findAllRoomsByEmployeeNo(principal.getEmployeeNo());

        // 프론트엔드 반복 처리 오류를 막기 위해 빈 리스트를 보장한다.
        return ResponseEntity.ok(rooms != null ? rooms : new ArrayList<>());
    }

    /* 채팅방 초기 진입에 필요한 방 정보와 최근 메시지를 조회한다. */
    @GetMapping("/room/init")
    public ResponseEntity<?> getChatInitData(
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) String targetNo,
            @AuthenticationPrincipal EmployeePrincipal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // 방 번호나 상대 사번이 모두 없으면 초기화할 대상을 알 수 없으므로 요청을 거부한다.
        if (roomId == null && (targetNo == null || targetNo.trim().isEmpty())) {
            log.warn("채팅 초기화 실패: roomId와 targetNo가 모두 누락되었습니다.");
            return ResponseEntity.badRequest().body("채팅방 ID 또는 상대방 사번이 필요합니다.");
        }

        Map<String, Object> response = new HashMap<>();
        String userNo = principal.getEmployeeNo();

        try {
            // 1:1 채팅 진입이면 기존 방을 찾고 없을 때 새 방을 생성한다.
            if (roomId == null && targetNo != null && !targetNo.trim().isEmpty()) {
                roomId = chatService.getOrCreateRoom(userNo, targetNo);
            }

            if (roomId != null) {
                ChatRoomDetailDTO detail = chatService.findRoomDetailById(roomId);
                // 해당 방 정보가 없으면 존재하지 않는 채팅방으로 응답한다.
                if (detail == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("존재하지 않는 채팅방입니다.");
                }

                List<ChatMessageDTO> history = chatService.getChatHistoryWithOffset(roomId, userNo,30, 0);

                String roomName = detail.getRoomName();

                // 1:1 채팅의 방 이름이 비어 있으면 상대 이름으로 표시명을 만든다.
                if ((roomName == null || roomName.isEmpty()) && targetNo != null) {
                    String targetName = chatService.findEmployeeName(targetNo);
                    roomName = targetName + "님과의 대화";
                }

                response.put("roomId", roomId);
                response.put("roomName", roomName);
                response.put("chatHistory", history != null ? history : new ArrayList<>());

                // Redis에 저장된 임시 메시지가 있으면 초기 응답에 포함한다.
                try {
                    String draftKey = roomId + ":" + userNo;
                    DraftDTO draft = draftService.getDraft(draftKey);
                    if (draft != null && draft.getContent() != null) {
                        response.put("draft", draft.getContent());
                    }
                } catch (Exception e) {
                    log.warn("임시저장 메시지 로드 실패", e);
                }

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("채팅 초기화 중 서버 오류 발생: ", e);
            return ResponseEntity.status(500).body("서버 내부 오류가 발생했습니다.");
        }

        return ResponseEntity.badRequest().build();
    }

    /* 채팅방 번호로 채팅방 상세 정보를 조회한다. */
    @GetMapping("/room/detail/{roomId}")
    public ResponseEntity<ChatRoomDetailDTO> getRoomDetail(@PathVariable Long roomId) {
        ChatRoomDetailDTO detail = chatService.findRoomDetailById(roomId);
        return ResponseEntity.ok(detail);
    }

    /* 채팅방의 기본 메시지 내역을 조회한다. */
    @GetMapping("/history/{roomId}")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @PathVariable Long roomId,
            @AuthenticationPrincipal EmployeePrincipal principal) {
        // 로그인 사용자의 사번을 기준으로 읽음 수가 반영된 히스토리를 조회한다.
        List<ChatMessageDTO> history = chatService.getChatHistory(roomId, principal.getEmployeeNo());
        return ResponseEntity.ok(history);
    }

    /* 스크롤 페이징용 메시지 목록을 조회한다. */
    @GetMapping("/messages")
    @ResponseBody
    public List<ChatMessageDTO> getMessages(
            @RequestParam Long roomId,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @AuthenticationPrincipal EmployeePrincipal principal) {
        if (principal == null) {
            log.warn("인증되지 않은 사용자의 메시지 조회 시도");
            return new ArrayList<>();
        }

        // 요청 파라미터 대신 인증된 사용자의 사번을 기준으로 메시지를 조회한다.
        return chatService.getChatHistoryWithOffset(roomId, principal.getEmployeeNo(), limit, offset);
    }

    /* 선택한 직원들로 그룹 채팅방을 생성한다. */
    @PostMapping("/room/group")
    @ResponseBody
    public ResponseEntity<?> createGroupChat(
            @RequestBody Map<String, Object> params,
            @AuthenticationPrincipal EmployeePrincipal principal) {

        // 요청 본문에서 방 이름과 참여자 사번 목록을 추출한다.
        String roomName = (String) params.get("roomName");
        List<String> employeeNos = (List<String>) params.get("employeeNos");

        // 생성 요청자 본인이 참여자 목록에 없으면 자동으로 포함한다.
        if (employeeNos == null) employeeNos = new ArrayList<>();
        if (!employeeNos.contains(principal.getEmployeeNo())) {
            employeeNos.add(principal.getEmployeeNo());
        }

        // 채팅방을 만들고 생성된 방 번호를 반환한다.
        Long roomId = chatService.createGroupChatRoom(roomName, employeeNos);

        Map<String, Object> response = new HashMap<>();
        response.put("roomId", roomId);
        return ResponseEntity.ok(response);
    }

    /* 입력 중인 메시지를 Redis에 임시 저장한다. */
    @PostMapping("/draft")
    @ResponseBody
    public void saveDraft(
            @RequestBody DraftDTO draftDTO,
            @RequestParam Long roomId,
            @AuthenticationPrincipal EmployeePrincipal principal) {
        // 방 번호와 사용자 사번을 조합해 임시저장 키를 만든다.
        String draftKey = roomId + ":" + principal.getEmployeeNo();
        draftService.addDraft(draftKey, draftDTO);
    }
}
