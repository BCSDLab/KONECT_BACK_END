package gg.agit.konect.domain.appversion.controller;

import gg.agit.konect.domain.appversion.dto.AppVersionResponse;
import gg.agit.konect.domain.appversion.enums.PlatformType;
import gg.agit.konect.global.auth.annotation.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "(Normal) AppVersion: 앱 버전", description = "앱 버전 API")
@RequestMapping("/app-versions")
public interface AppVersionApi {

    @PublicApi
    @Operation(summary = "플랫폼별 최신 앱 버전을 조회한다.")
    @GetMapping("/{platform}")
    ResponseEntity<AppVersionResponse> getLatestVersion(@PathVariable PlatformType platform);
}
