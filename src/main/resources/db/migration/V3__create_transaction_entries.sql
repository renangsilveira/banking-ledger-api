CREATE TABLE transaction_entries (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID         NOT NULL REFERENCES transactions(id),
    account_id     UUID         NOT NULL REFERENCES accounts(id),
    entry_type     VARCHAR(10)  NOT NULL,
    amount         BIGINT       NOT NULL,
    currency       VARCHAR(3)   NOT NULL DEFAULT 'BRL',
    balance_before BIGINT       NOT NULL,
    balance_after  BIGINT       NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_entry_type   CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_entry_amount CHECK (amount > 0)
);
