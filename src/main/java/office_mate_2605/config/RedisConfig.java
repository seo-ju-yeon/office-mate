package office_mate_2605.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 연결과 RedisTemplate 직렬화 설정을 담당하는 Config. (작성자: 공통)
 *
 * <p>application.properties에 정의된 Redis host, port 값을 사용해 Lettuce 기반
 * RedisConnectionFactory를 생성한다. 인증 토큰, 임시 데이터, 캐시성 데이터 등
 * Redis를 사용하는 기능에서 공통으로 사용할 연결 설정을 제공한다.</p>
 *
 * <p>Redis key는 문자열로 저장하고 value는 Jackson JSON 직렬화를 사용해 저장하도록
 * RedisTemplate을 구성한다. 이를 통해 별도 Serializable 구현 없이 객체 데이터를 Redis에 저장할 수 있다.</p>
 */
@Configuration
public class RedisConfig {
    // application.properties에 정의한 Redis 서버 정보를 주입받음.
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        /* Spring Data Redis가 실제 Redis 서버와 통신 할 때 사용할 연결 팩토리 */
        // Boot 3.x 기본 클라이언트인 Lettuce 기반으로생성
        return new LettuceConnectionFactory(host, port);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        // key 는 사람이 읽기 쉬운 문자열로 저장하고,value 는 JSON 으로 저장.
        // 이렇게 설정하면 DTO가 Serializable 을 구현하지 않아도 Redis 에 저장할 수 있음.

        // 1. ObjectMapper 설정
        // Redis value 를 JSON 으로 직렬화/역직렬화할 때사용할 ObjectMapper.
        ObjectMapper objectMapper = new ObjectMapper();
        // ObjectMapper 기반 JSON serializer를RedisTemplate value serializer로 사용.
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 2. RedisTemplate 설정
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        // Redis key 는 일반 문자열 형태로 저장.
        template.setKeySerializer(new StringRedisSerializer());
        // Redis value 는 JSON 문자열 기반으로 저장.
        template.setValueSerializer(serializer);
        // template 설정 적용 후 반환.
        template.afterPropertiesSet();
        return template;
    }
}
