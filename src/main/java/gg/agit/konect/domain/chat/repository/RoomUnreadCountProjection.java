package gg.agit.konect.domain.chat.repository;

public interface RoomUnreadCountProjection {

    Integer getRoomId();

    Long getUnreadCount();
}
