package com.guitu.repository;

import com.guitu.domain.DirectConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DirectConversationRepository extends JpaRepository<DirectConversation, Long> {
    Optional<DirectConversation> findByUserOneIdAndUserTwoId(Long userOneId, Long userTwoId);

    @Query("""
            select c from DirectConversation c
            where c.userOne.id = :userId or c.userTwo.id = :userId
            order by coalesce(c.lastMessageAt, c.createdAt) desc, c.id desc
            """)
    Page<DirectConversation> findMine(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select c from DirectConversation c
            where c.id = :conversationId and (c.userOne.id = :userId or c.userTwo.id = :userId)
            """)
    Optional<DirectConversation> findOwnedConversation(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
