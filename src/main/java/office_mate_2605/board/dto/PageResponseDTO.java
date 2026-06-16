package office_mate_2605.board.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * 페이지 응답 DTO ( 작성자 : 서민성 )
 * <p>목록 조회 결과와 페이지네이션 정보(시작·끝 페이지, 이전·다음 여부)를
 * 함께 담아 뷰에 전달함.</p>
 */
@Getter
@ToString
public class PageResponseDTO<E> {

    private int page;   // 현재 페이지 번호
    private int size;   // 한 페이지당 게시글 수
    private int total;  // 전체 게시글 수
    private int start;  // 페이지 블록 시작 번호
    private int end;    // 페이지 블록 끝 번호
    private boolean prev; // 이전 페이지 블록 존재 여부
    private boolean next; // 다음 페이지 블록 존재 여부
    private List<E> dtoList; // 목록 데이터

    /* 페이지 응답 생성자 - 요청 DTO·전체 개수·목록 데이터를 받아 페이지네이션 정보를 계산함 */
    @Builder(builderMethodName = "withAll")
    public PageResponseDTO(PageRequestDTO pageRequestDTO, int total, List<E> dtoList) {
        this.page = pageRequestDTO.getPage();
        this.size = pageRequestDTO.getSize();
        this.total = total;
        this.dtoList = dtoList;

        // 현재 페이지 기준으로 5개 단위 페이지 블록의 끝·시작 번호 계산
        this.end = (int) (Math.ceil(this.page / 5.0) * 5);
        this.start = this.end - 4;

        // end는 마지막 페이지 번호를 초과할 수 없음
        int last = (int) (Math.ceil((total / (double) size)));
        this.end = Math.min(end, last);

        // 이전 블록 존재 여부: start가 1보다 크면 이전 블록 있음
        this.prev = this.start > 1;

        // 다음 블록 존재 여부: 전체 게시글이 현재 블록 끝 페이지 * size 초과 시 다음 블록 있음
        this.next = total > this.end * this.size;
    }
}