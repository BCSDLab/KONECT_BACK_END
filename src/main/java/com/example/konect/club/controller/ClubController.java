package com.example.konect.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.konect.club.dto.ClubDetailResponse;
import com.example.konect.club.dto.ClubsResponse;
import com.example.konect.club.service.ClubService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubController implements ClubApi {

    private final ClubService clubService;

    @GetMapping
    public ResponseEntity<ClubsResponse> getClubs(
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @RequestParam(name = "limit", defaultValue = "10", required = false) Integer limit,
        @RequestParam(name = "query", defaultValue = "", required = false) String query,
        @RequestParam(name = "isRecruiting", defaultValue = "false", required = false) Boolean isRecruiting
    ) {
        ClubsResponse response = clubService.getClubs(page, limit, query, isRecruiting);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{clubId}")
    public ResponseEntity<ClubDetailResponse> getClubDetail(
        @PathVariable(name = "clubId") Integer clubId
    ) {
        ClubDetailResponse response = clubService.getClubDetail(clubId);
        return ResponseEntity.ok(response);
    }
}
