package gg.agit.konect.domain.version.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.version.dto.VersionResponse;
import gg.agit.konect.domain.version.enums.PlatformType;
import gg.agit.konect.domain.version.model.Version;
import gg.agit.konect.domain.version.repository.VersionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VersionService {

    private final VersionRepository versionRepository;

    public VersionResponse getLatestVersion(PlatformType platform) {
        Version version = versionRepository.getLatestByPlatform(platform);
        return VersionResponse.from(version);
    }
}
