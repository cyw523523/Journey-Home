-- 地图定位功能数据库增量脚本
-- 如果使用 spring.jpa.hibernate.ddl-auto=update，通常后端启动时会自动补齐这些字段。
-- 如果你的数据库没有自动更新，可在 MySQL 中手动执行本脚本。

ALTER TABLE animals
  ADD COLUMN location_lng DECIMAL(10,7) NULL,
  ADD COLUMN location_lat DECIMAL(10,7) NULL;

ALTER TABLE rescue_stations
  ADD COLUMN location_lng DECIMAL(10,7) NULL,
  ADD COLUMN location_lat DECIMAL(10,7) NULL,
  ADD COLUMN service_time VARCHAR(128) NULL;

CREATE TABLE IF NOT EXISTS user_location_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    location_lat DECIMAL(10,7) NOT NULL,
    location_lng DECIMAL(10,7) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_user_location_history_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
