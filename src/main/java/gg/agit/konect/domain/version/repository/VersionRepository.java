package gg.agit.konect.domain.version.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.version.enums.PlatformType;
import gg.agit.konect.domain.version.model.Version;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface VersionRepository extends Repository<Version, Integer> {

    Optional<Version> findTopByPlatformOrderByCreatedAtDesc(PlatformType platform);

    Version save(Version version);

    default Version getLatestByPlatform(PlatformType platform) {
        return findTopByPlatformOrderByCreatedAtDesc(platform)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_VERSION));
    }
}
