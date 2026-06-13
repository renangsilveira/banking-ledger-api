package com.example.ledger.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(
    UUID accountId,
    BigDecimal balance,
    String currency
) {}
