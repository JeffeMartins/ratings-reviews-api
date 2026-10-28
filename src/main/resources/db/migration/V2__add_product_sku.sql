ALTER TABLE products ADD COLUMN sku VARCHAR(64);
UPDATE products SET sku = 'PRD-' || UPPER(id::text);
ALTER TABLE products ALTER COLUMN sku SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT uk_products_sku UNIQUE (sku);
