package gg.agit.konect.domain.appversion.controller;

import gg.agit.konect.domain.appversion.dto.AppVersionResponse;
import gg.agit.konect.domain.appversion.enums.PlatformType;
import gg.agit.konect.domain.appversion.service.AppVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AppVersionController implements AppVersionApi {

    private final AppVersionService appVersionService;

    @Override
    public ResponseEntity<AppVersionResponse> getLatestVersion(@PathVariable PlatformType platform) {
        AppVersionResponse response = appVersionService.getLatestVersion(platform);
        return ResponseEntity.ok(response);
    }
}
