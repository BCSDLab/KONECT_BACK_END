package gg.agit.konect.domain.website.model;

import gg.agit.konect.domain.university.enums.UniversityRegion;

public record WebsiteUniversitySummary(
    Integer id,
    String name,
    String campusName,
    UniversityRegion region,
    String regionName,
    Long clubCount
) {
}
