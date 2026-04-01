package gg.agit.konect.domain.notification.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.notification.enums.NotificationInboxType;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "notification_inbox")
@NoArgsConstructor(access = PROTECTED)
public class NotificationInbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationInboxType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "body", nullable = false, length = 300)
    private String body;

    @Column(name = "path", length = 200)
    private String path;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Builder
    private NotificationInbox(User user, NotificationInboxType type, String title, String body, String path) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.body = body;
        this.path = path;
        this.isRead = false;
    }

    public static NotificationInbox of(
        User user,
        NotificationInboxType type,
        String title,
        String body,
        String path
    ) {
        return NotificationInbox.builder()
            .user(user)
            .type(type)
            .title(title)
            .body(body)
            .path(path)
            .build();
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
