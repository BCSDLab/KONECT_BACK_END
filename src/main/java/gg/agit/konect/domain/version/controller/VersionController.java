package gg.agit.konect.domain.version.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.version.dto.VersionResponse;
import gg.agit.konect.domain.version.enums.PlatformType;
import gg.agit.konect.domain.version.service.VersionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class VersionController implements VersionApi {

    private final VersionService VersionService;

    @Override
    public ResponseEntity<VersionResponse> getLatestVersion(@RequestParam PlatformType platform) {
        VersionResponse response = VersionService.getLatestVersion(platform);
        return ResponseEntity.ok(response);
    }
}
