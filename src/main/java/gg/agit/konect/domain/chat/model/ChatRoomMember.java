package gg.agit.konect.domain.chat.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
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
@Table(name = "chat_room_member")
@NoArgsConstructor(access = PROTECTED)
public class ChatRoomMember extends BaseEntity {

    @EmbeddedId
    private ChatRoomMemberId id;

    @ManyToOne(fetch = LAZY)
    @MapsId("chatRoomId")
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    @Builder
    private ChatRoomMember(
        ChatRoomMemberId id,
        ChatRoom chatRoom,
        User user,
        LocalDateTime lastReadAt
    ) {
        this.id = id;
        this.chatRoom = chatRoom;
        this.user = user;
        this.lastReadAt = lastReadAt;
    }

    public static ChatRoomMember of(ChatRoom chatRoom, User user, LocalDateTime lastReadAt) {
        return ChatRoomMember.builder()
            .id(new ChatRoomMemberId(chatRoom.getId(), user.getId()))
            .chatRoom(chatRoom)
            .user(user)
            .lastReadAt(lastReadAt)
            .build();
    }

    public Integer getChatRoomId() {
        return id.getChatRoomId();
    }

    public Integer getUserId() {
        return id.getUserId();
    }

    public void updateLastReadAt(LocalDateTime lastReadAt) {
        if (this.lastReadAt.isBefore(lastReadAt)) {
            this.lastReadAt = lastReadAt;
        }
    }
}
