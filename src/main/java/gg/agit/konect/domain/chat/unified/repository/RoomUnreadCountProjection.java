package gg.agit.konect.domain.chat.unified.repository;

public interface RoomUnreadCountProjection {

    Integer getRoomId();

    Long getUnreadCount();
}
