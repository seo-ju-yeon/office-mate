package office_mate_2605.chat.service;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.chat.dto.DraftDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
class DraftServiceTest {
    @Autowired
    private DraftService draftService;

    @Test
    void addDraft() {
        DraftDTO draftDTO = DraftDTO.builder()
                .content("test message")
                .build();

        draftService.addDraft("test-session-id", draftDTO);
    }

    @Test
    void getDraft() {
        DraftDTO draftDTO = draftService.getDraft("test-session-id");
        log.info("draftDTO: {}", draftDTO);
        assertNotNull(draftDTO);
    }

    @Test
    void removeDraft() {
        draftService.removeDraft("test-session-id");
        DraftDTO draftDTO = draftService.getDraft("test-session-id");
        assertNull(draftDTO);
    }
}