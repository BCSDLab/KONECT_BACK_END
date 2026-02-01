package gg.agit.konect.domain.appversion.service;

import gg.agit.konect.domain.appversion.dto.AppVersionResponse;
import gg.agit.konect.domain.appversion.enums.PlatformType;
import gg.agit.konect.domain.appversion.model.AppVersion;
import gg.agit.konect.domain.appversion.repository.AppVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppVersionService {

    private final AppVersionRepository appVersionRepository;

    public AppVersionResponse getLatestVersion(PlatformType platform) {
        AppVersion appVersion = appVersionRepository.getLatestByPlatform(platform);
        return AppVersionResponse.from(appVersion);
    }
}
