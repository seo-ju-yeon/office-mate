package office_mate_2605.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 페이지 요청 DTO ( 작성자 : 서민성 )
 * <p>목록 조회 시 페이지 번호·사이즈·검색 조건을 담으며,
 * MyBatis offset 계산 및 쿼리 파라미터 링크 생성 기능을 제공함.</p>
 */
@Log4j2
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {

    @Builder.Default
    private int page = 1; // 페이지 번호 (기본값 1)

    @Builder.Default
    private int size = 10; // 한 페이지당 게시글 수 (기본값 10)

    private String type;    // 검색 종류. 예: t(제목), w(작성자), tc(제목+내용)
    private String keyword; // 검색 키워드

    private String link;    // 캐싱된 쿼리 파라미터 링크 문자열
    private Long projectId; // 프로젝트 공지 탭 필터용 (null = 전체)

    /* 검색 종류 문자열을 한 글자씩 분리하여 배열로 반환 */
    public String[] getTypes() {
        if (this.type == null || this.type.isEmpty()) {
            return null;
        }
        log.info(this.type);
        return this.type.split("");
    }

    /* JPA Pageable 객체 생성 - 전달받은 정렬 기준으로 내림차순 정렬함 */
    public Pageable getPageAble(String prop) {
        return PageRequest.of(this.page - 1, this.size, Sort.by(prop).descending());
    }

    /* 쿼리 파라미터 링크 문자열 생성 - 최초 호출 시 생성 후 캐싱함 */
    public String getLink() {
        if (this.link == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("page=").append(this.page);
            stringBuilder.append("&size=").append(this.size);

            // type, keyword가 있을 경우에만 파라미터 추가
            if (this.type != null && !this.type.isEmpty()) {
                stringBuilder.append("&type=").append(this.type);
            }
            if (this.keyword != null && !this.keyword.isEmpty()) {
                stringBuilder.append("&keyword=").append(URLEncoder.encode(keyword, StandardCharsets.UTF_8));
            }
            if (this.projectId != null) {
                stringBuilder.append("&projectId=").append(this.projectId);
            }
            this.link = stringBuilder.toString();
        }
        return this.link;
    }

    /* MyBatis 쿼리용 offset 계산 - (page - 1) * size */
    public int getOffset() {
        return (this.page - 1) * this.size;
    }
}