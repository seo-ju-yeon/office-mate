package office_mate_2605.management.audit_log.dto;

import java.util.List;

/**
 * 감사 로그 목록 조회의 페이지 응답 데이터를 전달하는 DTO. (작성자: 서주연)
 *
 * <p>현재 페이지의 감사 로그 목록, 페이지 번호, 페이지 크기,
 * 전체 건수, 전체 페이지 수를 함께 내려 관리 화면의 페이지네이션에 사용한다.</p>
 */
public record AuditLogPageResponseDTO(
        List<AuditLogResponseDTO> content,  // 현재 페이지에 표시할 감사 로그 목록
        int page,  // 현재 페이지 번호, 0부터 시작
        int size,  // 한 페이지에 조회한 감사 로그 개수
        long totalElements,  // 현재 검색 조건에 해당하는 전체 감사 로그 건수
        int totalPages  // 전체 페이지 수
) {
    public static AuditLogPageResponseDTO of(List<AuditLogResponseDTO> content,
                                             int page,
                                             int size,
                                             long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new AuditLogPageResponseDTO(content, page, size, totalElements, totalPages);
    }
}
