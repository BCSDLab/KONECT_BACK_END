package gg.agit.konect.domain.chat.group.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.user.model.User;
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
@Table(name = "group_chat_message")
@NoArgsConstructor(access = PROTECTED)
public class GroupChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private GroupChatRoom room;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Builder
    private GroupChatMessage(
        Integer id,
        GroupChatRoom room,
        User sender,
        String content
    ) {
        this.id = id;
        this.room = room;
        this.sender = sender;
        this.content = content;
    }

    public static GroupChatMessage of(GroupChatRoom room, User sender, String content) {
        return GroupChatMessage.builder()
            .room(room)
            .sender(sender)
            .content(content)
            .build();
    }

    public boolean isSentBy(Integer userId) {
        return sender.getId().equals(userId);
    }
}
