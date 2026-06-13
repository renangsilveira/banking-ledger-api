package com.example.ledger.service;

import com.example.ledger.domain.account.Account;
import com.example.ledger.domain.account.AccountType;
import com.example.ledger.domain.account.Money;
import com.example.ledger.domain.transaction.Transaction;
import com.example.ledger.dto.request.DepositRequest;
import com.example.ledger.dto.request.TransferRequest;
import com.example.ledger.dto.request.WithdrawRequest;
import com.example.ledger.dto.response.TransactionResponse;
import com.example.ledger.exception.AccountInactiveException;
import com.example.ledger.exception.AccountNotFoundException;
import com.example.ledger.exception.InsufficientFundsException;
import com.example.ledger.repository.AccountRepository;
import com.example.ledger.repository.TransactionRepository;
import com.example.ledger.service.IdempotencyService.IdempotencyResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private IdempotencyService idempotencyService;

    @InjectMocks
    private TransactionService transactionService;

    private Account sourceAccount;
    private Account destinationAccount;
    private UUID sourceId;
    private UUID destinationId;

    @BeforeEach
    void setUp() {
        sourceId      = UUID.fromString("00000000-0000-0000-0000-000000000001");
        destinationId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        sourceAccount      = Account.open("John Doe", AccountType.CHECKING, "BRL", "key-src");
        destinationAccount = Account.open("Jane Doe", AccountType.CHECKING, "BRL", "key-dst");

        // Simula IDs que seriam gerados pelo JPA
        ReflectionTestUtils.setField(sourceAccount,      "id", sourceId);
        ReflectionTestUtils.setField(destinationAccount, "id", destinationId);

        sourceAccount.credit(Money.of(new BigDecimal("1000.00"), "BRL"));
    }

    private void mockIdempotentOperation() {
        when(idempotencyService.executeIdempotent(
            anyString(), anyString(), any(), any(Supplier.class), any(Supplier.class)))
            .thenAnswer(inv -> {
                Supplier<IdempotencyResult<TransactionResponse>> op = inv.getArgument(3);
                return op.get().body();
            });
    }

    private void mockTransactionSave() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction tx = inv.getArgument(0);
            ReflectionTestUtils.setField(tx, "id", UUID.randomUUID());
            return tx;
        });
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("should deposit funds successfully")
        void shouldDepositSuccessfully() {
            DepositRequest request = new DepositRequest(
                sourceId, new BigDecimal("500.00"), "BRL", "Test deposit");

            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(sourceAccount));
            mockTransactionSave();
            when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            mockIdempotentOperation();

            TransactionResponse response = transactionService.deposit(request, "key-001");

            assertThat(response.type().name()).isEqualTo("DEPOSIT");
            assertThat(response.status().name()).isEqualTo("COMPLETED");
            assertThat(response.amount()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when account does not exist")
        void shouldThrowWhenAccountNotFound() {
            DepositRequest request = new DepositRequest(
                sourceId, new BigDecimal("500.00"), "BRL", "Test");

            when(accountRepository.findById(sourceId)).thenReturn(Optional.empty());
            mockIdempotentOperation();

            assertThatThrownBy(() -> transactionService.deposit(request, "key-001"))
                .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("should withdraw funds successfully")
        void shouldWithdrawSuccessfully() {
            WithdrawRequest request = new WithdrawRequest(
                sourceId, new BigDecimal("300.00"), "BRL", "Test withdrawal");

            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(sourceAccount));
            mockTransactionSave();
            when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            mockIdempotentOperation();

            TransactionResponse response = transactionService.withdraw(request, "key-002");

            assertThat(response.type().name()).isEqualTo("WITHDRAWAL");
            assertThat(response.amount()).isEqualByComparingTo("300.00");
        }

        @Test
        @DisplayName("should throw InsufficientFundsException when balance is too low")
        void shouldThrowWhenInsufficientFunds() {
            WithdrawRequest request = new WithdrawRequest(
                sourceId, new BigDecimal("9999.00"), "BRL", "Test");

            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(sourceAccount));
            mockIdempotentOperation();

            assertThatThrownBy(() -> transactionService.withdraw(request, "key-002"))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("insufficient funds");
        }

        @Test
        @DisplayName("should throw AccountInactiveException when account is blocked")
        void shouldThrowWhenAccountInactive() {
            sourceAccount.block();
            WithdrawRequest request = new WithdrawRequest(
                sourceId, new BigDecimal("100.00"), "BRL", "Test");

            when(accountRepository.findById(sourceId)).thenReturn(Optional.of(sourceAccount));
            mockIdempotentOperation();

            assertThatThrownBy(() -> transactionService.withdraw(request, "key-002"))
                .isInstanceOf(AccountInactiveException.class);
        }
    }

    @Nested
    @DisplayName("transfer")
    class Transfer {

        @Test
        @DisplayName("should transfer funds between accounts successfully")
        void shouldTransferSuccessfully() {
            TransferRequest request = new TransferRequest(
                sourceId, destinationId, new BigDecimal("200.00"), "BRL", "Test transfer");

            when(accountRepository.findByIdWithLock(sourceId))
                .thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithLock(destinationId))
                .thenReturn(Optional.of(destinationAccount));
            mockTransactionSave();
            when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            mockIdempotentOperation();

            TransactionResponse response = transactionService.transfer(request, "key-003");

            assertThat(response.type().name()).isEqualTo("TRANSFER");
            assertThat(response.amount()).isEqualByComparingTo("200.00");
        }

        @Test
        @DisplayName("should throw InsufficientFundsException when source has no funds")
        void shouldThrowWhenInsufficientFundsForTransfer() {
            TransferRequest request = new TransferRequest(
                sourceId, destinationId, new BigDecimal("9999.00"), "BRL", "Test");

            when(accountRepository.findByIdWithLock(sourceId))
                .thenReturn(Optional.of(sourceAccount));
            when(accountRepository.findByIdWithLock(destinationId))
                .thenReturn(Optional.of(destinationAccount));
            mockIdempotentOperation();

            assertThatThrownBy(() -> transactionService.transfer(request, "key-003"))
                .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when source account does not exist")
        void shouldThrowWhenSourceNotFound() {
            TransferRequest request = new TransferRequest(
                sourceId, destinationId, new BigDecimal("100.00"), "BRL", "Test");

            when(accountRepository.findByIdWithLock(sourceId))
                .thenReturn(Optional.empty());
            mockIdempotentOperation();

            assertThatThrownBy(() -> transactionService.transfer(request, "key-003"))
                .isInstanceOf(AccountNotFoundException.class);
        }
    }
}
