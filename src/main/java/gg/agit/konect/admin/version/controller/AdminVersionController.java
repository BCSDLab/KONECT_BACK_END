package gg.agit.konect.admin.version.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.admin.version.dto.AdminVersionCreateRequest;
import gg.agit.konect.admin.version.service.AdminVersionService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.auth.annotation.Auth;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/versions")
@Auth(roles = {UserRole.ADMIN})
public class AdminVersionController implements AdminVersionApi {

    private final AdminVersionService adminVersionService;

    @Override
    public ResponseEntity<Void> createVersion(@Valid @RequestBody AdminVersionCreateRequest request) {
        adminVersionService.createVersion(request);
        return ResponseEntity.ok().build();
    }
}
