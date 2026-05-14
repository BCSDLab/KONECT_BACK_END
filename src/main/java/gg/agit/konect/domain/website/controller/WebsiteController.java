package gg.agit.konect.domain.website.controller;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.website.dto.WebsiteClubDetailResponse;
import gg.agit.konect.domain.website.dto.WebsiteClubListCondition;
import gg.agit.konect.domain.website.dto.WebsiteClubsResponse;
import gg.agit.konect.domain.website.dto.WebsiteHomeResponse;
import gg.agit.konect.domain.website.service.WebsiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WebsiteController implements WebsiteApi {

    private final WebsiteService websiteService;

    @Override
    public ResponseEntity<WebsiteHomeResponse> getHome(
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "region", required = false) UniversityRegion region
    ) {
        WebsiteHomeResponse response = websiteService.getHome(query, region);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<WebsiteClubsResponse> getUniversityClubs(
        @PathVariable(name = "universityId") Integer universityId,
        @Valid @ParameterObject @ModelAttribute WebsiteClubListCondition condition
    ) {
        WebsiteClubsResponse response = websiteService.getUniversityClubs(universityId, condition);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<WebsiteClubDetailResponse> getClubDetail(
        @PathVariable(name = "clubId") Integer clubId
    ) {
        WebsiteClubDetailResponse response = websiteService.getClubDetail(clubId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<WebsiteClubsResponse> getRecentClubs(
        @RequestParam(name = "clubIds") List<Integer> clubIds
    ) {
        WebsiteClubsResponse response = websiteService.getRecentClubs(clubIds);
        return ResponseEntity.ok(response);
    }
}
