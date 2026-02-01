package gg.agit.konect.domain.appversion.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.appversion.enums.PlatformType;
import gg.agit.konect.domain.appversion.model.AppVersion;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface AppVersionRepository extends Repository<AppVersion, Integer> {

    Optional<AppVersion> findTopByPlatformOrderByCreatedAtDesc(PlatformType platform);

    AppVersion save(AppVersion appVersion);

    default AppVersion getLatestByPlatform(PlatformType platform) {
        return findTopByPlatformOrderByCreatedAtDesc(platform)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_APP_VERSION));
    }
}
