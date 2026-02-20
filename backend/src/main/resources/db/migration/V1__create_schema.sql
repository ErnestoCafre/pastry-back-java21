-- ============================================================
-- V1: Schema completo inicial — Malva Pastry Shop
-- Generado a partir de entidades JPA con Hibernate 7 / Spring Boot 4
-- Este script es marcado como BASELINE en prod (no se ejecuta).
-- Se usa para crear desde cero en staging y CI.
-- ============================================================

-- 1. roles (sin FKs propias)
CREATE TABLE IF NOT EXISTS roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    inserted_at TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP,
    CONSTRAINT uq_roles_name UNIQUE (name)
);

-- 2. users (FK → roles)
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    last_name     VARCHAR(255),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    system_admin  BOOLEAN      NOT NULL DEFAULT FALSE,
    role_id       BIGINT       NOT NULL,
    inserted_at   TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT fk_users_role  FOREIGN KEY (role_id) REFERENCES roles (id)
);

-- 3. public_users (sin FKs propias)
CREATE TABLE IF NOT EXISTS public_users (
    id           BIGSERIAL    PRIMARY KEY,
    google_id    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    avatar_url   VARCHAR(500),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    inserted_at  TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP,
    CONSTRAINT uq_public_users_google_id UNIQUE (google_id),
    CONSTRAINT uq_public_users_email     UNIQUE (email)
);

-- 4. categories (FK → users via deleted_by_id)
CREATE TABLE IF NOT EXISTS categories (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    deleted_at    TIMESTAMP,
    deleted_by_id BIGINT,
    inserted_at   TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT fk_categories_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES users (id)
);

-- 5. ingredients (FK → users via deleted_by_id)
CREATE TABLE IF NOT EXISTS ingredients (
    id              BIGSERIAL      PRIMARY KEY,
    name            VARCHAR(100)   NOT NULL,
    description     TEXT,
    unit_cost       NUMERIC(12, 2) NOT NULL,
    unit_of_measure VARCHAR(20)    NOT NULL,
    deleted_at      TIMESTAMP,
    deleted_by_id   BIGINT,
    inserted_at     TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP,
    CONSTRAINT fk_ingredients_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES users (id)
);

-- 6. products (FK → users, categories)
CREATE TABLE IF NOT EXISTS products (
    id               BIGSERIAL      PRIMARY KEY,
    name             VARCHAR(100)   NOT NULL,
    description      TEXT,
    preparation_days INTEGER,
    image_url        VARCHAR(500),
    base_price       NUMERIC(12, 2),
    visible          BOOLEAN        NOT NULL DEFAULT FALSE,
    user_id          BIGINT,
    category_id      BIGINT,
    deleted_at       TIMESTAMP,
    deleted_by_id    BIGINT,
    inserted_at      TIMESTAMP      NOT NULL,
    updated_at       TIMESTAMP,
    CONSTRAINT fk_product_user       FOREIGN KEY (user_id)        REFERENCES users (id),
    CONSTRAINT fk_product_category   FOREIGN KEY (category_id)    REFERENCES categories (id),
    CONSTRAINT fk_product_deleted_by FOREIGN KEY (deleted_by_id)  REFERENCES users (id)
);

-- 7. tags (FK → users via deleted_by_id)
CREATE TABLE IF NOT EXISTS tags (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    description   VARCHAR(200),
    deleted_at    TIMESTAMP,
    deleted_by_id BIGINT,
    inserted_at   TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT uq_tags_slug       UNIQUE (slug),
    CONSTRAINT fk_tags_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES users (id)
);

-- 8. storefront_sections (FK → users via deleted_by_id)
CREATE TABLE IF NOT EXISTS storefront_sections (
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    description   TEXT,
    display_order INTEGER,
    visible       BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted_at    TIMESTAMP,
    deleted_by_id BIGINT,
    inserted_at   TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT uq_storefront_sections_slug       UNIQUE (slug),
    CONSTRAINT fk_storefront_sections_deleted_by FOREIGN KEY (deleted_by_id) REFERENCES users (id)
);

-- 9. product_ingredients (FK → products, ingredients)
CREATE TABLE IF NOT EXISTS product_ingredients (
    id            BIGSERIAL      PRIMARY KEY,
    product_id    BIGINT         NOT NULL,
    ingredient_id BIGINT         NOT NULL,
    quantity      NUMERIC(14, 4) NOT NULL,
    inserted_at   TIMESTAMP      NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT fk_product_ingredient_product    FOREIGN KEY (product_id)    REFERENCES products (id),
    CONSTRAINT fk_product_ingredient_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
);

-- 10. product_tags (FK → products, tags)
CREATE TABLE IF NOT EXISTS product_tags (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT    NOT NULL,
    tag_id      BIGINT    NOT NULL,
    inserted_at TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP,
    CONSTRAINT uk_product_tag          UNIQUE (product_id, tag_id),
    CONSTRAINT fk_product_tag_product  FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_tag_tag      FOREIGN KEY (tag_id)     REFERENCES tags (id)
);

-- 11. storefront_section_products (FK → storefront_sections, products)
CREATE TABLE IF NOT EXISTS storefront_section_products (
    id                    BIGSERIAL PRIMARY KEY,
    storefront_section_id BIGINT    NOT NULL,
    product_id            BIGINT    NOT NULL,
    display_order         INTEGER,
    inserted_at           TIMESTAMP NOT NULL,
    updated_at            TIMESTAMP,
    CONSTRAINT uk_section_product         UNIQUE (storefront_section_id, product_id),
    CONSTRAINT fk_section_product_section FOREIGN KEY (storefront_section_id) REFERENCES storefront_sections (id),
    CONSTRAINT fk_section_product_product FOREIGN KEY (product_id)            REFERENCES products (id)
);

-- 12. sales (FK → users, products)
CREATE TABLE IF NOT EXISTS sales (
    id               BIGSERIAL      PRIMARY KEY,
    sale_date        TIMESTAMP      NOT NULL,
    registered_by_id BIGINT         NOT NULL,
    product_id       BIGINT,
    product_name     VARCHAR(100)   NOT NULL,
    quantity         INTEGER        NOT NULL,
    unit_price       NUMERIC(12, 2) NOT NULL,
    total_amount     NUMERIC(12, 2) NOT NULL,
    notes            TEXT,
    customer_name    VARCHAR(150),
    customer_dni     VARCHAR(20),
    customer_phone   VARCHAR(20),
    inserted_at      TIMESTAMP      NOT NULL,
    updated_at       TIMESTAMP,
    CONSTRAINT fk_sale_user    FOREIGN KEY (registered_by_id) REFERENCES users (id),
    CONSTRAINT fk_sale_product FOREIGN KEY (product_id)       REFERENCES products (id)
);

-- 13. sale_ingredients (FK → sales, ingredients)
CREATE TABLE IF NOT EXISTS sale_ingredients (
    id              BIGSERIAL      PRIMARY KEY,
    sale_id         BIGINT         NOT NULL,
    ingredient_id   BIGINT,
    ingredient_name VARCHAR(100)   NOT NULL,
    quantity_used   NUMERIC(14, 4) NOT NULL,
    unit_cost       NUMERIC(12, 2) NOT NULL,
    unit_of_measure VARCHAR(20)    NOT NULL,
    total_cost      NUMERIC(12, 2) NOT NULL,
    inserted_at     TIMESTAMP      NOT NULL,
    updated_at      TIMESTAMP,
    CONSTRAINT fk_sale_ingredient_sale       FOREIGN KEY (sale_id)       REFERENCES sales (id),
    CONSTRAINT fk_sale_ingredient_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
);

-- 14. favorites (FK → public_users, products)
CREATE TABLE IF NOT EXISTS favorites (
    id             BIGSERIAL PRIMARY KEY,
    public_user_id BIGINT    NOT NULL,
    product_id     BIGINT    NOT NULL,
    inserted_at    TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP,
    CONSTRAINT uk_favorite_public_user_product UNIQUE (public_user_id, product_id),
    CONSTRAINT fk_favorite_public_user FOREIGN KEY (public_user_id) REFERENCES public_users (id),
    CONSTRAINT fk_favorite_product    FOREIGN KEY (product_id)      REFERENCES products (id)
);

-- 15. product_reviews (FK → public_users, products, users)
CREATE TABLE IF NOT EXISTS product_reviews (
    id              BIGSERIAL   PRIMARY KEY,
    public_user_id  BIGINT      NOT NULL,
    product_id      BIGINT      NOT NULL,
    content         TEXT        NOT NULL,
    rating          INTEGER     NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    moderated_by_id BIGINT,
    moderated_at    TIMESTAMP,
    inserted_at     TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP,
    CONSTRAINT uk_review_public_user_product UNIQUE (public_user_id, product_id),
    CONSTRAINT fk_review_public_user   FOREIGN KEY (public_user_id)  REFERENCES public_users (id),
    CONSTRAINT fk_review_product       FOREIGN KEY (product_id)      REFERENCES products (id),
    CONSTRAINT fk_review_moderated_by  FOREIGN KEY (moderated_by_id) REFERENCES users (id)
);
