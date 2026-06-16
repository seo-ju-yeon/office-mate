package office_mate_2605.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import office_mate_2605.chat.dto.DraftDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** 채팅 임시저장 서비스 (작성자: 박재경)
 * Redis를 사용해 사용자가 입력 중인 채팅 메시지를 임시 저장하는 서비스 클래스다.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class DraftService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String PREFIX = "draftContent:";
    private static final Duration TTL = Duration.ofHours(1);

    /* 메시지 임시저장 내용을 Redis에 저장한다. */
    public void addDraft(String sessionId, DraftDTO draftDTO) {
        // 방 번호와 사용자 사번으로 구성된 키를 만든다.
        String key = PREFIX + sessionId;

        // 임시저장 데이터는 지정된 만료 시간과 함께 저장한다.
        redisTemplate.opsForValue().set(key, draftDTO, TTL);
    }

    /* Redis에 저장된 임시 메시지를 조회한다. */
    public DraftDTO getDraft(String sessionId) {
        // 방 번호와 사용자 사번으로 구성된 키를 만든다.
        String key = PREFIX + sessionId;
        Object value = redisTemplate.opsForValue().get(key);

        // 저장된 값이 없으면 null을 반환한다.
        if (value == null) {
            return null;
        }

        // Redis 조회 값을 DraftDTO로 변환한다.
        return objectMapper.convertValue(value, DraftDTO.class);
    }

    /* Redis에 저장된 임시 메시지를 삭제한다. */
    public void removeDraft(String sessionId) {
        // 방 번호와 사용자 사번으로 구성된 키를 만든다.
        String key = PREFIX + sessionId;
        redisTemplate.delete(key);
    }
}
