CREATE TABLE service_image (
    service_key VARCHAR(40) PRIMARY KEY,
    image_url VARCHAR(2048) NOT NULL,
    storage_key VARCHAR(255) NOT NULL UNIQUE,
    updated_at TIMESTAMP NOT NULL
);
