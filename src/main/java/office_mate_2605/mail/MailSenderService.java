package office_mate_2605.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * 사용자 계정 관련 안내 메일 발송을 처리하는 Service. (작성자: 서주연)
 *
 * <p>비밀번호 찾기 과정에서 발급한 임시 비밀번호와 비밀번호 재설정 링크를
 * HTML 메일 템플릿으로 구성해 사용자 이메일로 발송한다.
 * 임시 비밀번호는 Redis에 짧은 유효 시간으로 저장된 값을 사용자에게 안내하는 용도로 사용한다.</p>
 *
 * <p>메일 발신자 주소와 발신자명은 설정 파일에서 주입받으며,
 * 실제 발송은 JavaMailSender를 사용하는 공통 send 메서드에서 처리한다.</p>
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class MailSenderService {
    private final JavaMailSender mailSender;

    @Value("${myapp.custom.mail.sender.mailFrom}")
    private String mailFrom;

    @Value("${myapp.custom.mail.sender.mailFromName}")
    private String mailFromName;

    // 비밀번호 찾기 전용 메일
    public void sendPasswordResetMail(String to, String tempPassword, String resetUrl)
            throws MessagingException, UnsupportedEncodingException {
        // tempPassword는 PasswordResetService에서 Redis에 10분짜리 BCrypt 해시로 저장된 값의 원문
        // 이 메서드는 사용자에게 임시 비밀번호와 재설정 링크를 메일로 안내
        // 사용자는 이 값을 재설정 화면에 입력하고, 새 비밀번호를 다시 지정해야함

        log.info("--- [sendPasswordResetMail] 발송 시작 | To: {} ---", to);

        String subject = "[OfficeMate] 비밀번호 재설정 임시 비밀번호 안내";
        // 이메일 템플릿 구성
        String body = "<div style='background-color: #f4f5f7; padding: 40px 20px; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif;'>"
                + "  <div style='max-width: 500px; margin: 0 auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(9, 30, 66, 0.1);'>"
                + "    <!-- Header -->"
                + "    <div style='background-color: #0052cc; padding: 30px; text-align: center;'>"
                + "      <h1 style='color: #ffffff; margin: 0; font-size: 24px; font-weight: 800; letter-spacing: -0.5px;'>OfficeMate</h1>"
                + "    </div>"
                + "    <!-- Content -->"
                + "    <div style='padding: 40px 30px;'>"
                + "      <h2 style='color: #172b4d; margin-top: 0; font-size: 20px; font-weight: 700;'>비밀번호 재설정 안내</h2>"
                + "      <p style='color: #42526e; font-size: 15px; line-height: 1.6;'>요청하신 비밀번호 재설정을 위한 임시 비밀번호가 발급되었습니다. 아래의 보안 코드를 확인해 주세요.</p>"
                + "      "
                + "      <div style='margin: 30px 0; padding: 25px; background-color: #f4f5f7; border-radius: 6px; text-align: center; border: 1px dashed #dfe1e6;'>"
                + "        <span style='font-family: \"SF Mono\", \"Courier New\", monospace; font-size: 32px; font-weight: 800; color: #0052cc; letter-spacing: 5px;'>"
                +           tempPassword
                + "        </span>"
                + "      </div>"
                + "      "
                + "      <p style='color: #42526e; font-size: 14px; margin-bottom: 30px;'>아래 버튼을 클릭하여 재설정 페이지로 이동한 후, <b>사번</b>과 위 <b>임시 비밀번호</b>를 사용하여 새 비밀번호를 설정해 주세요.</p>"
                + "      "
                + "      <div style='text-align: center;'>"
                + "        <a href='" + resetUrl + "' style='display: inline-block; padding: 14px 28px; background-color: #0052cc; color: #ffffff; text-decoration: none; border-radius: 4px; font-weight: 700; font-size: 15px;'>비밀번호 재설정하기</a>"
                + "      </div>"
                + "    </div>"
                + "    <!-- Footer -->"
                + "    <div style='padding: 20px 30px; background-color: #fafbfc; border-top: 1px solid #ebecf0;'>"
                + "      <p style='margin: 0; font-size: 12px; color: #6b778c; line-height: 1.5;'>"
                + "        • 본 임시 비밀번호는 발급 후 <span style='color: #de350b; font-weight: 700;'>10분 동안만 유효</span>합니다.<br>"
                + "        • 본인이 요청하지 않은 경우, 이 메일을 무시하시고 고객 지원 센터로 문의해 주세요."
                + "      </p>"
                + "    </div>"
                + "  </div>"
                + "  <div style='text-align: center; margin-top: 20px;'>"
                + "    <p style='font-size: 12px; color: #97a0af;'>&copy; 2026 OfficeMate Project. All rights reserved.</p>"
                + "  </div>"
                + "</div>";

        send(to, subject, body);
//        log.info("--- 임시 발급된 비밀번호: {} ---", tempPassword);
        log.info("--- 임시 비밀번호 메일 발송 완료 | To: {} ---", to);
        log.info("--- [sendPasswordResetMail] 발송 완료 | To: {} ---", to);
    }

    // 공통 메일 발송 헬퍼
    private void send(String to, String subject, String htmlBody)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        message.addRecipients(MimeMessage.RecipientType.TO, to);
        message.setSubject(subject);
        message.setText(htmlBody, "utf-8", "html");
        message.setFrom(new InternetAddress(mailFrom, mailFromName, "UTF-8"));
        mailSender.send(message);
    }
}
