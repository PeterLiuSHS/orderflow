CREATE TABLE restaurants (
                             id BIGSERIAL PRIMARY KEY,

                             owner_user_id BIGINT NOT NULL,

                             name VARCHAR(255) NOT NULL,

                             description VARCHAR(1000),

                             phone VARCHAR(50) NOT NULL,

                             address_line VARCHAR(500) NOT NULL,

                             city VARCHAR(255) NOT NULL,

                             postal_code VARCHAR(50) NOT NULL,

                             latitude DOUBLE PRECISION,

                             longitude DOUBLE PRECISION,

                             open BOOLEAN NOT NULL DEFAULT FALSE,

                             approved BOOLEAN NOT NULL DEFAULT FALSE,

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             deleted_at TIMESTAMP
);