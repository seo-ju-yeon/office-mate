package office_mate_2605.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * 첨부파일 업로드 유틸리티 ( 작성자 : 서민성 )
 * <p>게시글 첨부파일의 서버 저장·삭제를 처리하며,
 * 허용된 확장자 검증 및 UUID 기반 고유 파일명 생성을 담당함.</p>
 */
@Log4j2
@Component
public class FileUploadUtil {

    @Value("${my.upload.path}")
    private String uploadPath;

    // 허용 확장자 목록
    private static final List<String> ALLOWED_EXTENSIONS =
            List.of(".pdf", ".docx", ".xlsx", ".pptx", ".png", ".jpg", ".jpeg", ".csv");

    /*
     * 첨부파일을 서버에 저장하고 접근 가능한 URL 경로를 반환함.
     *
     * @param file 업로드된 MultipartFile
     * @return 저장된 파일의 URL 경로 (예: /upload/uuid_filename.pdf)
     * @throws IllegalArgumentException 허용되지 않는 확장자일 경우
     */
    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            log.debug("업로드 파일 없음 - null 반환");
            return null;
        }

        // 확장자 추출 및 허용 여부 검증
        String originalName = file.getOriginalFilename();
        String ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "허용되지 않는 파일 형식입니다: " + ext +
                            " (허용: pdf, docx, xlsx, pptx, png, jpg, jpeg, csv)");
        }

        // 저장 디렉토리가 없으면 생성
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
            log.debug("업로드 디렉토리 생성: {}", uploadPath);
        }

        // UUID + 원본 파일명으로 고유 파일명 생성 후 저장
        String savedName = UUID.randomUUID() + "_" + originalName;
        File savedFile = new File(uploadPath, savedName);
        file.transferTo(savedFile);
        log.debug("파일 저장 완료: {}", savedFile.getAbsolutePath());

        // DB에 저장할 접근 URL 경로 반환
        return "/upload/" + savedName;
    }

    /* 저장된 파일 삭제 - storedPath에서 파일명을 추출해 실제 파일을 제거함 */
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isEmpty()) return;

        // /upload/ prefix 제거 후 실제 파일 경로로 변환
        String fileName = storedPath.replace("/upload/", "");
        File file = new File(uploadPath, fileName);
        if (file.exists()) {
            file.delete();
            log.debug("파일 삭제 완료: {}", file.getAbsolutePath());
        }
    }
}