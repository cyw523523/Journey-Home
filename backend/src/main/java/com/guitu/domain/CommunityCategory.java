package com.guitu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "community_categories")
public class CommunityCategory extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 64)
    private String nameEn;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String icon;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private long postCount = 0;
}
