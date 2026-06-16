package office_mate_2605.management.audit_log.controller;

import lombok.RequiredArgsConstructor;
import office_mate_2605.management.audit_log.domain.AuditAction;
import office_mate_2605.management.audit_log.domain.AuditResult;
import office_mate_2605.management.audit_log.dto.AuditLogPageResponseDTO;
import office_mate_2605.management.audit_log.dto.AuditLogSearchRequestDTO;
import office_mate_2605.management.audit_log.service.AuditLogQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * 감사 로그 관리 화면의 조회와 CSV 다운로드 API를 처리하는 Controller. (작성자: 서주연)
 *
 * <p>SUPER 권한 사용자가 감사 로그 목록을 조건별로 조회하고,
 * 현재 필터 조건에 맞는 감사 로그를 CSV 파일로 내려받을 수 있도록 요청/응답을 담당한다.</p>
 */
@RestController
@RequestMapping("/api/management/audit-logs")
@RequiredArgsConstructor
public class AuditLogRestController {
    // 감사 로그 관리 화면에서 사용하는 조회 API Controller

    // audit_log 조회 조건 처리와 페이지 응답 생성을 담당하는 서비스
    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER')")
    // 화면 필터 조건을 검색 요청 DTO로 묶어 Service에 전달
    public ResponseEntity<AuditLogPageResponseDTO> searchAuditLogs(
            // 조회할 페이지
            @RequestParam(required = false) Integer page,
            // 한 페이지에 가져올 개수
            @RequestParam(required = false) Integer size,
            // 감사 행위 필터
            @RequestParam(required = false) AuditAction action,
            // 처리 결과 필터 (success, fail)
            @RequestParam(required = false) AuditResult result,
            // 행위자 사번
            @RequestParam(required = false) String actorNo,
            // 조회 시작일
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            // 조회 종료일
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        AuditLogSearchRequestDTO request = new AuditLogSearchRequestDTO(
                page,
                size,
                action,
                result,
                actorNo,
                startDate,
                endDate
        );

        return ResponseEntity.ok(auditLogQueryService.search(request));
    }

    // 현재 필터 조건으로 감사 로그 조회 -> CSV 문자열 생성 -> 브라우저 다운로드 응답 반환
    @GetMapping(value = "/export.csv", produces = "text/csv; charset=UTF-8")
    @PreAuthorize("hasRole('SUPER')")
    public ResponseEntity<String> exportAuditLogsCsv(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditResult result,
            @RequestParam(required = false) String actorNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // CSV 다운로드는 페이지가 아니라 현재 필터 조건 전체를 기준으로 조회
        AuditLogSearchRequestDTO request = new AuditLogSearchRequestDTO(
                null,
                null,
                action,
                result,
                actorNo,
                startDate,
                endDate
        );

        // Service에서 CSV 문자열을 생성
        String csv = auditLogQueryService.exportCsv(request);

        // 브라우저가 파일 다운로드로 인식하도록 응답 헤더를 설정
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
