package gg.agit.konect.admin.version.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.admin.version.dto.AdminVersionCreateRequest;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.auth.annotation.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Admin) Version: 버전", description = "어드민 버전 API")
@RequestMapping("/admin/versions")
@Auth(roles = {UserRole.ADMIN})
public interface AdminVersionApi {

    @Operation(summary = "새 버전을 등록한다.")
    @PostMapping
    ResponseEntity<Void> createVersion(
        @Valid @RequestBody AdminVersionCreateRequest request
    );
}
