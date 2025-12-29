package gg.agit.konect.domain.club.model;

import java.util.ArrayList;
import java.util.List;

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

    public int getCount() {
        return members.size();
    }

    public boolean contains(Integer userId) {
        return members.stream()
            .anyMatch(member -> member.isSameUser(userId));
    }

    @Override
    public List<ClubMember> members() {
        return new ArrayList<>(members);
    }
}
