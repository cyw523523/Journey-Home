package com.guitu.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Haversine 球面距离计算工具类。
 * 用于根据两组经纬度计算地球表面两点之间的近似距离，单位为公里。
 */
public final class HaversineUtil {
    /** 地球平均半径，单位：公里。 */
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private HaversineUtil() {
    }

    /**
     * 计算两点之间的球面距离。
     *
     * @param lat1 起点纬度
     * @param lng1 起点经度
     * @param lat2 终点纬度
     * @param lng2 终点经度
     * @return 两点距离，单位：公里，保留 3 位小数
     */
    public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return BigDecimal.valueOf(distance).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
