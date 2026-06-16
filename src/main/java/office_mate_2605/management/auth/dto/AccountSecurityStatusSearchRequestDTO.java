package office_mate_2605.management.auth.dto;

import java.util.Locale;

/**
 * 계정 보안 관리 목록 검색 조건을 전달하는 DTO. (작성자: 서주연)
 *
 * <p>페이지 번호, 페이지 크기, 잠긴 계정만 보기 여부,
 * 사번/이름 검색어를 묶어 Repository 조회 조건으로 전달한다.
 * 화면 조회 부하를 줄이기 위해 페이지 번호와 크기 보정 메서드를 제공한다.</p>
 */
public record AccountSecurityStatusSearchRequestDTO(
        Integer page,  // 조회할 페이지 번호, 0부터 시작
        Integer size,  // 한 페이지에 조회할 계정 보안 상태 개수
        Boolean lockedOnly,  // true이면 잠긴 계정만 조회
        String keyword  // 사번 또는 이름 검색어
) {
    public int normalizedPage() {
        // page는 0부터 시작하며, 음수는 첫 페이지로 보정함
        return page == null || page < 0 ? 0 : page;
    }

    public int normalizedSize() {
        // size가 없거나 1보다 작으면 기본 10건으로 조회함
        if (size == null || size < 1) {
            return 10;
        }

        // 과도한 페이지 크기 요청을 막기 위해 최대 100건으로 제한함
        return Math.min(size, 100);
    }

    public boolean onlyLocked() {
        // Boolean.TRUE일 때만 잠긴 계정 필터를 적용함
        return Boolean.TRUE.equals(lockedOnly);
    }

    public String normalizedKeyword() {
        // 검색어가 없으면 전체 조회를 의미하므로 null 반환함
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        // 사번/이름 검색 비교를 위해 공백 제거 후 대문자로 정규화함
        return keyword.trim().toUpperCase(Locale.ROOT);
    }
}
