package com.example.ledger.fixtures;

import com.example.ledger.dto.request.DepositRequest;
import com.example.ledger.dto.request.TransferRequest;
import com.example.ledger.dto.request.WithdrawRequest;

import java.math.BigDecimal;
import java.util.UUID;

public class TransactionFixtures {

    public static DepositRequest depositRequest(UUID accountId, String amount) {
        return new DepositRequest(accountId, new BigDecimal(amount), "BRL", "Test deposit");
    }

    public static WithdrawRequest withdrawRequest(UUID accountId, String amount) {
        return new WithdrawRequest(accountId, new BigDecimal(amount), "BRL", "Test withdrawal");
    }

    public static TransferRequest transferRequest(UUID sourceId, UUID destinationId, String amount) {
        return new TransferRequest(sourceId, destinationId, new BigDecimal(amount), "BRL", "Test transfer");
    }
}
