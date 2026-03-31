package gg.agit.konect.domain.chat.model;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private ChatType roomType;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    @Builder
    private ChatRoom(Integer id, ChatType roomType, Club club) {
        this.id = id;
        this.roomType = roomType;
        this.club = club;
    }

    public static ChatRoom directOf() {
        return ChatRoom.builder()
            .roomType(ChatType.DIRECT)
            .build();
    }

    public static ChatRoom clubGroupOf(Club club) {
        return ChatRoom.builder()
            .roomType(ChatType.CLUB_GROUP)
            .club(club)
            .build();
    }

    public static ChatRoom groupOf() {
        return ChatRoom.builder()
            .roomType(ChatType.GROUP)
            .build();
    }

    public static void validateIsNotSameParticipant(User sender, User receiver) {
        if (sender.getId().equals(receiver.getId())) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }
    }

    public boolean isDirectRoom() {
        return roomType == ChatType.DIRECT;
    }

    public boolean isGroupRoom() {
        return roomType == ChatType.GROUP || roomType == ChatType.CLUB_GROUP;
    }

    public boolean isClubGroupRoom() {
        return roomType == ChatType.CLUB_GROUP;
    }

    public void updateLastMessage(String lastMessageContent, LocalDateTime lastMessageSentAt) {
        this.lastMessageContent = lastMessageContent;
        this.lastMessageSentAt = lastMessageSentAt;
    }
}
