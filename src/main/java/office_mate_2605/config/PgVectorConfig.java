package office_mate_2605.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * PostgreSQL pgvector 메인 데이터베이스 연결 설정을 담당하는 Config. (작성자: 공통)
 *
 * <p>spring.datasource.pgvector 설정 값을 기반으로 메인 DataSource,
 * JdbcTemplate, DataSourceTransactionManager를 Spring Bean으로 등록한다.
 * auditDataSource 같은 다른 DataSource와 구분하기 위해 pgVectorDataSource를 Primary Bean으로 지정한다.</p>
 *
 * <p>JPA 기반 CRUD는 JpaTransactionManager를 기본 transactionManager로 사용하고,
 * JdbcTemplate이나 직접 SQL 처리는 pgVectorTxManager를 사용할 수 있도록 분리한다.</p>
 */
@Configuration
public class PgVectorConfig {
    @Bean
    @Primary  // auditDataSource와 구분하기 위해 메인 DB DataSource를 기본 Bean으로 지정
    @ConfigurationProperties("spring.datasource.pgvector")
    public DataSourceProperties pgVectorDataSourceProperties() {
        // 데이터 소스 프로퍼티 빈 생성
        // -> PostgresSQL 전용 DB 연결 정보를 가져오기 위해 별도의 프로퍼티 설정 URL, 계정, 비밀번호 등을 저장
        return new DataSourceProperties();
    }

    @Bean(name = "pgVectorDataSource")
    @Primary  // auditDataSource와 구분하기 위해 메인 DB DataSource를 기본 Bean으로 지정
    public DataSource pgVectorDataSource() {
        // 데이터베이스 연결 객체 생성 -> pgVectorDataSourceProperties에서 가져온 설정으로 PostgresSQL 연결 객체 생성
        return pgVectorDataSourceProperties()
                .initializeDataSourceBuilder()
                .build();
    }

    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        // SQL 실행을 쉽게 해주는 Spring JDBC 객체 생성 -> pgVectorDataSource를 사용하는 JdbcTemplate 생성
        return new JdbcTemplate(pgVectorDataSource);
    }

    @Bean(name = "pgVectorTxManager")
    public PlatformTransactionManager pgVectorTxManager(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        // 트랜잭션 관리자 생성 -> pgVectorDataSource를 사용하는 트랜잭션 관리자 생성
        return new DataSourceTransactionManager(pgVectorDataSource);
    }

    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager jpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        // JPA CRUD: transactionManager → JpaTransactionManager
        // JdbcTemplate/MyBatis/Batch 직접 SQL: pgVectorTxManager → DataSourceTransactionManager
        return new JpaTransactionManager(entityManagerFactory);
    }

//    @Bean
//    public VectorStore vectorStore(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate, EmbeddingModel embeddingModel) {
//        /* 벡터 스토어 빈 생성 -> pgVectorJdbcTemplate과 임베딩 모델을 사용하여 PgVectorStore 생성 */
//        // dimensions : 임베딩 벡터의 차원 수 (OpenAI text-embedding-3-small 기준 1536)
//        // distanceType : 벡터 간 유사도 계산 방식 (코사인 거리)
//        // initializeSchema : true로 설정하면 테이블과 인덱스를 자동으로 생성
//        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
//                .dimensions(1536)  // OpenAI text-embedding-3-small 기준
//                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
//                .initializeSchema(true)  // 테이블, 인덱스 자동 생성
//                .build();
//    }
}
