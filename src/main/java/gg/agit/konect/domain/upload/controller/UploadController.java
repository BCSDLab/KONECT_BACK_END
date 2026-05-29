package gg.agit.konect.domain.upload.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gg.agit.konect.domain.upload.dto.ImageUploadResponse;
import gg.agit.konect.domain.upload.enums.UploadTarget;
import gg.agit.konect.domain.upload.service.UploadService;
import gg.agit.konect.global.ratelimit.annotation.RateLimit;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UploadController implements UploadApi {

    private final UploadService uploadService;

    // 비로그인 공개 업로드는 계정 식별자가 없어 프록시가 복원한 클라이언트 IP 기준으로 시간당 총량을 제한한다.
    @RateLimit(maxRequests = 60, timeWindowSeconds = 3600, keyExpression = "#clientIp")
    @Override
    public ResponseEntity<ImageUploadResponse> uploadImage(
        MultipartFile file,
        UploadTarget target
    ) {
        return ResponseEntity.ok(uploadService.uploadImage(file, target));
    }
}
