package gg.agit.konect.domain.club.model;

import java.time.LocalDate;

import gg.agit.konect.domain.club.enums.RecruitmentStatus;

public record ClubSummaryInfo(
    Integer id,
    String name,
    String imageUrl,
    String categoryName,
    String description,
    RecruitmentStatus status,
    Boolean isAlwaysRecruiting,
    LocalDate applicationDeadline
) {

}
