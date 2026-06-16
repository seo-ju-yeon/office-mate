package office_mate_2605.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 문서 그룹과 JWT 인증 스키마를 설정하는 Config. (작성자: 공통)
 *
 * <p>/api/** 경로는 REST API 그룹으로, 그 외 화면 이동용 Controller 경로는 COMMON API 그룹으로 분리해
 * Swagger UI에서 API 문서를 구분해서 확인할 수 있도록 설정한다.</p>
 *
 * <p>JWT Bearer 토큰 인증 방식을 OpenAPI 보안 스키마로 등록해,
 * Swagger UI에서 Authorization 헤더를 포함한 API 테스트를 할 수 있게 한다.</p>
 */
@Configuration
public class SwaggerConfig {
    // 경로에 /api가 포함된 컨트롤러의 경우에는 REST API로 인식
    @Bean
    public GroupedOpenApi restApi() {
        return GroupedOpenApi.builder()
                .pathsToMatch("/api/**")
                .group("REST API")
                .build();
    }

    @Bean
    public GroupedOpenApi commonApi() {
        // 경로에 /api가 포함안된 컨트롤러의 경우에는 COMMON API로 인식
        return GroupedOpenApi.builder()
                .pathsToMatch("/**/*")
                .pathsToExclude("/api/**/*") // 제외할 경로
                .group("COMMON API")
                .build();
    }

    // Swagger UI는 'Authorization'과 같이 보안과 관련된 헤더를 추가
    @Bean
    public OpenAPI customOpenAPI() {
        // import할 때 스프링이 아닌 Swagger 관련 API (io.swagger.v3.oas.models)를 이용해야 하므로 주의가 필요

        // Security Scheme 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)  // HTTP 타입
                .scheme("bearer")  // Bearer 토큰
                .bearerFormat("JWT"); // JWT 형식

        // 보안 요구사항 추가
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearerAuth");

        return new OpenAPI()
                .info(new Info()
                        .title("API Documentation")
                        .version("1.0")
                        .description("API documentation with JWT security"))
                .addSecurityItem(securityRequirement) // 보안 요구사항 추가
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", securityScheme));
    }
}
