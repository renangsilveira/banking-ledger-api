package com.example.ledger.exception;

import java.util.UUID;

public class AccountInactiveException extends RuntimeException {

    public AccountInactiveException(UUID accountId) {
        super(String.format("Account %s is not active", accountId));
    }
}
