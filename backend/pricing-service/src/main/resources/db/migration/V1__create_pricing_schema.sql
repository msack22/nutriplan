CREATE TABLE tracked_ingredient (
    name     VARCHAR(200) NOT NULL PRIMARY KEY,
    added_at DATETIME(6)  NOT NULL
);

CREATE TABLE product_price (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain        VARCHAR(30)  NOT NULL,
    ingredient   VARCHAR(200) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    brand        VARCHAR(120),
    price        DOUBLE       NOT NULL,
    package_size DOUBLE,
    package_unit VARCHAR(10),
    product_url  VARCHAR(500),
    fetched_at   DATETIME(6)  NOT NULL,
    CONSTRAINT uk_price_chain_ingredient UNIQUE (chain, ingredient)
);

CREATE TABLE price_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    chain        VARCHAR(30)  NOT NULL,
    ingredient   VARCHAR(200) NOT NULL,
    price_date   DATE         NOT NULL,
    price        DOUBLE       NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    INDEX idx_history_ingredient (ingredient, price_date)
);
