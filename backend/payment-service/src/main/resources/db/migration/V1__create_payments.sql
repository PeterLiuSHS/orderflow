CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY ,

    order_id BIGINT NOT NULL ,

    user_id BIGINT NOT NULL ,

    amount NUMERIC(10, 2) NOT NULL ,

    status VARCHAR(50) NOT NULL ,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_payments_order_id UNIQUE (order_id)
);