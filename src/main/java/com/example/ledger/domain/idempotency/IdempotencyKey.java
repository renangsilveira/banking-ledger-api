package com.example.ledger.domain.idempotency;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@EntityListeners(AuditingEntityListener.class)
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "key", unique = true, nullable = false, updatable = false)
    private String key;

    @Column(name = "entity_type", nullable = false, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(name = "http_status", nullable = false, updatable = false)
    private Integer httpStatus;

    @Column(name = "response_body", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String responseBody;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    protected IdempotencyKey() {}

    public static IdempotencyKey of(
            String key,
            String entityType,
            UUID entityId,
            Integer httpStatus,
            String responseBody) {

        IdempotencyKey ik = new IdempotencyKey();
        ik.key          = key;
        ik.entityType   = entityType;
        ik.entityId     = entityId;
        ik.httpStatus   = httpStatus;
        ik.responseBody = responseBody;
        ik.expiresAt    = LocalDateTime.now().plusHours(24);
        return ik;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    public UUID getId()            { return id; }
    public String getKey()         { return key; }
    public String getEntityType()  { return entityType; }
    public UUID getEntityId()      { return entityId; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getResponseBody(){ return responseBody; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
