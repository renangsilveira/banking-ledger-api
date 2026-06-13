package com.example.ledger.domain.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money value object")
class MoneyTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        @DisplayName("should create Money from BigDecimal value")
        void shouldCreateFromBigDecimal() {
            Money money = Money.of(new BigDecimal("100.50"), "BRL");

            assertThat(money.getAmount()).isEqualTo(10050L);
            assertThat(money.getCurrency()).isEqualTo("BRL");
            assertThat(money.toDecimal()).isEqualByComparingTo("100.50");
        }

        @Test
        @DisplayName("should create zero Money")
        void shouldCreateZero() {
            Money money = Money.zero("BRL");

            assertThat(money.getAmount()).isZero();
            assertThat(money.isZero()).isTrue();
        }

        @Test
        @DisplayName("should reject null value")
        void shouldRejectNullValue() {
            assertThatThrownBy(() -> Money.of(null, "BRL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Value must not be null");
        }

        @Test
        @DisplayName("should reject invalid currency")
        void shouldRejectInvalidCurrency() {
            assertThatThrownBy(() -> Money.of(BigDecimal.TEN, "brl"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency must be a valid ISO 4217 code");
        }

        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            assertThatThrownBy(() -> Money.ofCents(-1L, "BRL"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be non-negative");
        }
    }

    @Nested
    @DisplayName("arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("should add two Money values")
        void shouldAdd() {
            Money a = Money.of(new BigDecimal("100.00"), "BRL");
            Money b = Money.of(new BigDecimal("50.75"), "BRL");

            Money result = a.add(b);

            assertThat(result.toDecimal()).isEqualByComparingTo("150.75");
        }

        @Test
        @DisplayName("should subtract two Money values")
        void shouldSubtract() {
            Money a = Money.of(new BigDecimal("100.00"), "BRL");
            Money b = Money.of(new BigDecimal("30.25"), "BRL");

            Money result = a.subtract(b);

            assertThat(result.toDecimal()).isEqualByComparingTo("69.75");
        }

        @Test
        @DisplayName("should reject subtraction resulting in negative amount")
        void shouldRejectNegativeSubtraction() {
            Money a = Money.of(new BigDecimal("10.00"), "BRL");
            Money b = Money.of(new BigDecimal("20.00"), "BRL");

            assertThatThrownBy(() -> a.subtract(b))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative amount");
        }

        @Test
        @DisplayName("should reject arithmetic with different currencies")
        void shouldRejectCurrencyMismatch() {
            Money brl = Money.of(new BigDecimal("100.00"), "BRL");
            Money usd = Money.of(new BigDecimal("100.00"), "USD");

            assertThatThrownBy(() -> brl.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");
        }
    }

    @Nested
    @DisplayName("comparison")
    class Comparison {

        @Test
        @DisplayName("should compare Money values correctly")
        void shouldCompare() {
            Money small = Money.of(new BigDecimal("10.00"), "BRL");
            Money large = Money.of(new BigDecimal("20.00"), "BRL");

            assertThat(small.isLessThan(large)).isTrue();
            assertThat(large.isGreaterThan(small)).isTrue();
            assertThat(large.isLessThan(small)).isFalse();
        }

        @Test
        @DisplayName("should consider equal Money values as equal")
        void shouldBeEqual() {
            Money a = Money.of(new BigDecimal("100.00"), "BRL");
            Money b = Money.of(new BigDecimal("100.00"), "BRL");

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }

    @Nested
    @DisplayName("formatting")
    class Formatting {

        @Test
        @DisplayName("should format Money as string")
        void shouldFormatAsString() {
            Money money = Money.of(new BigDecimal("1234.56"), "BRL");

            assertThat(money.toString()).isEqualTo("BRL 1234.56");
        }
    }
}
