package office_mate_2605.management.audit_log.service;

import lombok.RequiredArgsConstructor;
import office_mate_2605.management.audit_log.dto.AuditLogPageResponseDTO;
import office_mate_2605.management.audit_log.dto.AuditLogResponseDTO;
import office_mate_2605.management.audit_log.dto.AuditLogSearchRequestDTO;
import office_mate_2605.management.audit_log.repository.AuditLogQueryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 감사 로그 관리 화면의 조회와 CSV 변환 로직을 처리하는 Service. (작성자: 서주연)
 *
 * <p>검색 조건에 맞는 감사 로그 목록과 페이지 정보를 조립하고,
 * CSV 다운로드 요청 시 조회 결과를 Excel에서 열기 쉬운 UTF-8 BOM 포함 CSV 문자열로 변환한다.</p>
 */
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    // CSV 한 번 다운로드 시 허용할 최대 감사 로그 건수
    private static final int EXPORT_LIMIT = 5000;

    // Excel에서 한글 CSV가 깨지는 것을 줄이기 위한 UTF-8 BOM
    private static final String CSV_BOM = "\uFEFF";

    // audit_log 조회 SQL을 담당하는 Repository
    private final AuditLogQueryRepository auditLogQueryRepository;

    public AuditLogPageResponseDTO search(AuditLogSearchRequestDTO request) {
        // 요청 page, size 값을 화면 조회에 사용할 수 있는 범위로 보정
        int page = request.normalizedPage();
        int size = request.normalizedSize();

        // 현재 검색 조건에 맞는 감사 로그 목록과 전체 건수를 조회
        List<AuditLogResponseDTO> content = auditLogQueryRepository.findAll(request);
        long totalElements = auditLogQueryRepository.count(request);

        // 화면에서 사용할 목록과 페이지 정보를 하나의 응답 DTO로 묶음
        return AuditLogPageResponseDTO.of(content, page, size, totalElements);
    }

    public String exportCsv(AuditLogSearchRequestDTO request) {
        // 과도한 다운로드를 막기 위해 최대 EXPORT_LIMIT건만 조회
        List<AuditLogResponseDTO> logs = auditLogQueryRepository.findForExport(request, EXPORT_LIMIT);

        StringBuilder csv = new StringBuilder(CSV_BOM);
        csv.append("ID,Trace ID,행위자 사번,역할,액션,대상 유형,대상 ID,HTTP Method,요청 URI,IP,결과,사유,발생 시각,DB 적재 시각\n");

        // 조회한 감사 로그를 CSV row로 변환
        for (AuditLogResponseDTO log : logs) {
            csv.append(toCsvLine(log));
        }

        return csv.toString();
    }

    private String toCsvLine(AuditLogResponseDTO log) {
        // DTO 필드 순서를 CSV 헤더 순서와 맞춤
        return String.join(",",
                csvValue(log.id()),
                csvValue(log.traceId()),
                csvValue(log.actorNo()),
                csvValue(log.actorRole()),
                csvValue(log.action()),
                csvValue(log.targetType()),
                csvValue(log.targetId()),
                csvValue(log.httpMethod()),
                csvValue(log.requestUri()),
                csvValue(log.clientIp()),
                csvValue(log.result()),
                csvValue(log.reason()),
                csvValue(log.occurredAt()),
                csvValue(log.flushedAt())
        ) + "\n";
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        String text = String.valueOf(value);

        // 쉼표, 따옴표, 줄바꿈이 포함된 값은 CSV 규칙에 맞게 따옴표로 감쌈
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }

        return text;
    }
}
