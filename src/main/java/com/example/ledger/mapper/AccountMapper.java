package com.example.ledger.mapper;

import com.example.ledger.domain.account.Account;
import com.example.ledger.dto.response.AccountResponse;
import com.example.ledger.dto.response.BalanceResponse;

public class AccountMapper {

    private AccountMapper() {}

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getHolderName(),
            account.getAccountType(),
            account.getStatus(),
            account.getBalance().toDecimal(),
            account.getBalance().getCurrency(),
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }

    public static BalanceResponse toBalanceResponse(Account account) {
        return new BalanceResponse(
            account.getId(),
            account.getBalance().toDecimal(),
            account.getBalance().getCurrency()
        );
    }
}
