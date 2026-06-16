package office_mate_2605.management.audit_log.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 감사 로그 전용 DataSource와 JdbcTemplate을 구성하는 설정 클래스. (작성자: 서주연)
 *
 * <p>메인 업무 DB와 분리된 office_mate_audit_log DB 연결 정보를 생성하고,
 * 감사 로그 저장/조회에 사용할 전용 JdbcTemplate과 트랜잭션 매니저 Bean을 등록한다.</p>
 */
@Configuration
public class AuditDataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.audit")
    public DataSourceProperties auditDataSourceProperties() {
        // audit 전용 DB 연결 정보를 application.properties에서 읽어온다.
        return new DataSourceProperties();
    }

    @Bean(name = "auditDataSource")
    public DataSource auditDataSource() {
        // office_mate_audit_log DB에 연결할 DataSource를 생성한다.
        return auditDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "auditJdbcTemplate")
    public JdbcTemplate auditJdbcTemplate(@Qualifier("auditDataSource") DataSource auditDataSource) {
        // audit DB에 SQL을 실행하기 위한 전용 JdbcTemplate
        return new JdbcTemplate(auditDataSource);
    }

    @Bean(name = "auditTxManager")
    public PlatformTransactionManager auditTxManager(@Qualifier("auditDataSource") DataSource auditDataSource) {
        // 추후 Batch에서 audit DB insert 트랜잭션을 관리할 때 사용
        return new DataSourceTransactionManager(auditDataSource);
    }
}
