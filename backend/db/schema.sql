-- E-Bookstore data model (PostgreSQL)
CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(150) NOT NULL,
  gift_points   INTEGER NOT NULL DEFAULT 0 CHECK (gift_points >= 0),
  created_at    TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE categories (
  id   BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE brands (
  id   BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL UNIQUE      -- publisher / brand
);

CREATE TABLE products (
  id                 BIGSERIAL PRIMARY KEY,
  title              VARCHAR(255) NOT NULL,
  author             VARCHAR(150),
  description        TEXT,
  price              NUMERIC(10,2) NOT NULL CHECK (price >= 0),
  stock              INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
  image_url          VARCHAR(500),
  delivery_days      INTEGER NOT NULL DEFAULT 5,   -- tentative delivery = today + delivery_days
  category_id        BIGINT NOT NULL REFERENCES categories(id),
  brand_id           BIGINT REFERENCES brands(id)
);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_brand    ON products(brand_id);

-- Explicit "related products" (fallback: same category)
CREATE TABLE related_products (
  product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
  related_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
  PRIMARY KEY (product_id, related_id)
);

CREATE TABLE addresses (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  line1       VARCHAR(255) NOT NULL,
  line2       VARCHAR(255),
  city        VARCHAR(100) NOT NULL,
  state       VARCHAR(100),
  postal_code VARCHAR(20)  NOT NULL,
  country     VARCHAR(100) NOT NULL,
  is_default  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE carts (
  id      BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE cart_items (
  id         BIGSERIAL PRIMARY KEY,
  cart_id    BIGINT NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL REFERENCES products(id),
  quantity   INTEGER NOT NULL CHECK (quantity > 0),
  UNIQUE (cart_id, product_id)
);

CREATE TABLE orders (
  id                  BIGSERIAL PRIMARY KEY,
  user_id             BIGINT NOT NULL REFERENCES users(id),
  address_id          BIGINT NOT NULL REFERENCES addresses(id),
  status              VARCHAR(20) NOT NULL DEFAULT 'PLACED'
                      CHECK (status IN ('PLACED','PAID','CANCELLED','DELIVERED')),
  subtotal            NUMERIC(10,2) NOT NULL,
  gift_points_used    INTEGER NOT NULL DEFAULT 0,
  total               NUMERIC(10,2) NOT NULL,
  created_at          TIMESTAMP NOT NULL DEFAULT now(),   -- cancel allowed within 48h of this
  cancelled_at        TIMESTAMP
);
CREATE INDEX idx_orders_user ON orders(user_id, created_at DESC);

CREATE TABLE order_items (
  id         BIGSERIAL PRIMARY KEY,
  order_id   BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product_id BIGINT NOT NULL REFERENCES products(id),
  quantity   INTEGER NOT NULL CHECK (quantity > 0),
  unit_price NUMERIC(10,2) NOT NULL,                      -- price snapshot at purchase
  expected_delivery DATE
);

CREATE TABLE payments (
  id         BIGSERIAL PRIMARY KEY,
  order_id   BIGINT NOT NULL UNIQUE REFERENCES orders(id),
  method     VARCHAR(20) NOT NULL CHECK (method IN ('CREDIT_CARD','DEBIT_CARD','GIFT_POINTS')),
  amount     NUMERIC(10,2) NOT NULL,
  status     VARCHAR(20) NOT NULL CHECK (status IN ('SUCCESS','FAILED','REFUNDED')),
  card_last4 CHAR(4),                                     -- never store full card numbers
  paid_at    TIMESTAMP NOT NULL DEFAULT now()
);
