CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,

                        user_id BIGINT NOT NULL,

                        restaurant_id BIGINT NOT NULL,

                        status VARCHAR(50) NOT NULL,

                        total_amount NUMERIC(10, 2) NOT NULL,

                        delivery_address VARCHAR(500) NOT NULL,

                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,

                             order_id BIGINT NOT NULL,

                             menu_item_id BIGINT NOT NULL,

                             item_name VARCHAR(255) NOT NULL,

                             unit_price NUMERIC(10, 2) NOT NULL,

                             quantity INTEGER NOT NULL,

                             subtotal NUMERIC(10, 2) NOT NULL,

                             CONSTRAINT fk_order_items_order
                                 FOREIGN KEY (order_id)
                                     REFERENCES orders(id)
                                     ON DELETE CASCADE
);