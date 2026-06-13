CREATE TABLE idempotency_keys (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    key           VARCHAR(255)  NOT NULL UNIQUE,
    entity_type   VARCHAR(50)   NOT NULL,
    entity_id     UUID,
    http_status   INT           NOT NULL,
    response_body TEXT          NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    expires_at    TIMESTAMP     NOT NULL DEFAULT NOW() + INTERVAL '24 hours'
);
