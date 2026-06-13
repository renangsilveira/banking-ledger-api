package com.example.ledger.service;

import com.example.ledger.domain.account.Account;
import com.example.ledger.domain.account.Money;
import com.example.ledger.domain.transaction.EntryType;
import com.example.ledger.domain.transaction.Transaction;
import com.example.ledger.domain.transaction.TransactionEntry;
import com.example.ledger.domain.transaction.TransactionType;
import com.example.ledger.dto.request.DepositRequest;
import com.example.ledger.dto.request.WithdrawRequest;
import com.example.ledger.dto.response.TransactionResponse;
import com.example.ledger.exception.AccountNotFoundException;
import com.example.ledger.mapper.TransactionMapper;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.TransactionRepository;
import com.example.ledger.service.IdempotencyService.IdempotencyResult;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              IdempotencyService idempotencyService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request, String idempotencyKey) {
        return idempotencyService.executeIdempotent(
            idempotencyKey,
            "TRANSACTION",
            TransactionResponse.class,
            () -> {
                Account account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new AccountNotFoundException(request.accountId()));

                Money amount = Money.of(request.amount(), request.currency());
                long balanceBefore = account.getBalance().getAmount();

                account.credit(amount);

                Transaction tx = Transaction.create(
                    idempotencyKey,
                    TransactionType.DEPOSIT,
                    amount,
                    request.description(),
                    MDC.get("correlationId")
                );

                tx.addEntry(TransactionEntry.of(
                    account,
                    EntryType.CREDIT,
                    amount,
                    balanceBefore,
                    account.getBalance().getAmount()
                ));

                // Double-entry: external source debit entry
                tx.addEntry(TransactionEntry.of(
                    account,
                    EntryType.DEBIT,
                    amount,
                    balanceBefore,
                    balanceBefore
                ));

                transactionRepository.save(tx);
                accountRepository.save(account);

                TransactionResponse response = TransactionMapper.toResponse(tx);
                return new IdempotencyResult<>(response, tx.getId(), 201);
            },
            () -> transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(TransactionMapper::toResponse)
                    .orElseThrow(() -> new AccountNotFoundException(UUID.randomUUID()))
        );
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request, String idempotencyKey) {
        return idempotencyService.executeIdempotent(
            idempotencyKey,
            "TRANSACTION",
            TransactionResponse.class,
            () -> {
                Account account = accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new AccountNotFoundException(request.accountId()));

                Money amount = Money.of(request.amount(), request.currency());
                long balanceBefore = account.getBalance().getAmount();

                account.debit(amount);

                Transaction tx = Transaction.create(
                    idempotencyKey,
                    TransactionType.WITHDRAWAL,
                    amount,
                    request.description(),
                    MDC.get("correlationId")
                );

                tx.addEntry(TransactionEntry.of(
                    account,
                    EntryType.DEBIT,
                    amount,
                    balanceBefore,
                    account.getBalance().getAmount()
                ));

                tx.addEntry(TransactionEntry.of(
                    account,
                    EntryType.CREDIT,
                    amount,
                    balanceBefore,
                    balanceBefore
                ));

                transactionRepository.save(tx);
                accountRepository.save(account);

                TransactionResponse response = TransactionMapper.toResponse(tx);
                return new IdempotencyResult<>(response, tx.getId(), 201);
            },
            () -> transactionRepository.findByIdempotencyKey(idempotencyKey)
                    .map(TransactionMapper::toResponse)
                    .orElseThrow(() -> new AccountNotFoundException(UUID.randomUUID()))
        );
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        return transactionRepository.findById(id)
            .map(TransactionMapper::toResponse)
            .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }
}
