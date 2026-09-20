CREATE TABLE menu_items (
    id BIGSERIAL PRIMARY KEY ,

    restaurant_id BIGINT NOT NULL ,

    name VARCHAR(255) NOT NULL ,

    description VARCHAR(1000),

    price NUMERIC(10, 2) NOT NULL ,

    category VARCHAR(100),

    available BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    deleted_at TIMESTAMP,

    CONSTRAINT fk_menu_items_restaurant
                        FOREIGN KEY (restaurant_id)
                        REFERENCES restaurants(id)
);