package office_mate_2605.management.audit_log.dto;

import office_mate_2605.management.audit_log.domain.AuditAction;
import office_mate_2605.management.audit_log.domain.AuditResult;

import java.time.LocalDate;

/**
 * 감사 로그 검색 조건을 전달하는 DTO. (작성자: 서주연)
 *
 * <p>페이지 정보, 감사 행위, 처리 결과, 행위자 사번,
 * 조회 시작일/종료일을 묶어 Repository 조회 조건으로 전달한다.
 * 화면 조회 부하를 줄이기 위해 페이지 번호와 크기 보정 메서드를 제공한다.</p>
 */
public record AuditLogSearchRequestDTO(
        Integer page,  // 조회할 페이지 번호, 0부터 시작
        Integer size,  // 한 페이지에 조회할 감사 로그 개수
        AuditAction action,  // 생성/조회/수정/삭제/로그인/내보내기 등 감사 행위 필터
        AuditResult result,  // 요청 처리 결과 필터. SUCCESS 또는 FAIL
        String actorNo,  // 특정 행위자 사번으로 조회할 때 사용하는 필터
        LocalDate startDate,  // 조회 시작일. 해당 날짜 00:00:00부터 조회
        LocalDate endDate  // 조회 종료일. 해당 날짜 전체를 포함해 조회
) {
    public int normalizedPage() {
        // page는 0부터 시작하며, 음수는 첫 페이지로 보정한다.
        return page == null || page < 0 ? 0 : page;
    }

    public int normalizedSize() {
        // 너무 큰 size 요청을 막아 화면 조회 부하를 제한한다.
        if (size == null || size < 1) {
            return 20;
        }

        return Math.min(size, 100);
    }
}
