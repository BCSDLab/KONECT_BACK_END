package gg.agit.konect.admin.appversion.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.admin.appversion.dto.AdminAppVersionCreateRequest;
import gg.agit.konect.domain.appversion.model.AppVersion;
import gg.agit.konect.domain.appversion.repository.AppVersionRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAppVersionService {

    private final AppVersionRepository appVersionRepository;

    @Transactional
    public void createVersion(AdminAppVersionCreateRequest request) {
        AppVersion appVersion = AppVersion.builder()
                .platform(request.platform())
                .version(request.version())
                .releaseNotes(request.releaseNotes())
                .build();

        appVersionRepository.save(appVersion);
    }
}
