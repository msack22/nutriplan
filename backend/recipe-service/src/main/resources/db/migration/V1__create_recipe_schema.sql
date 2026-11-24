-- Flyway: migraciones versionadas e inmutables (V1, V2, ...). Nunca editar una ya aplicada:
-- se agrega una nueva. Flyway guarda el historial en la tabla flyway_schema_history.

CREATE TABLE category (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    slug       VARCHAR(80)  NOT NULL UNIQUE,
    name       VARCHAR(120) NOT NULL,
    emoji      VARCHAR(16),
    sort_order INT          NOT NULL DEFAULT 0
);

CREATE TABLE recipe (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id      VARCHAR(64) UNIQUE,
    title            VARCHAR(255) NOT NULL,
    category_id      BIGINT       NOT NULL,
    servings         INT          NOT NULL DEFAULT 1,
    yield_label      VARCHAR(120),
    kcal             DOUBLE,
    protein_g        DOUBLE,
    carbs_g          DOUBLE,
    fat_g            DOUBLE,
    nutrition_source VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    notes            TEXT,
    video_url        VARCHAR(500),
    active           BIT          NOT NULL DEFAULT 1,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    CONSTRAINT fk_recipe_category FOREIGN KEY (category_id) REFERENCES category (id),
    INDEX idx_recipe_kcal (kcal),
    INDEX idx_recipe_title (title)
);

CREATE TABLE recipe_meal_type (
    recipe_id BIGINT      NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (recipe_id, meal_type),
    CONSTRAINT fk_meal_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id) ON DELETE CASCADE
);

CREATE TABLE recipe_tag (
    recipe_id BIGINT      NOT NULL,
    tag       VARCHAR(30) NOT NULL,
    PRIMARY KEY (recipe_id, tag),
    CONSTRAINT fk_tag_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id) ON DELETE CASCADE
);

CREATE TABLE recipe_step (
    recipe_id BIGINT NOT NULL,
    position  INT    NOT NULL,
    step      TEXT   NOT NULL,
    PRIMARY KEY (recipe_id, position),
    CONSTRAINT fk_step_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id) ON DELETE CASCADE
);

CREATE TABLE recipe_ingredient (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id     BIGINT       NOT NULL,
    position      INT          NOT NULL,
    raw_text      VARCHAR(500) NOT NULL,
    name          VARCHAR(200) NOT NULL,
    quantity      DOUBLE,
    unit          VARCHAR(20),
    optional_item BIT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_ingredient_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id) ON DELETE CASCADE,
    INDEX idx_ingredient_name (name)
);

CREATE TABLE recipe_embedding (
    recipe_id  BIGINT      NOT NULL PRIMARY KEY,
    model      VARCHAR(80) NOT NULL,
    dimensions INT         NOT NULL,
    vector     MEDIUMBLOB  NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_embedding_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id) ON DELETE CASCADE
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
