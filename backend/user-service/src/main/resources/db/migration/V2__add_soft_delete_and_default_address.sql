ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP;

CREATE TABLE user_default_addresses (
                                        id BIGSERIAL PRIMARY KEY,
                                        user_id BIGINT NOT NULL UNIQUE,
                                        recipient_name VARCHAR(255) NOT NULL,
                                        phone VARCHAR(50) NOT NULL,
                                        address_line VARCHAR(500) NOT NULL,
                                        city VARCHAR(255) NOT NULL,
                                        postal_code VARCHAR(50) NOT NULL,
                                        latitude DOUBLE PRECISION,
                                        longitude DOUBLE PRECISION,
                                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT fk_default_address_user
                                            FOREIGN KEY (user_id)
                                                REFERENCES users(id)
);