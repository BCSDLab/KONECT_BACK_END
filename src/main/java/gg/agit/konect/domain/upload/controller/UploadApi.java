package gg.agit.konect.domain.upload.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import gg.agit.konect.domain.upload.dto.ImageUploadResponse;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Upload: 업로드", description = "업로드 API")
@RequestMapping("/upload")
public interface UploadApi {

    @Operation(summary = "이미지 파일을 업로드한다.", description = """
        서버가 multipart 파일을 받아 S3에 업로드합니다.

        - 응답의 fileUrl을 기존 도메인 API의 imageUrl로 사용합니다.

        ## 에러
        - MISSING_ACCESS_TOKEN (401): 액세스 토큰이 필요합니다.
        - INVALID_REQUEST_BODY (400): 파일이 비어있거나 요청 형식이 올바르지 않은 경우
        - INVALID_FILE_CONTENT_TYPE (400): 지원하지 않는 Content-Type 인 경우
        - INVALID_FILE_SIZE (400): 파일 크기가 제한을 초과한 경우
        - FAILED_UPLOAD_FILE (500): S3 업로드에 실패한 경우
        """)
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ImageUploadResponse> uploadImage(
        @UserId Integer userId,
        @RequestPart("file") MultipartFile file
    );
}
