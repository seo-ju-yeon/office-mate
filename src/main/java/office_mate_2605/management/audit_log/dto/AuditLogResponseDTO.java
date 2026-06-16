package office_mate_2605.management.audit_log.dto;

import office_mate_2605.management.audit_log.domain.AuditAction;
import office_mate_2605.management.audit_log.domain.AuditResult;

import java.time.OffsetDateTime;

/**
 * 감사 로그 단건 조회 결과를 화면에 전달하는 DTO. (작성자: 서주연)
 *
 * <p>audit_log 테이블의 한 행을 관리 화면에서 표시하기 위한 형태로 변환하며,
 * 행위자, 대상, HTTP 요청 정보, 처리 결과, 발생/적재 시각을 포함한다.</p>
 */
public record AuditLogResponseDTO(

        Long id,  // 감사 로그 PK
        String traceId,  // 하나의 API 요청을 추적하기 위한 고유 ID
        String actorNo,  // 행위를 수행한 직원 사번
        String actorRole,  // 요청 당시 행위자의 시스템 권한
        AuditAction action,  // 생성/조회/수정/삭제/로그인/내보내기 같은 감사 행위
        String targetType,  // 감사 대상 리소스 유형. 예: EMPLOYEE, AUTH, AUDIT_LOG
        String targetId,  // 감사 대상 데이터 식별자. 예: 직원 사번, 게시글 ID
        String httpMethod,  // 요청 HTTP 메서드. 예: GET, POST, PATCH, DELETE
        String requestUri,  // 요청 URI
        String clientIp,  // 요청을 보낸 클라이언트 IP
        String userAgent,  // 요청 브라우저 또는 클라이언트 정보
        AuditResult result,  // 요청 처리 결과. SUCCESS 또는 FAIL
        String reason,  // 실패 사유 또는 감사에 필요한 부가 설명
        OffsetDateTime occurredAt,  // 실제 API 요청이 발생한 시각
        OffsetDateTime flushedAt  // Redis 큐에서 audit_log DB로 이관된 시각
) {
}
