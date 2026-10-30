-- ==========================================================
-- RestockRadar — MySQL Schema
-- Matches the Physical Database Design document.
-- Engine: InnoDB | Charset: utf8mb4
-- Note: Spring Boot's spring.jpa.hibernate.ddl-auto=update will also
-- auto-create/update these tables on first run. Running this script
-- manually is optional but gives you full control over indexes.
-- ==========================================================

CREATE DATABASE IF NOT EXISTS restockradar_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE restockradar_db;

-- ---------------- Users ----------------
CREATE TABLE IF NOT EXISTS users (
    user_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_name   VARCHAR(100) NOT NULL,
    shop_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(100) UNIQUE,
    phone        VARCHAR(15) UNIQUE,
    password     VARCHAR(255) NOT NULL,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- Categories ----------------
CREATE TABLE IF NOT EXISTS categories (
    category_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL,
    description   TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- Products ----------------
CREATE TABLE IF NOT EXISTS products (
    product_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT NOT NULL,
    category_id    BIGINT,
    product_name   VARCHAR(100) NOT NULL,
    cost_price     DECIMAL(10,2) NOT NULL,
    selling_price  DECIMAL(10,2) NOT NULL,
    current_stock  INT NOT NULL,
    minimum_stock  INT NOT NULL,
    barcode        VARCHAR(100) UNIQUE,
    batch_number   VARCHAR(50),
    expiry_date    DATE,
    supplier_id    BIGINT,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL,
    CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_products_name ON products(product_name);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_user ON products(user_id);

-- ---------------- Sales ----------------
CREATE TABLE IF NOT EXISTS sales (
    sale_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT NOT NULL,
    quantity_sold  INT NOT NULL,
    selling_price  DECIMAL(10,2) NOT NULL,
    total_amount   DECIMAL(10,2),
    sale_date      DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_sales_product ON sales(product_id);
CREATE INDEX idx_sales_date ON sales(sale_date);

-- ---------------- Inventory ----------------
CREATE TABLE IF NOT EXISTS inventory (
    inventory_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id       BIGINT NOT NULL UNIQUE,
    available_stock  INT NOT NULL,
    stock_status     VARCHAR(20),  -- Low / Healthy / Overstock
    last_updated     DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- Reorder Recommendations ----------------
CREATE TABLE IF NOT EXISTS reorder_recommendations (
    reorder_id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id               BIGINT NOT NULL,
    average_daily_sales       DECIMAL(10,2),
    days_to_stockout          INT,
    suggested_quantity        INT,
    reorder_date               DATE,
    recommendation_status     VARCHAR(20) DEFAULT 'Pending', -- Pending / Ordered
    CONSTRAINT fk_reorder_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_reorder_product ON reorder_recommendations(product_id);

-- ---------------- Stock Alerts ----------------
CREATE TABLE IF NOT EXISTS stock_alerts (
    alert_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT NOT NULL,
    alert_type     VARCHAR(30),  -- Low / Overstock
    alert_message  TEXT,
    alert_date     DATETIME DEFAULT CURRENT_TIMESTAMP,
    status         VARCHAR(20) DEFAULT 'Active', -- Active / Resolved
    CONSTRAINT fk_alerts_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_alerts_product ON stock_alerts(product_id);

-- ---------------- Reports ----------------
CREATE TABLE IF NOT EXISTS reports (
    report_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id     BIGINT NOT NULL,
    revenue        DECIMAL(10,2),
    top_selling    BOOLEAN DEFAULT FALSE,
    dead_stock     BOOLEAN DEFAULT FALSE,
    generated_on   DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reports_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_reports_product ON reports(product_id);

-- ---------------- Stock Intakes (Shipment Receipts) ----------------
CREATE TABLE IF NOT EXISTS stock_intakes (
    intake_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id          BIGINT NOT NULL,
    supplier_id         BIGINT,
    quantity_received   INT NOT NULL,
    purchase_unit_price DECIMAL(10,2),
    total_cost          DECIMAL(10,2),
    batch_number        VARCHAR(50),
    expiry_date         DATE,
    invoice_number      VARCHAR(100),
    notes               TEXT,
    received_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_intake_product FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE,
    CONSTRAINT fk_intake_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_intakes_product ON stock_intakes(product_id);

-- Email/phone already indexed via UNIQUE constraints above.
