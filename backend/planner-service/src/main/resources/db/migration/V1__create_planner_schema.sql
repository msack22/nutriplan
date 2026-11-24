CREATE TABLE user_profile (
    user_id           VARCHAR(64) NOT NULL PRIMARY KEY,   -- "sub" del JWT de Keycloak
    display_name      VARCHAR(120),
    sex               VARCHAR(10),
    birth_date        DATE,
    height_cm         DOUBLE,
    current_weight_kg DOUBLE,
    target_weight_kg  DOUBLE,
    activity_level    VARCHAR(20),
    goal              VARCHAR(20),
    default_people    INT         NOT NULL DEFAULT 1,
    vegetarian        BIT         NOT NULL DEFAULT 0,
    version           BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE user_profile_meal (
    user_id   VARCHAR(64) NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, meal_type),
    CONSTRAINT fk_profile_meal FOREIGN KEY (user_id) REFERENCES user_profile (user_id) ON DELETE CASCADE
);

CREATE TABLE user_profile_excluded (
    user_id    VARCHAR(64)  NOT NULL,
    ingredient VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, ingredient),
    CONSTRAINT fk_profile_excluded FOREIGN KEY (user_id) REFERENCES user_profile (user_id) ON DELETE CASCADE
);

CREATE TABLE weight_entry (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(64) NOT NULL,
    entry_date DATE        NOT NULL,
    weight_kg  DOUBLE      NOT NULL,
    note       VARCHAR(255),
    CONSTRAINT uk_weight_user_date UNIQUE (user_id, entry_date)
);

CREATE TABLE weekly_plan (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    VARCHAR(64) NOT NULL,
    week_start DATE        NOT NULL,
    people     INT         NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT uk_plan_user_week UNIQUE (user_id, week_start)
);

CREATE TABLE planned_meal (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id      BIGINT       NOT NULL,
    day_of_week  VARCHAR(10)  NOT NULL,
    meal_type    VARCHAR(20)  NOT NULL,
    recipe_id    BIGINT       NOT NULL,
    recipe_title VARCHAR(255) NOT NULL,
    servings     INT          NOT NULL,
    kcal         DOUBLE,
    protein_g    DOUBLE,
    carbs_g      DOUBLE,
    fat_g        DOUBLE,
    eaten        BIT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_meal_plan FOREIGN KEY (plan_id) REFERENCES weekly_plan (id) ON DELETE CASCADE,
    CONSTRAINT uk_meal_slot UNIQUE (plan_id, day_of_week, meal_type)
);

CREATE TABLE outbox_event (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    topic        VARCHAR(150) NOT NULL,
    message_key  VARCHAR(100),
    type         VARCHAR(150) NOT NULL,
    payload      MEDIUMTEXT   NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    published_at DATETIME(6),
    attempts     INT          NOT NULL DEFAULT 0,
    INDEX idx_outbox_pending (published_at, created_at)
);

CREATE TABLE processed_event (
    event_id     VARCHAR(64) NOT NULL PRIMARY KEY,
    processed_at DATETIME(6) NOT NULL
);
