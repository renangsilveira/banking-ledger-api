package com.example.ledger.dto.response;

import com.example.ledger.domain.transaction.TransactionStatus;
import com.example.ledger.domain.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    String idempotencyKey,
    TransactionType type,
    TransactionStatus status,
    BigDecimal amount,
    String currency,
    String description,
    String correlationId,
    LocalDateTime createdAt
) {}
