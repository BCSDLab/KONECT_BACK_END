package com.example.konect.club.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import com.example.konect.common.model.BaseEntity;
import com.example.konect.user.model.User;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "club_member")
@NoArgsConstructor(access = PROTECTED)
public class ClubMember extends BaseEntity {

    @EmbeddedId
    private ClubMemberId id;

    @MapsId(value = "clubId")
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @MapsId(value = "userId")
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Builder
    private ClubMember(Club club, User user) {
        this.id = new ClubMemberId(club.getId(), user.getId());
        this.club = club;
        this.user = user;
    }
}
