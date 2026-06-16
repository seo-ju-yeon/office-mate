package office_mate_2605.project.service;

import lombok.extern.log4j.Log4j2;
import office_mate_2605.project.dto.ProjectDetailResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@Transactional("pgVectorTxManager")
class ProjectServiceImplTest {
    @Autowired
    private ProjectService projectService;

    @Test
    void getAllProjectsWithDetailsTest() {
        // 1. 서비스 메서드 호출 (내부적으로 forEach 변환 로직 실행)
        List<ProjectDetailResponseDTO> dtoList = projectService.getAllProjectsWithDetails();

        log.info("====================================================");
        log.info("DTO 필드 매핑 결과");
        log.info("====================================================");

        if (dtoList.isEmpty()) {
            log.warn("변환된 DTO 리스트가 비어 있습니다.");
            log.info("체크포인트: DB의 project, employee, departments 테이블 간 JOIN 조건이 맞는지 확인하세요.");
        } else {
            // 루프를 돌며 각 필드에 값이 제대로 박혔는지 찍어봅니다.
            dtoList.forEach(dto -> {
                log.info("프로젝트 ID: {}", dto.getId());
                log.info("   - 프로젝트명: {}", dto.getName());

                // 핵심: 조인으로 가져온 필드들이 null인지 아닌지 로그로 강조
                if (dto.getManagerName() != null) {
                    log.info("   - 담당자 이름 매핑 완료: {}", dto.getManagerName());
                } else {
                    log.error("   - 담당자 이름 매핑 실패 (NULL)");
                }

                if (dto.getDepartmentName() != null) {
                    log.info("   - 부서 이름 매핑 완료: {}", dto.getDepartmentName());
                } else {
                    log.error("   - 부서 이름 매핑 실패 (NULL)");
                }

                log.info("   - 기간: {} ~ {}", dto.getStartsOn(), dto.getEndsOn());
                log.info("   - 진행률: {}%", dto.getProgressRate());
                log.info("----------------------------------------------------");
            });

            log.info("총 {}건의 데이터 변환 검증 완료", dtoList.size());
        }
        log.info("====================================================");
    }
}