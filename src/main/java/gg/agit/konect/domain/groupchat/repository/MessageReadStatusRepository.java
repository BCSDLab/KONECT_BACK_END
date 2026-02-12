package gg.agit.konect.domain.groupchat.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.groupchat.model.MessageReadStatus;
import gg.agit.konect.domain.groupchat.model.MessageReadStatusId;

public interface MessageReadStatusRepository extends Repository<MessageReadStatus, MessageReadStatusId> {

    MessageReadStatus save(MessageReadStatus status);

    List<MessageReadStatus> saveAll(Iterable<MessageReadStatus> statuses);

    int countByMessageId(Integer messageId);

    List<MessageReadStatus> findByMessageIdIn(List<Integer> messageIds);

    boolean existsByMessageIdAndUserId(Integer messageId, Integer userId);
}
