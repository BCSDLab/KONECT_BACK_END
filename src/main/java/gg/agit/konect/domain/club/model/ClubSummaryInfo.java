package gg.agit.konect.domain.club.model;

import java.util.List;

import gg.agit.konect.domain.club.enums.RecruitmentStatus;

public record ClubSummaryInfo(
    Integer id,
    String name,
    String imageUrl,
    String categoryName,
    String description,
    RecruitmentStatus status,
    List<String> tags
) {

}
