package gg.agit.konect.domain.chat.model;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
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
@Table(name = "chat_room")
@NoArgsConstructor(access = PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "last_message_content", length = 1000)
    private String lastMessageContent;

    @Column(name = "last_message_sent_at")
    private LocalDateTime lastMessageSentAt;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @Builder
    private ChatRoom(Integer id, Club club) {
        this.id = id;
        this.club = club;
    }

    public static ChatRoom of(User sender, User receiver) {
        validateIsNotSameParticipant(sender, receiver);
        return ChatRoom.builder().build();
    }

    public static ChatRoom groupOf(Club club) {
        return ChatRoom.builder()
            .club(club)
            .build();
    }

    public static void validateIsNotSameParticipant(User sender, User receiver) {
        if (sender.getId().equals(receiver.getId())) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }
    }

    public boolean isDirectRoom() {
        return club == null;
    }

    public boolean isGroupRoom() {
        return club != null;
    }

    public void updateLastMessage(String lastMessageContent, LocalDateTime lastMessageSentAt) {
        this.lastMessageContent = lastMessageContent;
        this.lastMessageSentAt = lastMessageSentAt;
    }
}
