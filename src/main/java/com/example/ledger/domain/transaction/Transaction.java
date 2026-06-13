package com.example.ledger.domain.transaction;

import com.example.ledger.domain.account.Money;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", unique = true, nullable = false, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount",   column = @Column(name = "amount",   nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money amount;

    @Column
    private String description;

    @Column(name = "correlation_id")
    private String correlationId;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<TransactionEntry> entries = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Transaction() {}

    public static Transaction create(
            String idempotencyKey,
            TransactionType type,
            Money amount,
            String description,
            String correlationId) {

        Transaction tx = new Transaction();
        tx.idempotencyKey = idempotencyKey;
        tx.type           = type;
        tx.status         = TransactionStatus.COMPLETED;
        tx.amount         = amount;
        tx.description    = description;
        tx.correlationId  = correlationId;
        return tx;
    }

    public void addEntry(TransactionEntry entry) {
        entries.add(entry);
        entry.setTransaction(this);
    }

    public UUID getId()                         { return id; }
    public String getIdempotencyKey()           { return idempotencyKey; }
    public TransactionType getType()            { return type; }
    public TransactionStatus getStatus()        { return status; }
    public Money getAmount()                    { return amount; }
    public String getDescription()              { return description; }
    public String getCorrelationId()            { return correlationId; }
    public List<TransactionEntry> getEntries()  { return Collections.unmodifiableList(entries); }
    public LocalDateTime getCreatedAt()         { return createdAt; }
}
