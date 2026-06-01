package gg.agit.konect.admin.website.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportConfirmRequest;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportPreviewResponse;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportRequest;
import gg.agit.konect.admin.website.dto.AdminWebsiteClubSheetImportResponse;
import gg.agit.konect.admin.website.service.AdminWebsiteClubSheetImportService;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.auth.annotation.Auth;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/konect/universities")
@Auth(roles = {UserRole.ADMIN})
public class AdminWebsiteClubSheetImportController implements AdminWebsiteClubSheetImportApi {

    private final AdminWebsiteClubSheetImportService adminWebsiteClubSheetImportService;

    @Override
    public ResponseEntity<AdminWebsiteClubSheetImportPreviewResponse> previewClubs(
        Integer universityId,
        AdminWebsiteClubSheetImportRequest request
    ) {
        AdminWebsiteClubSheetImportPreviewResponse response =
            adminWebsiteClubSheetImportService.previewClubs(universityId, request.spreadsheetUrl());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<AdminWebsiteClubSheetImportResponse> confirmImport(
        Integer universityId,
        AdminWebsiteClubSheetImportConfirmRequest request
    ) {
        AdminWebsiteClubSheetImportResponse response =
            adminWebsiteClubSheetImportService.confirmImport(universityId, request.clubs());
        return ResponseEntity.ok(response);
    }
}
