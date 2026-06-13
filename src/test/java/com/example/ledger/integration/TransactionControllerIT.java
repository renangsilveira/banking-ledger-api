package com.example.ledger.integration;

import com.example.ledger.fixtures.AccountFixtures;
import com.example.ledger.fixtures.TransactionFixtures;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Transaction Controller")
class TransactionControllerIT extends IntegrationTestBase {

    private String accountId;
    private String secondAccountId;

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = baseUrl();

        accountId = given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(AccountFixtures.checkingAccountRequest())
        .when()
            .post("/api/v1/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");

        secondAccountId = given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(AccountFixtures.savingsAccountRequest())
        .when()
            .post("/api/v1/accounts")
        .then()
            .statusCode(201)
            .extract().path("id");
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/deposit")
    class DepositEndpoint {

        @Test
        @DisplayName("should deposit funds and return 201")
        void shouldDeposit() {
            given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(TransactionFixtures.depositRequest(UUID.fromString(accountId), "500.00"))
            .when()
                .post("/api/v1/transactions/deposit")
            .then()
                .statusCode(201)
                .body("type", equalTo("DEPOSIT"))
                .body("status", equalTo("COMPLETED"))
                .body("amount", equalTo(500.0f))
                .body("currency", equalTo("BRL"));
        }

        @Test
        @DisplayName("should update account balance after deposit")
        void shouldUpdateBalanceAfterDeposit() {
            String idempotencyKey = UUID.randomUUID().toString();

            given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(TransactionFixtures.depositRequest(UUID.fromString(accountId), "750.00"))
            .when()
                .post("/api/v1/transactions/deposit")
            .then()
                .statusCode(201);

            given()
            .when()
                .get("/api/v1/accounts/" + accountId + "/balance")
            .then()
                .statusCode(200)
                .body("balance", equalTo(750.0f));
        }

        @Test
        @DisplayName("should return same response for duplicate idempotency key")
        void shouldBeIdempotent() {
            String idempotencyKey = UUID.randomUUID().toString();
            var body = TransactionFixtures.depositRequest(UUID.fromString(accountId), "100.00");

            String firstId = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
            .when()
                .post("/api/v1/transactions/deposit")
            .then()
                .statusCode(201)
                .extract().path("id");

            String secondId = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(body)
            .when()
                .post("/api/v1/transactions/deposit")
            .then()
                .statusCode(201)
                .extract().path("id");

            org.assertj.core.api.Assertions.assertThat(firstId).isEqualTo(secondId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/withdraw")
    class WithdrawEndpoint {

        @Test
        @DisplayName("should withdraw funds successfully")
        void shouldWithdraw() {
            // Deposit first
            given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(TransactionFixtures.depositRequest(UUID.fromString(accountId), "500.00"))
            .when()
                .post("/api/v1/transactions/deposit")
            .then()
                .statusCode(201);

            // Withdraw
            given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(TransactionFixtures.withdrawRequest(UUID.fromString(accountId), "200.00"))
            .when()
                .post("/api/v1/transactions/withdraw")
            .then()
                .statusCode(201)
                .body("type", equalTo("WITHDRAWAL"))
                .body("amount", equalTo(200.0f));
        }

        @Test
        @DisplayName("should return 422 when insufficient funds")
        void shouldReturn422WhenInsufficientFunds() {
            given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(TransactionFixtures.withdrawRequest(UUID.fromString(accountId), "9999.00"))
            .when()
                .post("/api/v1/transactions/withdraw")
            .then()
                .statusCode(422)
                .body("error", equalTo("Unprocessable Entity"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions/transfer")
    class TransferEndpoint {

        @Test
        @DisplayName("should transfer funds between accounts")
        void shouldTransfer() {
            // Deposit to source account
            given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(TransactionFixtures.depositRequest(UUID.fromString(accountId), "1000.00"))
            .when()
                .post("/api/v1/transactions/deposit")
            .then()
                .statusCode(201);

            // Transfer
            given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(TransactionFixtures.transferRequest(
                    UUID.fromString(accountId),
                    UUID.fromString(secondAccountId),
                    "400.00"))
            .when()
                .post("/api/v1/transactions/transfer")
            .then()
                .statusCode(201)
                .body("type", equalTo("TRANSFER"))
                .body("amount", equalTo(400.0f));

            // Verify source balance
            given()
            .when()
                .get("/api/v1/accounts/" + accountId + "/balance")
            .then()
                .body("balance", equalTo(600.0f));

            // Verify destination balance
            given()
            .when()
                .get("/api/v1/accounts/" + secondAccountId + "/balance")
            .then()
                .body("balance", equalTo(400.0f));
        }
    }
}
