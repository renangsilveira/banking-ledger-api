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

@DisplayName("Statement Controller")
class StatementControllerIT extends IntegrationTestBase {

    private String accountId;

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

        // Deposit R$ 1000
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(TransactionFixtures.depositRequest(UUID.fromString(accountId), "1000.00"))
        .when()
            .post("/api/v1/transactions/deposit")
        .then()
            .statusCode(201);

        // Withdraw R$ 250
        given()
            .contentType(ContentType.JSON)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .body(TransactionFixtures.withdrawRequest(UUID.fromString(accountId), "250.00"))
        .when()
            .post("/api/v1/transactions/withdraw")
        .then()
            .statusCode(201);
    }

    @Nested
    @DisplayName("GET /api/v1/accounts/{id}/statement")
    class GetStatement {

        @Test
        @DisplayName("should return paginated statement with all entries")
        void shouldReturnPaginatedStatement() {
            given()
            .when()
                .get("/api/v1/accounts/" + accountId + "/statement")
            .then()
                .statusCode(200)
                .body("content", hasSize(greaterThanOrEqualTo(2)))
                .body("totalElements", greaterThanOrEqualTo(2))
                .body("content[0].entryId", notNullValue())
                .body("content[0].transactionId", notNullValue())
                .body("content[0].transactionType", notNullValue())
                .body("content[0].entryType", notNullValue())
                .body("content[0].amount", notNullValue())
                .body("content[0].balanceBefore", notNullValue())
                .body("content[0].balanceAfter", notNullValue());
        }

        @Test
        @DisplayName("should return entries in descending order by date")
        void shouldReturnEntriesInDescendingOrder() {
            given()
                .queryParam("page", 0)
                .queryParam("size", 10)
            .when()
                .get("/api/v1/accounts/" + accountId + "/statement")
            .then()
                .statusCode(200)
                .body("content[0].transactionType", equalTo("WITHDRAWAL"))
                .body("first", equalTo(true));
        }

        @Test
        @DisplayName("should return correct pagination metadata")
        void shouldReturnCorrectPaginationMetadata() {
            given()
                .queryParam("page", 0)
                .queryParam("size", 2)
            .when()
                .get("/api/v1/accounts/" + accountId + "/statement")
            .then()
                .statusCode(200)
                .body("size", equalTo(2))
                .body("number", equalTo(0))
                .body("first", equalTo(true));
        }

        @Test
        @DisplayName("should return 404 for unknown account")
        void shouldReturn404ForUnknownAccount() {
            given()
            .when()
                .get("/api/v1/accounts/" + UUID.randomUUID() + "/statement")
            .then()
                .statusCode(404)
                .body("error", equalTo("Not Found"));
        }

        @Test
        @DisplayName("should return empty content for account with no transactions")
        void shouldReturnEmptyForAccountWithNoTransactions() {
            String emptyAccountId = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(AccountFixtures.savingsAccountRequest())
            .when()
                .post("/api/v1/accounts")
            .then()
                .statusCode(201)
                .extract().path("id");

            given()
            .when()
                .get("/api/v1/accounts/" + emptyAccountId + "/statement")
            .then()
                .statusCode(200)
                .body("content", hasSize(0))
                .body("totalElements", equalTo(0))
                .body("empty", equalTo(true));
        }
    }
}
