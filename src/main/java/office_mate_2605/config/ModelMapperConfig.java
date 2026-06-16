package office_mate_2605.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DTO와 Entity 간 객체 변환을 위한 ModelMapper Bean을 등록하는 Config. (작성자: 공통)
 *
 * <p>프로젝트 전역에서 ModelMapper를 주입받아 사용할 수 있도록 Spring Bean으로 등록한다.
 * private 필드 매칭을 허용하고, 필드명이 완전히 일치하지 않아도 유연하게 매핑되도록
 * LOOSE 전략을 적용한다.</p>
 *
 * <p>게시판, 캘린더, 채팅, 직원 관리 등 여러 모듈에서 공통으로 사용하는 변환 설정이다.</p>
 */
@Configuration
public class ModelMapperConfig {
    @Bean
    public ModelMapper getModelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.LOOSE);  // 다대일 처리를 위해 LOOSE 설정
        return modelMapper;
    }
}
