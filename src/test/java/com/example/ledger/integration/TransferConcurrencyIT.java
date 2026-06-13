package com.example.ledger.integration;

import com.example.ledger.fixtures.AccountFixtures;
import com.example.ledger.fixtures.TransactionFixtures;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Transfer Concurrency")
class TransferConcurrencyIT extends IntegrationTestBase {

    private String sourceAccountId;
    private String destinationAccountId;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = baseUrl();

        sourceAccountId = given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(AccountFixtures.checkingAccountRequest())
        .when()
            .post("/api/v1/accounts")
        .then()
            .extract().path("id");

        destinationAccountId = given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(AccountFixtures.savingsAccountRequest())
        .when()
            .post("/api/v1/accounts")
        .then()
            .extract().path("id");

        // Deposit 1000.00 to source account
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(TransactionFixtures.depositRequest(UUID.fromString(sourceAccountId), "1000.00"))
        .when()
            .post("/api/v1/transactions/deposit")
        .then()
            .statusCode(201);
    }

    @Test
    @DisplayName("should maintain ledger balance consistency under concurrent transfers")
    void shouldMaintainBalanceConsistencyUnderConcurrentTransfers() throws InterruptedException {
        int threads = 10;
        String amountPerTransfer = "100.00";

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            String idempotencyKey = "concurrency-" + UUID.randomUUID();
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    int status = given()
                        .contentType(ContentType.JSON)
                        .header("Idempotency-Key", idempotencyKey)
                        .body(TransactionFixtures.transferRequest(
                            UUID.fromString(sourceAccountId),
                            UUID.fromString(destinationAccountId),
                            amountPerTransfer))
                    .when()
                        .post("/api/v1/transactions/transfer")
                    .then()
                        .extract().statusCode();

                    if (status == 201) successes.incrementAndGet();
                    else failures.incrementAndGet();
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            }));
        }

        latch.countDown();
        executor.shutdown();
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ignored) {}
        }

        float sourceBalance = given()
            .get("/api/v1/accounts/" + sourceAccountId + "/balance")
        .then()
            .extract().path("balance");

        float destinationBalance = given()
            .get("/api/v1/accounts/" + destinationAccountId + "/balance")
        .then()
            .extract().path("balance");

        System.out.printf("Successes: %d | Failures: %d%n", successes.get(), failures.get());
        System.out.printf("Source: %.2f | Destination: %.2f | Total: %.2f%n",
            sourceBalance, destinationBalance, sourceBalance + destinationBalance);

        // Main invariant: balance never negative
        assertThat(sourceBalance)
            .as("Source account balance must never be negative")
            .isGreaterThanOrEqualTo(0.0f);

        // Double-entry invariant: sum of balances = initial deposit (1000.00)
        assertThat(sourceBalance + destinationBalance)
            .as("Total balance must equal initial deposit of 1000.00")
            .isEqualTo(1000.0f);

        // At least one transfer must have been processed
        assertThat(successes.get())
            .as("At least one transfer must succeed")
            .isGreaterThan(0);
    }
}
