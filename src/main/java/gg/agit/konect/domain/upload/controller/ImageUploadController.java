package gg.agit.konect.domain.upload.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gg.agit.konect.domain.upload.dto.ImageUploadResponse;
import gg.agit.konect.domain.upload.service.ImageUploadService;
import gg.agit.konect.global.auth.annotation.UserId;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ImageUploadController implements ImageUploadApi {

    private final ImageUploadService imageUploadService;

    @Override
    public ResponseEntity<ImageUploadResponse> upload(@UserId Integer userId, MultipartFile file) {
        return ResponseEntity.ok(imageUploadService.upload(file));
    }
}
