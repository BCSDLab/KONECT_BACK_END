package gg.agit.konect.domain.version.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.version.dto.VersionResponse;
import gg.agit.konect.domain.version.enums.PlatformType;
import gg.agit.konect.global.auth.annotation.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Version: 버전", description = "버전 API")
@RequestMapping("/versions")
public interface VersionApi {

    @PublicApi
    @Operation(summary = "플랫폼별 최신 앱 버전을 조회한다.")
    @GetMapping("/latest")
    ResponseEntity<VersionResponse> getLatestVersion(@RequestParam PlatformType platform);
}
