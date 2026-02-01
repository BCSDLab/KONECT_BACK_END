package gg.agit.konect.admin.appversion.controller;

import gg.agit.konect.admin.appversion.dto.AdminAppVersionCreateRequest;
import gg.agit.konect.admin.appversion.service.AdminAppVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/app-versions")
public class AdminAppVersionController implements AdminAppVersionApi {

    private final AdminAppVersionService adminAppVersionService;

    @Override
    public ResponseEntity<Void> createVersion(@Valid @RequestBody AdminAppVersionCreateRequest request) {
        adminAppVersionService.createVersion(request);
        return ResponseEntity.ok().build();
    }
}
