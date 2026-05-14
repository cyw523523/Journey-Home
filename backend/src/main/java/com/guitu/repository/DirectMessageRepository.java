package com.guitu.repository;

import com.guitu.domain.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
    List<DirectMessage> findTop50ByConversationIdOrderByCreatedAtDesc(Long conversationId);

    long countByConversationIdAndReceiverIdAndReadFlagFalse(Long conversationId, Long receiverId);

    long countByReceiverIdAndReadFlagFalse(Long receiverId);

    List<DirectMessage> findByConversationIdAndReceiverIdAndReadFlagFalse(Long conversationId, Long receiverId);
}
