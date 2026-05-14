package com.guitu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "direct_conversations",
        uniqueConstraints = @UniqueConstraint(name = "uk_dc_participants", columnNames = {"user_one_id", "user_two_id"})
)
public class DirectConversation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_one_id")
    private User userOne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_two_id")
    private User userTwo;

    @Column(length = 500)
    private String lastMessageContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_sender_id")
    private User lastSender;

    private LocalDateTime lastMessageAt;
}
