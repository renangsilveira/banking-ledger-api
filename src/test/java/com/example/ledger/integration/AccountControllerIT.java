package com.example.ledger.integration;

import com.example.ledger.fixtures.AccountFixtures;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@DisplayName("Account Controller")
class AccountControllerIT extends IntegrationTestBase {

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = baseUrl();
    }

    @Nested
    @DisplayName("POST /api/v1/accounts")
    class CreateAccount {

        @Test
        @DisplayName("should create account and return 201")
        void shouldCreateAccount() {
            given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(AccountFixtures.checkingAccountRequest())
            .when()
                .post("/api/v1/accounts")
            .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("accountNumber", notNullValue())
                .body("holderName", equalTo("John Doe"))
                .body("accountType", equalTo("CHECKING"))
                .body("status", equalTo("ACTIVE"))
                .body("balance", equalTo(0.0f))
                .body("currency", equalTo("BRL"));
        }

        @Test
        @DisplayName("should return same response for duplicate idempotency key")
        void shouldBeIdempotent() {
            String idempotencyKey = UUID.randomUUID().toString();

            String firstId = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(AccountFixtures.checkingAccountRequest())
            .when()
                .post("/api/v1/accounts")
            .then()
                .statusCode(201)
                .extract().path("id");

            String secondId = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", idempotencyKey)
                .body(AccountFixtures.checkingAccountRequest())
            .when()
                .post("/api/v1/accounts")
            .then()
                .statusCode(201)
                .extract().path("id");

            org.assertj.core.api.Assertions.assertThat(firstId).isEqualTo(secondId);
        }

        @Test
        @DisplayName("should return 400 when holder name is blank")
        void shouldReturn400WhenHolderNameBlank() {
            given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("{\"holderName\": \"\", \"accountType\": \"CHECKING\", \"currency\": \"BRL\"}")
            .when()
                .post("/api/v1/accounts")
            .then()
                .statusCode(400)
                .body("violations.holderName", notNullValue());
        }

        @Test
        @DisplayName("should return 400 when Idempotency-Key header is missing")
        void shouldReturn400WhenIdempotencyKeyMissing() {
            given()
                .contentType(ContentType.JSON)
                .body(AccountFixtures.checkingAccountRequest())
            .when()
                .post("/api/v1/accounts")
            .then()
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/accounts/{id}")
    class GetAccount {

        @Test
        @DisplayName("should return account by id")
        void shouldReturnAccount() {
            String id = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(AccountFixtures.checkingAccountRequest())
            .when()
                .post("/api/v1/accounts")
            .then()
                .extract().path("id");

            given()
            .when()
                .get("/api/v1/accounts/" + id)
            .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("status", equalTo("ACTIVE"));
        }

        @Test
        @DisplayName("should return 404 for unknown account")
        void shouldReturn404() {
            given()
            .when()
                .get("/api/v1/accounts/" + UUID.randomUUID())
            .then()
                .statusCode(404)
                .body("error", equalTo("Not Found"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/accounts/{id}/balance")
    class GetBalance {

        @Test
        @DisplayName("should return zero balance for new account")
        void shouldReturnZeroBalance() {
            String id = given()
                .contentType(ContentType.JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body(AccountFixtures.checkingAccountRequest())
            .when()
                .post("/api/v1/accounts")
            .then()
                .extract().path("id");

            given()
            .when()
                .get("/api/v1/accounts/" + id + "/balance")
            .then()
                .statusCode(200)
                .body("accountId", equalTo(id))
                .body("balance", equalTo(0.0f))
                .body("currency", equalTo("BRL"));
        }
    }
}
