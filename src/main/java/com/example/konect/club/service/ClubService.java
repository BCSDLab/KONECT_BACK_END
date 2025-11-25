package com.example.konect.club.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.konect.club.dto.ClubsResponse;
import com.example.konect.club.model.Club;
import com.example.konect.club.model.ClubTag;
import com.example.konect.club.model.ClubTagMap;
import com.example.konect.club.repository.ClubRepository;
import com.example.konect.club.repository.ClubTagMapRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubTagMapRepository clubTagMapRepository;

    public ClubsResponse getClubs(Integer page, Integer limit, String query) {
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<Club> clubPage = StringUtils.hasText(query) ?
            clubRepository.findAllByQuery(query.trim(), pageable) : clubRepository.findAll(pageable);

        List<Integer> clubIds = clubPage.getContent().stream()
            .map(Club::getId)
            .toList();

        List<ClubTagMap> clubTagMaps = clubTagMapRepository.findByClubIdIn(clubIds);
        Map<Integer, List<ClubTag>> clubTagsMap = clubTagMaps.stream()
            .collect(Collectors.groupingBy(
                clubTagMap -> clubTagMap.getClub().getId(),
                Collectors.mapping(ClubTagMap::getTag, Collectors.toList())
            ));

        return ClubsResponse.of(clubPage, clubTagsMap);
    }
}
