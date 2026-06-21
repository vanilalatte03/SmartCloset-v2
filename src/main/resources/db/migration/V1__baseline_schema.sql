CREATE TABLE users (
    email_verified BOOLEAN DEFAULT TRUE NOT NULL,
    location_nx INT NULL,
    location_ny INT NULL,
    password_login_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    location_code VARCHAR(30) NULL,
    location_name VARCHAR(50) NULL,
    location_region1 VARCHAR(50) NULL,
    location_region2 VARCHAR(50) NULL,
    location_region3 VARCHAR(50) NULL,
    name VARCHAR(50) NOT NULL,
    location_full_name VARCHAR(100) NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    preferred_colors_json TEXT NOT NULL,
    preferred_materials_json TEXT NOT NULL,
    style_tags_json TEXT NOT NULL,
    location_source VARCHAR(30) NOT NULL,
    role VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE refresh_sessions (
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    issued_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    replaced_by_token_hash VARCHAR(255) NULL,
    token_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_sessions_token_hash UNIQUE (token_hash)
);

CREATE TABLE account_action_tokens (
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    used_at DATETIME(6) NULL,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_account_action_tokens_token_hash UNIQUE (token_hash)
);

CREATE TABLE social_accounts (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    linked_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_social_accounts_provider_user UNIQUE (provider, provider_user_id)
);

CREATE TABLE clothing_items (
    archived BOOLEAN NOT NULL,
    max_temperature INT NOT NULL,
    min_temperature INT NOT NULL,
    rain_suitable BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    image_size_bytes BIGINT NULL,
    image_uploaded_at DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    image_content_type VARCHAR(100) NULL,
    image_stored_filename VARCHAR(255) NULL,
    style_tags_json TEXT NOT NULL,
    category VARCHAR(30) NOT NULL,
    color VARCHAR(30) NOT NULL,
    material VARCHAR(30) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE recommendation_results (
    color_score INT NOT NULL,
    preference_score INT NOT NULL,
    rainy BOOLEAN NOT NULL,
    recommendation_history_score INT NOT NULL,
    total_score INT NOT NULL,
    wear_history_score INT NOT NULL,
    weather_base_time VARCHAR(4) NULL,
    weather_fallback_used BOOLEAN NOT NULL,
    weather_forecast_time VARCHAR(4) NULL,
    weather_kma_used BOOLEAN NOT NULL,
    weather_location_nx INT NOT NULL,
    weather_location_ny INT NOT NULL,
    weather_score INT NOT NULL,
    weather_temperature INT NOT NULL,
    windy BOOLEAN NOT NULL,
    worn BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    feedback_updated_at DATETIME(6) NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    weather_base_date VARCHAR(8) NULL,
    weather_forecast_date VARCHAR(8) NULL,
    weather_location_code VARCHAR(30) NOT NULL,
    weather_location_name VARCHAR(50) NOT NULL,
    weather_location_full_name VARCHAR(100) NULL,
    reasons_json JSON NOT NULL,
    forecast_period VARCHAR(30) DEFAULT 'CURRENT' NOT NULL,
    sentiment_feedback VARCHAR(30) NULL,
    situation VARCHAR(30) DEFAULT 'CASUAL' NOT NULL,
    thermal_feedback VARCHAR(30) NULL,
    weather_location_source VARCHAR(30) NOT NULL,
    weather_provider VARCHAR(30) NOT NULL,
    weather_type VARCHAR(30) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE recommendation_result_items (
    clothing_item_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    recommendation_result_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    slot VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_recommendation_result_items_result_slot UNIQUE (recommendation_result_id, slot)
);

CREATE TABLE wear_histories (
    created_at DATETIME(6) NOT NULL,
    id BIGINT NOT NULL AUTO_INCREMENT,
    recommendation_result_id BIGINT NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    worn_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wear_histories_recommendation_result UNIQUE (recommendation_result_id)
);

CREATE INDEX idx_users_location_code
    ON users (location_code);
CREATE INDEX idx_users_location_grid
    ON users (location_nx, location_ny);

CREATE INDEX idx_refresh_sessions_user_expires
    ON refresh_sessions (user_id, expires_at);
CREATE INDEX idx_refresh_sessions_user_revoked
    ON refresh_sessions (user_id, revoked_at);

CREATE INDEX idx_account_action_tokens_user_purpose_created
    ON account_action_tokens (user_id, purpose, created_at);
CREATE INDEX idx_account_action_tokens_purpose_expires
    ON account_action_tokens (purpose, expires_at);

CREATE INDEX idx_social_accounts_user_provider
    ON social_accounts (user_id, provider);
CREATE INDEX idx_social_accounts_email
    ON social_accounts (email);

CREATE INDEX idx_clothing_items_user_archived_id
    ON clothing_items (user_id, archived, id);
CREATE INDEX idx_clothing_items_user_category_archived
    ON clothing_items (user_id, category, archived);

CREATE INDEX idx_recommendation_results_user_created_at
    ON recommendation_results (user_id, created_at);
CREATE INDEX idx_recommendation_results_user_worn
    ON recommendation_results (user_id, worn);
CREATE INDEX idx_recommendation_results_user_feedback_updated_at
    ON recommendation_results (user_id, feedback_updated_at);
CREATE INDEX idx_recommendation_results_user_forecast_created_at
    ON recommendation_results (user_id, forecast_period, created_at);
CREATE INDEX idx_recommendation_results_weather_location_code
    ON recommendation_results (weather_location_code);

CREATE INDEX idx_recommendation_result_items_result
    ON recommendation_result_items (recommendation_result_id);
CREATE INDEX idx_recommendation_result_items_clothing
    ON recommendation_result_items (clothing_item_id);

CREATE INDEX idx_wear_histories_user_worn_at
    ON wear_histories (user_id, worn_at);

ALTER TABLE refresh_sessions
    ADD CONSTRAINT fk_refresh_sessions_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE account_action_tokens
    ADD CONSTRAINT fk_account_action_tokens_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE social_accounts
    ADD CONSTRAINT fk_social_accounts_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE clothing_items
    ADD CONSTRAINT fk_clothing_items_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE recommendation_results
    ADD CONSTRAINT fk_recommendation_results_user
    FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE recommendation_result_items
    ADD CONSTRAINT fk_recommendation_result_items_result
    FOREIGN KEY (recommendation_result_id) REFERENCES recommendation_results (id);

ALTER TABLE recommendation_result_items
    ADD CONSTRAINT fk_recommendation_result_items_clothing
    FOREIGN KEY (clothing_item_id) REFERENCES clothing_items (id);

ALTER TABLE wear_histories
    ADD CONSTRAINT fk_wear_histories_recommendation_result
    FOREIGN KEY (recommendation_result_id) REFERENCES recommendation_results (id);

ALTER TABLE wear_histories
    ADD CONSTRAINT fk_wear_histories_user
    FOREIGN KEY (user_id) REFERENCES users (id);
