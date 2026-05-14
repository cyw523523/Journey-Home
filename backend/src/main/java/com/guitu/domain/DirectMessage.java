package com.guitu.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "direct_messages")
public class DirectMessage extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private DirectConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Column(nullable = false, length = 2000)
    private String content;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "direct_message_images", joinColumns = @JoinColumn(name = "message_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>();

    @Column(nullable = false)
    private boolean readFlag = false;

    private LocalDateTime readAt;
}
