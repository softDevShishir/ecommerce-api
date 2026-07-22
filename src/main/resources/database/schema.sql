-- =============================================================================
-- E-Commerce API — PostgreSQL Schema
-- Database: ecommerce_db
-- =============================================================================

-- -----------------------------------------------------------------------------
-- USERS
-- Stores registered customer and admin accounts.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name  VARCHAR(100),
    role       VARCHAR(50)  NOT NULL DEFAULT 'USER'
                            CHECK (role IN ('ADMIN', 'USER')),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- PRODUCTS
-- Catalogue of items available for purchase.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id               BIGSERIAL      PRIMARY KEY,
    name             VARCHAR(255)   NOT NULL,
    description      TEXT,
    price            NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    stock_quantity   INTEGER        NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    category         VARCHAR(100),
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- ORDERS
-- Represents a purchase placed by a user.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id          BIGSERIAL      PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users(id),
    order_date  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_price NUMERIC(10, 2) NOT NULL CHECK (total_price >= 0),
    status      VARCHAR(50)    NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- ORDER_ITEMS
-- Line items belonging to an order.
-- Cascade-deleted when their parent order is removed.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id         BIGSERIAL      PRIMARY KEY,
    order_id   BIGINT         NOT NULL REFERENCES orders(id)   ON DELETE CASCADE,
    product_id BIGINT         NOT NULL REFERENCES products(id),
    quantity   INTEGER        NOT NULL CHECK (quantity > 0),
    price      NUMERIC(10, 2) NOT NULL CHECK (price >= 0)
);

-- -----------------------------------------------------------------------------
-- CART
-- One shopping cart per user (1-to-1).
-- Cascade-deleted when the owning user is removed.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -----------------------------------------------------------------------------
-- CART_ITEMS
-- Products held in a cart.
-- Cascade-deleted when their parent cart is removed.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT    NOT NULL REFERENCES cart(id)     ON DELETE CASCADE,
    product_id BIGINT    NOT NULL REFERENCES products(id),
    quantity   INTEGER   NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);

-- =============================================================================
-- INDEXES
-- =============================================================================

-- users
CREATE INDEX IF NOT EXISTS idx_users_email         ON users(email);

-- products
CREATE INDEX IF NOT EXISTS idx_products_name       ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_category   ON products(category);

-- orders
CREATE INDEX IF NOT EXISTS idx_orders_user_id      ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status       ON orders(status);

-- order_items
CREATE INDEX IF NOT EXISTS idx_order_items_order   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product ON order_items(product_id);

-- cart_items
CREATE INDEX IF NOT EXISTS idx_cart_items_cart     ON cart_items(cart_id);
CREATE INDEX IF NOT EXISTS idx_cart_items_product  ON cart_items(product_id);
