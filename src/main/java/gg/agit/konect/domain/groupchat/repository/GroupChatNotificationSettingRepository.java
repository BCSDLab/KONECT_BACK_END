package gg.agit.konect.domain.groupchat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.groupchat.model.GroupChatNotificationSetting;

public interface GroupChatNotificationSettingRepository extends Repository<GroupChatNotificationSetting, Integer> {

    GroupChatNotificationSetting save(GroupChatNotificationSetting setting);

    Optional<GroupChatNotificationSetting> findByRoomIdAndUserId(Integer roomId, Integer userId);

    List<GroupChatNotificationSetting> findByRoomIdAndIsMutedTrue(Integer roomId);
}
