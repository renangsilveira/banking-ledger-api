package com.example.ledger.dto.response;

import com.example.ledger.domain.transaction.EntryType;
import com.example.ledger.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StatementResponse(
    UUID entryId,
    UUID transactionId,
    TransactionType transactionType,
    EntryType entryType,
    BigDecimal amount,
    String currency,
    BigDecimal balanceBefore,
    BigDecimal balanceAfter,
    String description,
    LocalDateTime createdAt
) {}
