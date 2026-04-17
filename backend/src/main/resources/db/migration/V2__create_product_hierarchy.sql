-- V2: 상품 계층 테이블 생성
-- Requirements: 14.1 (데이터 저장)
-- 4단계 계층: Category → Subcategory → Brand → Product(SKU)

-- 상품 카테고리
CREATE TABLE product_categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT
);

-- 상품 서브카테고리
CREATE TABLE product_subcategories (
    id SERIAL PRIMARY KEY,
    category_id INTEGER NOT NULL REFERENCES product_categories(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL
);

CREATE INDEX idx_subcategories_category_id ON product_subcategories(category_id);

-- 브랜드
CREATE TABLE brands (
    id SERIAL PRIMARY KEY,
    subcategory_id INTEGER NOT NULL REFERENCES product_subcategories(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL
);

CREATE INDEX idx_brands_subcategory_id ON brands(subcategory_id);

-- 상품 (SKU)
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    brand_id INTEGER NOT NULL REFERENCES brands(id) ON DELETE CASCADE,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(300) NOT NULL,
    variant_color VARCHAR(50),
    variant_size VARCHAR(50),
    unit_cost DECIMAL(15, 2) NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_products_brand_id ON products(brand_id);
CREATE INDEX idx_products_sku ON products(sku);
