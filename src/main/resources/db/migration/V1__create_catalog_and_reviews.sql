CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES products(id),
    author VARCHAR(100) NOT NULL,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title VARCHAR(160) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_review_product_author UNIQUE (product_id, author)
);
CREATE INDEX idx_reviews_product_created ON reviews(product_id, created_at DESC, id DESC);
