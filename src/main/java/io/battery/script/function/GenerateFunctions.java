package io.battery.script.function;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import io.battery.util.Money;

/**
 * Random test data generator functions in the {@code gen} namespace, such as numbers,
 * UUIDs, dates, names, addresses, money, text and JSON.
 *
 * @see DefaultGenerateFunctions
 */
public interface GenerateFunctions {
    String NAMESPACE = "gen";

    @Description(value = """
            Returns a pseudorandomly chosen int value between the specified
            origin (inclusive) and the specified bound (exclusive).
            """, volatility = Volatility.Volatile)
    Integer randomInt(int origin, int bound);

    @Description(value = """
            Returns a pseudorandomly chosen long value between the specified
            origin (inclusive) and the specified bound (exclusive).
            """, volatility = Volatility.Volatile)
    Long randomLong(long origin, long bound);

    @Description(value = """
            Returns a pseudorandomly chosen double value between the specified
            origin (inclusive) and the specified bound (exclusive).
            """, volatility = Volatility.Volatile)
    Double randomDouble(double origin, double bound);

    @Description(value = """
            Returns a random UUID v4.
            """, volatility = Volatility.Volatile)
    UUID randomUUID();

    @Description(value = """
            Returns a random boolean.
            """, volatility = Volatility.Volatile)
    Boolean randomBoolean();

    @Description(value = """
            Returns a random ISO-8601 date without time zone.
            """, volatility = Volatility.Volatile)
    LocalDate randomDate();

    @Description(value = """
            Returns a random ISO-8601 time.
            """, volatility = Volatility.Volatile)
    LocalTime randomTime();

    @Description(value = """
            Returns a random ISO-8601 date and time.
            """, volatility = Volatility.Volatile)
    LocalDateTime randomDateTime();

    @Description(value = """
            Returns a random BigDecimal in the 0 - 2^16 span.
            """, volatility = Volatility.Volatile)
    BigDecimal randomBigDecimal();

    @Description(value = """
            Returns a random BigDecimal in the specified bound.
            """, volatility = Volatility.Volatile)
    BigDecimal randomBigDecimal(double origin, double bound);

    @Description(value = """
            Returns a random BigDecimal in the specified bound and scale.
            """, volatility = Volatility.Volatile)
    BigDecimal randomBigDecimal(double origin, double bound, int scale);

    @Description(value = """
            Returns a random English first name.
            """, volatility = Volatility.Volatile)
    String randomFirstName();

    @Description(value = """
            Returns a random English surname.
            """, volatility = Volatility.Volatile)
    String randomLastName();

    @Description(value = """
            Returns a random city name.
            """, volatility = Volatility.Volatile)
    String randomCity();

    @Description(value = """
            Returns a random phone number.
            """, volatility = Volatility.Volatile)
    String randomPhoneNumber();

    @Description(value = """
            Returns a random country name.
            """, volatility = Volatility.Volatile)
    String randomCountry();

    @Description(value = """
            Returns a random street address.
            """, volatility = Volatility.Volatile)
    String randomAddress();

    @Description(value = """
            Returns a random ISO-4217 currency code.
            """, volatility = Volatility.Volatile)
    String randomCurrency();

    @Description(value = """
            Returns a random US state name.
            """, volatility = Volatility.Volatile)
    String randomState();

    @Description(value = """
            Returns a random US state code.
            """, volatility = Volatility.Volatile)
    String randomStateCode();

    @Description(value = """
            Returns a random postal zip code.
            """, volatility = Volatility.Volatile)
    String randomZipCode();

    @Description(value = """
            Returns a random email.
            """, volatility = Volatility.Volatile)
    String randomEmail();

    @Description(value = """
            Returns a random Latin lore ipsum novel.
            """, volatility = Volatility.Volatile)
    String randomLoreIpsum(int min, int max, boolean paragraphs);

    @Description(value = """
            Returns a random json payload.
            """, volatility = Volatility.Volatile)
    String randomJson(int rootItems, int nestedItems);

    @Description(value = """
            Returns a random string.
            """, volatility = Volatility.Volatile)
    String randomString(int min, int max);

    @Description(value = """
            Returns a random string.
            """, volatility = Volatility.Volatile)
    String randomString(int min);

    @Description(value = """
            Returns a random word.
            """, volatility = Volatility.Volatile)
    String randomWord(int min);

    @Description(value = """
            Returns a random byte array.
            """, volatility = Volatility.Volatile)
    byte[] randomBytes(int min);

    @Description(value = """
            Returns a random amount of money in a random currency.
            """, volatility = Volatility.Volatile)
    Money randomMoney();

    @Description(value = """
            Returns a random amount of money in the given ISO 4217 currency code.
            """, volatility = Volatility.Volatile)
    Money randomMoney(String currency);
}
