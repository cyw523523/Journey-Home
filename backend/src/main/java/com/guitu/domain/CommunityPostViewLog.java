package com.guitu.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "community_post_view_logs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "viewer_key", "viewed_on"}))
public class CommunityPostViewLog extends BaseEntity {
    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false, length = 80)
    private String viewerKey;

    @Column(nullable = false)
    private LocalDate viewedOn;
}
