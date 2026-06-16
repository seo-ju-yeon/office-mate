package office_mate_2605.mail;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 비밀번호 재설정 메일 발송에 사용할 JavaMailSender 설정을 담당하는 Config. (작성자: 서주연)
 *
 * <p>application.properties에 정의된 SMTP host, port, 계정, 비밀번호,
 * SSL socketFactory 설정을 읽어 JavaMailSender Bean을 생성한다.
 * 메일 본문 한글과 HTML 템플릿이 깨지지 않도록 기본 인코딩은 UTF-8로 지정한다.</p>
 *
 * <p>비밀번호 재설정 임시 비밀번호 안내 메일처럼 서버에서 직접 발송하는 메일 기능에서 공통으로 사용하는 설정이다.</p>
 */
@Log4j2
@Configuration
public class MailSenderConfig {
    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.port}")
    private int socketPort;

    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean starttls;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean starttlsRequired;

    @Value("${spring.mail.properties.mail.smtp.socketFactory.fallback}")
    private boolean fallback;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.host}")
    private String host;

    @Bean
    public JavaMailSender getJavaMailSender() {
        log.info("--- [MailSenderConfig] JavaMailSender Bean 생성 시작 ---");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        log.info("메일 서버 호스트: {}", host);
        log.info("메일 서버 포트: {}", port);
        log.info("인증 계정: {}", username);
        // password는 보안을 고려하여 로드 '여부'로 log 기록
        log.info("비밀번호 로드 여부: {}", (password != null && !password.isEmpty()));

        mailSender.setJavaMailProperties(getProperties());
        mailSender.setDefaultEncoding("UTF-8");
        log.info("--- [MailSenderConfig] JavaMailSender Bean 설정 완료 ---");
        return mailSender;
    }

    private Properties getProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.ssl.enable", true);  // 해당 코드로 아래의 주석 2줄을 대체
//        props.put("mail.smtp.starttls.enable", starttls);
//        props.put("mail.smtp.starttls.required", starttlsRequired);
        props.put("mail.smtp.socketFactory.fallback", fallback);
        props.put("mail.smtp.socketFactory.port", socketPort);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        return props;
    }
}
