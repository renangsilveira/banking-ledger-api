CREATE INDEX idx_accounts_account_number    ON accounts(account_number);
CREATE INDEX idx_accounts_status            ON accounts(status);

CREATE INDEX idx_transactions_idempotency   ON transactions(idempotency_key);
CREATE INDEX idx_transactions_created_at    ON transactions(created_at DESC);

CREATE INDEX idx_entries_transaction_id     ON transaction_entries(transaction_id);
CREATE INDEX idx_entries_account_id         ON transaction_entries(account_id);
CREATE INDEX idx_entries_created_at         ON transaction_entries(created_at DESC);

CREATE INDEX idx_idempotency_key            ON idempotency_keys(key);
CREATE INDEX idx_idempotency_expires_at     ON idempotency_keys(expires_at);
