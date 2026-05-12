CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    avatar_url VARCHAR(500),
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS animals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type VARCHAR(32) NOT NULL,
    gender VARCHAR(32) NOT NULL,
    age INT,
    found_region VARCHAR(255) NOT NULL,
    health_condition VARCHAR(500),
    cover_image_url VARCHAR(500),
    description VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    review_comment VARCHAR(500),
    publisher_id BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_animals_publisher FOREIGN KEY (publisher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS animal_images (
    animal_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_animal_images_animal FOREIGN KEY (animal_id) REFERENCES animals(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rescues (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    location VARCHAR(255) NOT NULL,
    animal_condition VARCHAR(500) NOT NULL,
    contact VARCHAR(64) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    review_comment VARCHAR(500),
    publisher_id BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_rescues_publisher FOREIGN KEY (publisher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rescue_images (
    rescue_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_rescue_images_rescue FOREIGN KEY (rescue_id) REFERENCES rescues(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS adopt_applies (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    animal_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name VARCHAR(64) NOT NULL,
    contact VARCHAR(64) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    living_condition VARCHAR(1000) NOT NULL,
    experience VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    audit_opinion VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_adopt_applies_animal FOREIGN KEY (animal_id) REFERENCES animals(id),
    CONSTRAINT fk_adopt_applies_applicant FOREIGN KEY (applicant_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notices (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    publisher_id BIGINT NOT NULL,
    published_at DATETIME,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_notices_publisher FOREIGN KEY (publisher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    auditor_id BIGINT NOT NULL,
    action VARCHAR(32) NOT NULL,
    opinion VARCHAR(500) NOT NULL,
    audit_time DATETIME NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_audit_logs_auditor FOREIGN KEY (auditor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
