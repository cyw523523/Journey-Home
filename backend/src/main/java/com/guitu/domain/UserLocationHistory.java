package com.guitu.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户地图定位历史。
 * 仅记录用户主动使用地图周边查询时的坐标，便于后续扩展推荐与审计。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_location_history")
public class UserLocationHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "location_lat", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double latitude;

    @Column(name = "location_lng", nullable = false, columnDefinition = "DECIMAL(10,7)")
    private Double longitude;
}
