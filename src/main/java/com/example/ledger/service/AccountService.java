package com.example.ledger.service;

import com.example.ledger.domain.account.Account;
import com.example.ledger.dto.request.CreateAccountRequest;
import com.example.ledger.dto.response.AccountResponse;
import com.example.ledger.dto.response.BalanceResponse;
import com.example.ledger.exception.AccountNotFoundException;
import com.example.ledger.mapper.AccountMapper;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.service.IdempotencyService.IdempotencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final IdempotencyService idempotencyService;

    public AccountService(AccountRepository accountRepository,
                          IdempotencyService idempotencyService) {
        this.accountRepository = accountRepository;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public AccountResponse create(CreateAccountRequest request, String idempotencyKey) {
        log.info("Creating account for holder={}", request.holderName());
        return idempotencyService.executeIdempotent(
            idempotencyKey,
            "ACCOUNT",
            AccountResponse.class,
            () -> {
                Account account = Account.open(
                    request.holderName(),
                    request.accountType(),
                    request.currency(),
                    idempotencyKey
                );
                account.setAccountNumber(generateAccountNumber());
                Account saved = accountRepository.save(account);
                log.info("Account created id={}", saved.getId());
                AccountResponse response = AccountMapper.toResponse(saved);
                return new IdempotencyResult<>(response, saved.getId(), 201);
            },
            () -> accountRepository.findByIdempotencyKey(idempotencyKey)
                    .map(AccountMapper::toResponse)
                    .orElseThrow(() -> new AccountNotFoundException(UUID.randomUUID()))
        );
    }

    @Transactional(readOnly = true)
    public AccountResponse findById(UUID id) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountMapper.toResponse(account);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID id) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
        return AccountMapper.toBalanceResponse(account);
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.format("%010d", (long) (Math.random() * 10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
