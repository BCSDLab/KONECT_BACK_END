package gg.agit.konect.domain.notification.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationMuteSetting;

public interface NotificationMuteSettingRepository extends Repository<NotificationMuteSetting, Integer> {

    NotificationMuteSetting save(NotificationMuteSetting setting);

    @Query("""
        SELECT s
        FROM NotificationMuteSetting s
        WHERE s.targetType = :targetType
        AND s.user.id = :userId
        AND ((:targetId IS NULL AND s.targetId IS NULL) OR s.targetId = :targetId)
        """)
    Optional<NotificationMuteSetting> findByTargetTypeAndTargetIdAndUserId(
        @Param("targetType") NotificationTargetType targetType,
        @Param("targetId") Integer targetId,
        @Param("userId") Integer userId
    );

    @Query("""
        SELECT s
        FROM NotificationMuteSetting s
        WHERE s.targetType = :targetType
        AND s.user.id = :userId
        AND s.targetId IN :targetIds
        """)
    List<NotificationMuteSetting> findByTargetTypeAndTargetIdsAndUserId(
        @Param("targetType") NotificationTargetType targetType,
        @Param("targetIds") List<Integer> targetIds,
        @Param("userId") Integer userId
    );

    @Query("""
        SELECT s
        FROM NotificationMuteSetting s
        WHERE s.targetType = :targetType
        AND ((:targetId IS NULL AND s.targetId IS NULL) OR s.targetId = :targetId)
        AND s.isMuted = true
        """)
    List<NotificationMuteSetting> findByTargetTypeAndTargetIdAndIsMutedTrue(
        @Param("targetType") NotificationTargetType targetType,
        @Param("targetId") Integer targetId
    );
}
