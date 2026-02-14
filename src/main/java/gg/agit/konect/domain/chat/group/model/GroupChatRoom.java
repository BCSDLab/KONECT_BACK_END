package gg.agit.konect.domain.chat.group.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "group_chat_room")
@NoArgsConstructor(access = PROTECTED)
public class GroupChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, unique = true)
    private Club club;

    @Builder
    private GroupChatRoom(Integer id, Club club) {
        this.id = id;
        this.club = club;
    }

    public static GroupChatRoom of(Club club) {
        return GroupChatRoom.builder()
            .club(club)
            .build();
    }
}
