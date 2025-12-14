package gg.agit.konect.domain.club.model;

import java.util.List;

public record ClubSummaryInfo(
    Integer id,
    String name,
    String imageUrl,
    String categoryName,
    String description,
    List<String> tags
) {

}
