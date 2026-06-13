package com.example.ledger.dto.response;

import com.example.ledger.domain.account.AccountStatus;
import com.example.ledger.domain.account.AccountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    String accountNumber,
    String holderName,
    AccountType accountType,
    AccountStatus status,
    BigDecimal balance,
    String currency,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
