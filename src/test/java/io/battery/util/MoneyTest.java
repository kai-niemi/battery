package io.battery.util;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.battery.util.Money.EUR;
import static io.battery.util.Money.SEK;
import static io.battery.util.Money.USD;
import static io.battery.util.Money.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit-test")
public class MoneyTest {
    @Test
    public void givenDifferentCurrencies_whenUsingConstructors_thenExpectFractions() {
        Money sek = Money.of("0.00", SEK);
        assertThat(sek.toString()).isEqualTo("0.00 SEK");

        sek = Money.of(BigDecimal.valueOf(0.000), SEK);
        assertThat(sek.toString()).isEqualTo("0.00 SEK");

        Money eur = Money.of(new BigDecimal("10.50"), "EUR");
        assertThat(eur.getCurrency()).isEqualTo(EUR);
        assertThat(eur.getAmount()).isEqualTo(new BigDecimal("10.50"));

        Money usd = Money.of(new BigDecimal("20.00"), USD);
        assertThat(usd.getCurrency()).isEqualTo(USD);

        Money jpy = Money.of(new BigDecimal("100"), Currency.getInstance("JPY"));
        assertThat(jpy.getAmount()).isEqualTo(new BigDecimal("100"));

        Money zeroSek = Money.zero("SEK");
        assertThat(zeroSek).isEqualTo(of("0.00", SEK));

        Money zeroUsd = Money.zero(USD);
        assertThat(zeroUsd).isEqualTo(of("0.00", USD));
    }

    @Test
    public void testParse() {
        Money parsed = Money.parse("15.50 SEK");
        assertThat(parsed).isEqualTo(of("15.50", SEK));

        assertThatThrownBy(() -> Money.parse("15.50")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.parse("15.50 SEK EXTRA")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testNullAndIllegalArguments() {
        assertThatThrownBy(() -> Money.of((String) null, "SEK")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of("10.00", (Currency) null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Money.of("10.0", "SEK")).isInstanceOf(
                IllegalArgumentException.class); // Wrong scale 1 != 2
    }

    @Test
    public void whenBinaryArithmetics_thenSucceed() {
        assertThat(of("80.00", SEK).plus(of("20.00", SEK)))
                .isEqualTo(of("100.00", SEK));

        assertThat(of("10.05", SEK).plus(of("9.95", SEK), of("-0.50", SEK)))
                .isEqualTo(of("19.50", SEK));

        assertThat(of("100.00", SEK).minus(of("20.00", SEK)))
                .isEqualTo(of("80.00", SEK));

        assertThat(of("10.00", SEK).multiply(10))
                .isEqualTo(of("100.00", SEK));

        assertThat(of("10.00", SEK).multiply(2.5))
                .isEqualTo(of("25.00", SEK));

        assertThat(of("10.00", SEK).multiply(new BigDecimal("3.00")))
                .isEqualTo(of("30.00", SEK));

        assertThat(of("100.00", SEK).divide(5))
                .isEqualTo(of("20.00", SEK));

        assertThat(of("100.00", SEK).divide(5.0))
                .isEqualTo(of("20.00", SEK));

        assertThat(of("100.00", SEK).divide(new BigDecimal("5.00")))
                .isEqualTo(of("20.00", SEK));

        assertThat(of("100.00", SEK).divideAndRound(6))
                .isEqualTo(of("16.67", SEK));

        assertThat(of("100.00", SEK).remainder(100))
                .isEqualTo(of("0.00", SEK));

        assertThatThrownBy(() -> of("10.00", SEK).multiply((BigDecimal) null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> of("10.00", SEK).divide((BigDecimal) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void whenComparisonOperators_thenSucceed() {
        assertThat(of("110.00", SEK).isGreaterThan(of("100.00", SEK))).isTrue();
        assertThat(of("100.00", SEK).isGreaterThan(of("100.00", SEK))).isFalse();

        assertThat(of("100.00", SEK).isGreaterThanOrEqualTo(of("100.00", SEK))).isTrue();
        assertThat(of("99.00", SEK).isGreaterThanOrEqualTo(of("100.00", SEK))).isFalse();

        assertThat(of("99.00", SEK).isLessThan(of("100.00", SEK))).isTrue();
        assertThat(of("100.00", SEK).isLessThan(of("100.00", SEK))).isFalse();

        assertThat(of("100.00", SEK).isLessThanOrEqualTo(of("100.00", SEK))).isTrue();
        assertThat(of("101.00", SEK).isLessThanOrEqualTo(of("100.00", SEK))).isFalse();

        assertThat(of("100.00", SEK).isSameCurrency(of("100.00", SEK))).isTrue();
        assertThat(of("100.00", SEK).isSameCurrency(of("100.00", USD))).isFalse();

        assertThat(of("-1.00", SEK).isNegative()).isTrue();
        assertThat(of("0.00", SEK).isNegative()).isFalse();
        assertThat(of("1.00", SEK).isNegative()).isFalse();
    }

    @Test
    public void testEqualsAndHashCodeAndCompareTo() {
        Money m1 = of("50.00", SEK);
        Money m2 = of("50.00", SEK);
        Money m3 = of("60.00", SEK);
        Money m4 = of("50.00", USD);

        assertThat(m1).isEqualTo(m1);
        assertThat(m2).isEqualTo(m1);
        assertThat(m2.hashCode()).isEqualTo(m1.hashCode());

        assertThat(m1).isNotNull();
        assertThat(m1).isNotEqualTo("not-money");
        assertThat(m1).isNotEqualTo(m3);
        assertThat(m1).isNotEqualTo(m4);

        assertThat(m1.compareTo(m2)).isEqualTo(0);
        assertThat(m1.compareTo(m3)).isLessThan(0);
        assertThat(m3.compareTo(m1)).isGreaterThan(0);
    }

    @Test
    public void whenUnaryOperators_thenExpectImmutability() {
        Money m = Money.of("15.00", "SEK");
        assertThat(m.negate().negate()).isNotSameAs(m);
        assertThat(m.negate().negate()).isEqualTo(m);
        assertThat(m.negate()).isEqualTo(of("-15.00", SEK));
    }

    @Test
    public void whenMixingCurrencies_thenFail() {
        assertThatThrownBy(() -> Money.of("15.00", "SEK").minus(Money.of("0.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> Money.of("15.00", "SEK").plus(Money.of("0.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> Money.of("15.00", "SEK").isGreaterThan(Money.of("0.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> Money.of("15.00", "SEK").isGreaterThanOrEqualTo(Money.of("0.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> Money.of("15.00", "SEK").isLessThan(Money.of("0.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
        assertThatThrownBy(() -> Money.of("15.00", "SEK").isLessThanOrEqualTo(Money.of("0.00", "USD")))
                .isInstanceOf(CurrencyMismatchException.class);
    }
}
