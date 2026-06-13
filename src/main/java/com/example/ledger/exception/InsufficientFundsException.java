package com.example.ledger.exception;

import com.example.ledger.domain.account.Money;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID accountId, Money available, Money requested) {
        super(String.format(
            "Account %s has insufficient funds. Available: %s, Requested: %s",
            accountId, available, requested
        ));
    }
}
