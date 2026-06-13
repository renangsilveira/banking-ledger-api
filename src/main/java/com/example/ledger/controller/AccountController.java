package com.example.ledger.controller;

import com.example.ledger.dto.request.CreateAccountRequest;
import com.example.ledger.dto.response.AccountResponse;
import com.example.ledger.dto.response.BalanceResponse;
import com.example.ledger.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Account management endpoints")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @Operation(summary = "Create a new bank account")
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request,
            @Parameter(description = "Unique key to ensure idempotency", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        AccountResponse response = accountService.create(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponse> findById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(accountService.findById(id));
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get current balance of an account")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable UUID id) {

        return ResponseEntity.ok(accountService.getBalance(id));
    }
}
