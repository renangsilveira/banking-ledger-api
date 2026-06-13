package com.example.ledger.mapper;

import com.example.ledger.domain.transaction.Transaction;
import com.example.ledger.dto.response.TransactionResponse;

public class TransactionMapper {

    private TransactionMapper() {}

    public static TransactionResponse toResponse(Transaction transaction) {
        return new TransactionResponse(
            transaction.getId(),
            transaction.getIdempotencyKey(),
            transaction.getType(),
            transaction.getStatus(),
            transaction.getAmount().toDecimal(),
            transaction.getAmount().getCurrency(),
            transaction.getDescription(),
            transaction.getCorrelationId(),
            transaction.getCreatedAt()
        );
    }
}
