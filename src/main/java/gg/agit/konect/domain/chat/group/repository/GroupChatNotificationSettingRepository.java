package gg.agit.konect.domain.chat.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.group.model.GroupChatNotificationSetting;

public interface GroupChatNotificationSettingRepository extends Repository<GroupChatNotificationSetting, Integer> {

    GroupChatNotificationSetting save(GroupChatNotificationSetting setting);

    @Query("""
        SELECT s
        FROM GroupChatNotificationSetting s
        WHERE s.room.id = :roomId
        AND s.user.id = :userId
        """)
    Optional<GroupChatNotificationSetting> findByRoomIdAndUserId(
        @Param("roomId") Integer roomId,
        @Param("userId") Integer userId
    );

    @Query("""
        SELECT s
        FROM GroupChatNotificationSetting s
        WHERE s.room.id = :roomId
        AND s.isMuted = true
        """)
    List<GroupChatNotificationSetting> findByRoomIdAndIsMutedTrue(@Param("roomId") Integer roomId);
}
