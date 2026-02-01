package gg.agit.konect.admin.version.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.admin.version.dto.AdminVersionCreateRequest;
import gg.agit.konect.admin.version.service.AdminVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/versions")
public class AdminVersionController implements AdminVersionApi {

    private final AdminVersionService adminVersionService;

    @Override
    public ResponseEntity<Void> createVersion(@Valid @RequestBody AdminVersionCreateRequest request) {
        adminVersionService.createVersion(request);
        return ResponseEntity.ok().build();
    }
}
