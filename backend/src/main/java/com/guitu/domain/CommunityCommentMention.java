package com.guitu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "community_comment_mentions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "mentioned_user_id"}))
public class CommunityCommentMention extends BaseEntity {
    @Column(nullable = false)
    private Long commentId;
    @Column(nullable = false)
    private Long mentionedUserId;
}
