package com.example.ledger.domain.account;

import com.example.ledger.exception.AccountInactiveException;
import com.example.ledger.exception.InsufficientFundsException;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener.class)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_number", unique = true, nullable = false, updatable = false)
    private String accountNumber;

    @Column(name = "holder_name", nullable = false)
    private String holderName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount",   column = @Column(name = "balance_amount",   nullable = false)),
        @AttributeOverride(name = "currency", column = @Column(name = "balance_currency", nullable = false, length = 3))
    })
    private Money balance;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Account() {}

    public static Account open(String holderName, AccountType accountType, String currency, String idempotencyKey) {
        Account account = new Account();
        account.holderName     = holderName;
        account.accountType    = accountType;
        account.status         = AccountStatus.ACTIVE;
        account.balance        = Money.zero(currency);
        account.idempotencyKey = idempotencyKey;
        return account;
    }

    public void debit(Money amount) {
        ensureActive();
        if (balance.isLessThan(amount)) {
            throw new InsufficientFundsException(this.id, this.balance, amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(Money amount) {
        ensureActive();
        this.balance = this.balance.add(amount);
    }

    public void block() {
        this.status = AccountStatus.BLOCKED;
    }

    public void deactivate() {
        this.status = AccountStatus.INACTIVE;
    }

    private void ensureActive() {
        if (this.status != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(this.id);
        }
    }

    public UUID getId()                { return id; }
    public String getAccountNumber()   { return accountNumber; }
    public String getHolderName()      { return holderName; }
    public AccountType getAccountType(){ return accountType; }
    public AccountStatus getStatus()   { return status; }
    public Money getBalance()          { return balance; }
    public String getIdempotencyKey()  { return idempotencyKey; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public LocalDateTime getUpdatedAt(){ return updatedAt; }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
}
