package gg.agit.konect.admin.appversion.controller;

import gg.agit.konect.admin.appversion.dto.AdminAppVersionCreateRequest;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.auth.annotation.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "(Admin) AppVersion: 앱 버전", description = "어드민 앱 버전 API")
@RequestMapping("/admin/app-versions")
@Auth(roles = {UserRole.ADMIN})
public interface AdminAppVersionApi {

    @Operation(summary = "새 앱 버전을 등록한다.")
    @PostMapping
    ResponseEntity<Void> createVersion(
        @Valid @RequestBody AdminAppVersionCreateRequest request
    );
}
