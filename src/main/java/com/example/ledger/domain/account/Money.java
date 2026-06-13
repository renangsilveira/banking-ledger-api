package com.example.ledger.domain.account;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Embeddable
public class Money {

    @Column(name = "balance_amount", nullable = false)
    private Long amount;

    @Column(name = "balance_currency", nullable = false, length = 3)
    private String currency;

    protected Money() {}

    private Money(Long amount, String currency) {
        if (amount == null || amount < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        if (currency == null || !currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Currency must be a valid ISO 4217 code (e.g. BRL, USD)");
        }
        this.amount = amount;
        this.currency = currency;
    }

    public static Money of(BigDecimal value, String currency) {
        if (value == null) {
            throw new IllegalArgumentException("Value must not be null");
        }
        long cents = value.setScale(2, RoundingMode.HALF_UP)
                         .multiply(BigDecimal.valueOf(100))
                         .longValue();
        return new Money(cents, currency);
    }

    public static Money ofCents(Long cents, String currency) {
        return new Money(cents, currency);
    }

    public static Money zero(String currency) {
        return new Money(0L, currency);
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount + other.amount, this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        long result = this.amount - other.amount;
        if (result < 0) {
            throw new IllegalArgumentException("Subtraction would result in negative amount");
        }
        return new Money(result, this.currency);
    }

    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.amount < other.amount;
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount > other.amount;
    }

    public boolean isZero() {
        return this.amount == 0L;
    }

    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public Long getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Currency mismatch: %s vs %s", this.currency, other.currency)
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return Objects.equals(amount, money.amount) &&
               Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return currency + " " + toDecimal().toPlainString();
    }
}
