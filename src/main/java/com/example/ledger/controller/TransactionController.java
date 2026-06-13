package com.example.ledger.controller;

import com.example.ledger.dto.request.DepositRequest;
import com.example.ledger.dto.request.TransferRequest;
import com.example.ledger.dto.request.WithdrawRequest;
import com.example.ledger.dto.response.TransactionResponse;
import com.example.ledger.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Transaction management endpoints")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit funds into an account")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            @Parameter(description = "Unique key to ensure idempotency", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(transactionService.deposit(request, idempotencyKey));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw funds from an account")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @Parameter(description = "Unique key to ensure idempotency", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(transactionService.withdraw(request, idempotencyKey));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds between accounts")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @Parameter(description = "Unique key to ensure idempotency", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(transactionService.transfer(request, idempotencyKey));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }
}
