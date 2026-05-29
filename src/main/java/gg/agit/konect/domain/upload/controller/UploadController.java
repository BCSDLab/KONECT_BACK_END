package gg.agit.konect.domain.upload.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import gg.agit.konect.domain.upload.dto.ImageUploadResponse;
import gg.agit.konect.domain.upload.enums.UploadTarget;
import gg.agit.konect.domain.upload.service.UploadService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UploadController implements UploadApi {

    private final UploadService uploadService;

    @Override
    public ResponseEntity<ImageUploadResponse> uploadImage(
        MultipartFile file,
        UploadTarget target
    ) {
        return ResponseEntity.ok(uploadService.uploadImage(file, target));
    }
}
