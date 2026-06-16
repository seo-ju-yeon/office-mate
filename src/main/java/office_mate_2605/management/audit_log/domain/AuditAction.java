package office_mate_2605.management.audit_log.domain;

/**
 * 감사 로그에 기록할 사용자 행위 유형을 정의하는 Enum. (작성자: 서주연)
 *
 * <p>생성, 조회, 수정, 삭제, 로그인, 로그아웃, 권한 변경, 상태 변경,
 * 내보내기 같은 감사 대상 행위를 PostgreSQL audit_action type과 맞춰 기록한다.</p>
 */
public enum AuditAction {
    CREATE, // 데이터 생성
    READ, // 데이터 조회
    UPDATE, // 데이터 수정
    DELETE, // 데이터 삭제 또는 논리 삭제
    LOGIN, // 로그인 성공
    LOGIN_FAIL, // 로그인 실패
    LOGOUT, // 로그아웃
    PERMISSION_CHANGE, // 권한 또는 정책 변경
    STATUS_CHANGE, // 휴직/복직/퇴사 등 계정 또는 재직 상태 변경
    EXPORT // 파일 다운로드 또는 데이터 내보내기
}
