package office_mate_2605.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.UnsupportedEncodingException;

@SpringBootTest
class MailSenderServiceTest {

    @Autowired
    private MailSenderService mailSenderService;

    @Test
    void sendPasswordResetMail() throws MessagingException, UnsupportedEncodingException {
        // 실제로 메일을 받을 수 있는 이메일 주소를 입력한다.
        String to = "wndus6110@naver.com";

        // 테스트용 임시 비밀번호
        String tempPassword = "Test1234!";

        // 메일 본문에 들어갈 비밀번호 재설정 페이지 주소
        String resetUrl = "http://localhost:8080/password-reset";

        // 실제 SMTP 서버를 통해 메일을 발송한다.
        mailSenderService.sendPasswordResetMail(to, tempPassword, resetUrl);
    }

}