package com.example.ledger.service;

import com.example.ledger.dto.response.StatementResponse;
import com.example.ledger.exception.AccountNotFoundException;
import com.example.ledger.mapper.StatementMapper;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.TransactionEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BalanceService {

    private final AccountRepository accountRepository;
    private final TransactionEntryRepository transactionEntryRepository;

    public BalanceService(AccountRepository accountRepository,
                          TransactionEntryRepository transactionEntryRepository) {
        this.accountRepository = accountRepository;
        this.transactionEntryRepository = transactionEntryRepository;
    }

    @Transactional(readOnly = true)
    public Page<StatementResponse> getStatement(
            UUID accountId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        LocalDateTime effectiveFrom = from != null ? from : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime effectiveTo   = to   != null ? to   : LocalDateTime.now().plusYears(1);

        return transactionEntryRepository
            .findByAccountId(accountId, effectiveFrom, effectiveTo, pageable)
            .map(StatementMapper::toResponse);
    }
}
