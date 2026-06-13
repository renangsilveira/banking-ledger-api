package com.example.ledger.exception;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super(String.format("Account not found: %s", id));
    }
}
