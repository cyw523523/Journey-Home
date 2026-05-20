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

CREATE TABLE IF NOT EXISTS community_posts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_community_posts_author FOREIGN KEY (author_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS community_comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    content TEXT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_community_comments_post FOREIGN KEY (post_id) REFERENCES community_posts(id),
    CONSTRAINT fk_community_comments_author FOREIGN KEY (author_id) REFERENCES users(id),
    CONSTRAINT fk_cc_parent FOREIGN KEY (parent_comment_id) REFERENCES community_comments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS community_post_images (
    post_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_cp_images_post FOREIGN KEY (post_id) REFERENCES community_posts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS community_comment_images (
    comment_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_cc_images_comment FOREIGN KEY (comment_id) REFERENCES community_comments(id)
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

CREATE TABLE IF NOT EXISTS direct_conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_one_id BIGINT NOT NULL,
    user_two_id BIGINT NOT NULL,
    last_message_content VARCHAR(500),
    last_sender_id BIGINT,
    last_message_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_dc_user_one FOREIGN KEY (user_one_id) REFERENCES users(id),
    CONSTRAINT fk_dc_user_two FOREIGN KEY (user_two_id) REFERENCES users(id),
    CONSTRAINT fk_dc_last_sender FOREIGN KEY (last_sender_id) REFERENCES users(id),
    CONSTRAINT uk_dc_participants UNIQUE (user_one_id, user_two_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS direct_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    read_flag BIT NOT NULL,
    read_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_dm_conversation FOREIGN KEY (conversation_id) REFERENCES direct_conversations(id),
    CONSTRAINT fk_dm_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_dm_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS direct_message_images (
    message_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_dm_images_message FOREIGN KEY (message_id) REFERENCES direct_messages(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS supply_demands (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    category VARCHAR(32) NOT NULL,
    target_quantity INT NOT NULL,
    current_quantity INT NOT NULL DEFAULT 0,
    description VARCHAR(1000) NOT NULL,
    contact_name VARCHAR(255),
    contact_phone VARCHAR(64),
    shipping_address VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    image_url VARCHAR(500),
    publisher_id BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_supply_demands_publisher FOREIGN KEY (publisher_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS donation_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    demand_id BIGINT NOT NULL,
    donor_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    delivery_method VARCHAR(32),
    tracking_number VARCHAR(500),
    message VARCHAR(1000),
    donor_display_name VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    completed_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_donation_records_demand FOREIGN KEY (demand_id) REFERENCES supply_demands(id),
    CONSTRAINT fk_donation_records_donor FOREIGN KEY (donor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS volunteer_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    location VARCHAR(255) NOT NULL,
    max_volunteers INT NOT NULL DEFAULT 1,
    current_volunteers INT NOT NULL DEFAULT 0,
    scheduled_time DATETIME,
    image_url VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    review_comment VARCHAR(500),
    publisher_id BIGINT NOT NULL,
    related_rescue_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_vt_publisher FOREIGN KEY (publisher_id) REFERENCES users(id),
    CONSTRAINT fk_vt_rescue FOREIGN KEY (related_rescue_id) REFERENCES rescues(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS volunteer_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    volunteer_id BIGINT NOT NULL,
    message VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    review_comment VARCHAR(500),
    completed_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_va_task FOREIGN KEY (task_id) REFERENCES volunteer_tasks(id),
    CONSTRAINT fk_va_volunteer FOREIGN KEY (volunteer_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rescue_stations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    station_name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    address VARCHAR(255),
    contact_phone VARCHAR(64),
    image_url VARCHAR(500),
    certification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    follower_count INT NOT NULL DEFAULT 0,
    reject_reason TEXT,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_rs_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_follows (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    follower_id BIGINT NOT NULL,
    station_user_id BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT uf_follower FOREIGN KEY (follower_id) REFERENCES users(id),
    CONSTRAINT uf_station FOREIGN KEY (station_user_id) REFERENCES users(id),
    UNIQUE KEY uk_follow (follower_id, station_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
