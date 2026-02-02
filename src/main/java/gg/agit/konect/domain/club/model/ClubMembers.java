package gg.agit.konect.domain.club.model;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_PRESIDENT;

import java.util.List;

import gg.agit.konect.global.exception.CustomException;

public record ClubMembers(
    List<ClubMember> members
) {
    public static ClubMembers from(List<ClubMember> members) {
        return new ClubMembers(members);
    }

    public List<ClubMember> getPresidents() {
        return members.stream()
            .filter(ClubMember::isPresident)
            .toList();
    }

    public ClubMember getPresident() {
        return members.stream()
            .filter(ClubMember::isPresident)
            .findFirst()
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_PRESIDENT));
    }

    public int getCount() {
        return members.size();
    }

    public boolean contains(Integer userId) {
        return members.stream()
            .anyMatch(member -> member.isSameUser(userId));
    }
}
