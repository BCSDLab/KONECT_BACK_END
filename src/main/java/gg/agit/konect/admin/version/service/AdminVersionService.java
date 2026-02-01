package gg.agit.konect.admin.version.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.admin.version.dto.AdminVersionCreateRequest;
import gg.agit.konect.domain.version.model.Version;
import gg.agit.konect.domain.version.repository.VersionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVersionService {

    private final VersionRepository versionRepository;

    @Transactional
    public void createVersion(AdminVersionCreateRequest request) {
        Version version = Version.builder()
                .platform(request.platform())
                .version(request.version())
                .releaseNotes(request.releaseNotes())
                .build();

        versionRepository.save(version);
    }
}
