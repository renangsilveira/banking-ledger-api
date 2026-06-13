package com.example.ledger.dto.request;

import com.example.ledger.domain.account.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(

    @NotBlank(message = "Holder name is required")
    @Size(min = 2, max = 255, message = "Holder name must be between 2 and 255 characters")
    String holderName,

    @NotNull(message = "Account type is required")
    AccountType accountType,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code (e.g. BRL, USD)")
    String currency
) {}
