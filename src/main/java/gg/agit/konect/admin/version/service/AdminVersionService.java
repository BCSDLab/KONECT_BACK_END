package gg.agit.konect.admin.version.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.admin.version.dto.AdminVersionCreateRequest;
import gg.agit.konect.domain.version.model.Version;
import gg.agit.konect.domain.version.repository.VersionRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminVersionService {

    private final VersionRepository versionRepository;

    @Transactional
    public void createVersion(AdminVersionCreateRequest request) {
        try {
            Version version = Version.builder()
                .platform(request.platform())
                .version(request.version())
                .releaseNotes(request.releaseNotes())
                .build();

            versionRepository.save(version);
        } catch (DataIntegrityViolationException e) {
            throw CustomException.of(ApiResponseCode.DUPLICATE_VERSION);
        }
    }
}
