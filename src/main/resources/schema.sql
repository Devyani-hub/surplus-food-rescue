
CREATE TABLE IF NOT EXISTS donors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS organizations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    daily_capacity_kg DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS organization_food_types (
    organization_id BIGINT NOT NULL,
    food_type VARCHAR(40) NOT NULL,
    PRIMARY KEY (organization_id, food_type),
    FOREIGN KEY (organization_id)
        REFERENCES organizations(id)
);


CREATE TABLE IF NOT EXISTS food_listings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    donor_id BIGINT NOT NULL,
    food_type VARCHAR(40) NOT NULL,
    quantity_kg DECIMAL(10,2) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    pickup_address VARCHAR(500) NULL,
    pickup_deadline DATETIME NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    description VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donor_id) REFERENCES donors(id),
    INDEX idx_status_deadline (status, pickup_deadline)
);


CREATE TABLE IF NOT EXISTS allocations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    listing_id BIGINT NOT NULL UNIQUE,
    organization_id BIGINT NOT NULL,
    score DOUBLE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (listing_id) REFERENCES food_listings(id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    INDEX idx_org_allocation_date (organization_id, created_at)
);


CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('DONOR', 'NGO') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS donor_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    donor_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donor_id) REFERENCES donors(id),
    FOREIGN KEY (listing_id) REFERENCES food_listings(id),
    FOREIGN KEY (organization_id) REFERENCES organizations(id),
    INDEX idx_donor_notifications (donor_id, created_at)
);
