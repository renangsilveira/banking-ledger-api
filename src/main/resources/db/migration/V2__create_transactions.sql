CREATE TABLE transactions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key  VARCHAR(255)   NOT NULL UNIQUE,
    type             VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'COMPLETED',
    amount           BIGINT         NOT NULL,
    currency         VARCHAR(3)     NOT NULL DEFAULT 'BRL',
    description      VARCHAR(500),
    correlation_id   VARCHAR(255),
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_transaction_type   CHECK (type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT chk_transaction_status CHECK (status IN ('COMPLETED', 'FAILED')),
    CONSTRAINT chk_transaction_amount CHECK (amount > 0),
    CONSTRAINT chk_transaction_currency CHECK (currency ~ '^[A-Z]{3}$')
);
