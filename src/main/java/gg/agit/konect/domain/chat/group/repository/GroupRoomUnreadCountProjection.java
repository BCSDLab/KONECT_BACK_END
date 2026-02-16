package gg.agit.konect.domain.chat.group.repository;

public interface GroupRoomUnreadCountProjection {

    Integer getRoomId();

    Long getUnreadCount();
}
