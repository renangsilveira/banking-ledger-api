package com.example.ledger.domain.transaction;

import com.example.ledger.domain.account.Account;
import com.example.ledger.domain.account.Money;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transaction_entries")
@EntityListeners(AuditingEntityListener.class)
public class TransactionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false)
    private EntryType entryType;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount",   column = @Column(name = "amount",   nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false, length = 3))
    })
    private Money amount;

    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TransactionEntry() {}

    public static TransactionEntry of(
            Account account,
            EntryType entryType,
            Money amount,
            Long balanceBefore,
            Long balanceAfter) {

        TransactionEntry entry = new TransactionEntry();
        entry.account       = account;
        entry.entryType     = entryType;
        entry.amount        = amount;
        entry.balanceBefore = balanceBefore;
        entry.balanceAfter  = balanceAfter;
        return entry;
    }

    void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public UUID getId()               { return id; }
    public Transaction getTransaction(){ return transaction; }
    public Account getAccount()       { return account; }
    public EntryType getEntryType()   { return entryType; }
    public Money getAmount()          { return amount; }
    public Long getBalanceBefore()    { return balanceBefore; }
    public Long getBalanceAfter()     { return balanceAfter; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
