package com.example.ledger.mapper;

import com.example.ledger.domain.account.Money;
import com.example.ledger.domain.transaction.TransactionEntry;
import com.example.ledger.dto.response.StatementResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class StatementMapper {

    private StatementMapper() {}

    public static StatementResponse toResponse(TransactionEntry entry) {
        return new StatementResponse(
            entry.getId(),
            entry.getTransaction().getId(),
            entry.getTransaction().getType(),
            entry.getEntryType(),
            entry.getAmount().toDecimal(),
            entry.getAmount().getCurrency(),
            centsToDecimal(entry.getBalanceBefore()),
            centsToDecimal(entry.getBalanceAfter()),
            entry.getTransaction().getDescription(),
            entry.getCreatedAt()
        );
    }

    private static BigDecimal centsToDecimal(Long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
