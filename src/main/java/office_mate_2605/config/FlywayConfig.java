package office_mate_2605.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {
    // initMethod = "migrate"
    // 애플리케이션 시작 시 각 DB에서 자기 폴더의 마이그레이션만 실행

    @Bean(name = "mainFlyway", initMethod = "migrate")
    public Flyway mainFlyway(
            @Qualifier("pgVectorDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/main")
                .load();
    }

    @Bean(name = "auditFlyway", initMethod = "migrate")
    public Flyway auditFlyway(
            @Qualifier("auditDataSource") DataSource dataSource
    ) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/audit")
                .load();
    }
}
