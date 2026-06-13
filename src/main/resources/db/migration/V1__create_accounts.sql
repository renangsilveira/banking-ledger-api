CREATE TABLE accounts (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number   VARCHAR(20)    NOT NULL UNIQUE,
    holder_name      VARCHAR(255)   NOT NULL,
    account_type     VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    balance_amount   BIGINT         NOT NULL DEFAULT 0,
    balance_currency VARCHAR(3)     NOT NULL DEFAULT 'BRL',
    idempotency_key  VARCHAR(255)   UNIQUE,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_account_type   CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    CONSTRAINT chk_status         CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
    CONSTRAINT chk_balance_amount CHECK (balance_amount >= 0),
    CONSTRAINT chk_currency       CHECK (balance_currency ~ '^[A-Z]{3}$')
);
