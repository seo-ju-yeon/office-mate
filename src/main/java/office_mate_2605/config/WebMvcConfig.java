package office_mate_2605.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 정적 리소스 설정 ( 작성자 : 서민성 )
 * <p>업로드 파일과 JS 파일을 정적 리소스로 서빙하기 위한 URL 경로 매핑을 담당함.</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // application.properties의 업로드 디렉토리 경로 주입
    @Value("${my.upload.path}")
    private String uploadPath;

    /* 정적 리소스 핸들러 등록 - URL 경로와 실제 파일 시스템 경로를 매핑함 */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /upload/** 요청 → uploadPath 디렉토리의 실제 파일로 매핑 (첨부파일 서빙)
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath + "/");

        // /js/** 요청 → classpath:/static/js/ 디렉토리로 매핑 (JS 파일 서빙)
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
    }
}