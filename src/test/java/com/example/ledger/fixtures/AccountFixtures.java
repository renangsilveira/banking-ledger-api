package com.example.ledger.fixtures;

import com.example.ledger.dto.request.CreateAccountRequest;
import com.example.ledger.domain.account.AccountType;

public class AccountFixtures {

    public static CreateAccountRequest checkingAccountRequest() {
        return new CreateAccountRequest("John Doe", AccountType.CHECKING, "BRL");
    }

    public static CreateAccountRequest savingsAccountRequest() {
        return new CreateAccountRequest("Jane Doe", AccountType.SAVINGS, "BRL");
    }

    public static CreateAccountRequest usdAccountRequest() {
        return new CreateAccountRequest("John Doe", AccountType.CHECKING, "USD");
    }
}
