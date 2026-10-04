package io.battery.script.function;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import io.battery.util.Money;
import io.battery.util.RandomData;
import static io.battery.util.RandomData.selectRandom;

/**
 * Default {@link GenerateFunctions} implementation, loading word lists from the classpath
 * once and using {@code ThreadLocalRandom}, so that a single instance is safe to share
 * across virtual user threads.
 */
public class DefaultGenerateFunctions implements GenerateFunctions {
    private static final Logger logger = LoggerFactory.getLogger(RandomData.class);

    private static final JsonMapper jsonMapper = JsonMapper
            .builder()
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .build();

    private static final List<String> firstNames = new ArrayList<>();

    private static final List<String> lastNames = new ArrayList<>();

    private static final List<String> cities = new ArrayList<>();

    private static final List<String> countries = new ArrayList<>();

    private static final List<String> streets = new ArrayList<>();

    private static final List<String> currencies = new ArrayList<>();

    private static final List<String> states = new ArrayList<>();

    private static final List<String> stateCodes = new ArrayList<>();

    private static final List<String> lorem = new ArrayList<>();

    static {
        firstNames.addAll(readLines("firstnames.txt"));
        lastNames.addAll(readLines(("surnames.txt")));
        cities.addAll(readLines(("cities.txt")));
        streets.addAll(readLines(("streets.txt")));
        states.addAll(readLines(("states.txt")));
        stateCodes.addAll(readLines(("state_code.txt")));
        lorem.addAll(readLines(("lorem.txt")));

        Arrays.stream(Locale.getAvailableLocales())
                .filter(locale -> StringUtils.hasLength(locale.getDisplayCountry(Locale.US)))
                .map(locale -> locale.getDisplayCountry(Locale.US))
                .forEach(countries::add);

        Currency.getAvailableCurrencies().stream()
                .map(Currency::getCurrencyCode)
                .forEach(currencies::add);
    }

    private static List<String> readLines(String fileName) {
        try (InputStream resource = new ClassPathResource("random/" + fileName).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(resource))) {
            return reader.lines().collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("", e);
            return Collections.emptyList();
        }
    }

    private static final char[] VOWELS = "aeiou".toCharArray();

    private static final char[] CONSONANTS = "bcdfghjklmnpqrstvwxyz".toCharArray();

    private static class LoreIpsum {
        private final int min;

        private final int max;

        private final boolean paragraphs;

        public LoreIpsum(int min, int max, boolean paragraphs) {
            this.min = min;
            this.max = max;
            this.paragraphs = paragraphs;
        }

        public String generate() {
            return StringUtils.capitalize(paragraphs
                    ? getParagraphs(min, max)
                    : getWords(getCount(min, max), false));
        }

        public String getParagraphs(int min, int max) {
            StringBuilder sb = new StringBuilder();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (int j = 0; j < getCount(min, max); j++) {
                for (int i = 0; i < random.nextInt(5) + 2; i++) {
                    sb.append(StringUtils.capitalize(getWords(1, false)))
                            .append(getWords(getCount(2, 20), false))
                            .append(". ");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        }

        private int getCount(int min, int max) {
            min = Math.max(0, min);
            if (max < min) {
                max = min;
            }
            return max != min ? ThreadLocalRandom.current().nextInt(max - min) + min : min;
        }

        private String getWords(int count, boolean capitalize) {
            StringBuilder sb = new StringBuilder();

            int wordCount = 0;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            while (wordCount < count) {
                String word = lorem.get(random.nextInt(lorem.size()));
                if (capitalize) {
                    if (wordCount == 0 || word.length() > 3) {
                        word = StringUtils.capitalize(word);
                    }
                }
                sb.append(word);
                sb.append(" ");
                wordCount++;
            }
            return sb.toString().trim();
        }
    }

    @Override
    public UUID randomUUID() {
        return UUID.randomUUID();
    }

    @Override
    public Money randomMoney() {
        return Money.of(randomBigDecimal(), randomCurrency());
    }

    @Override
    public Money randomMoney(String currency) {
        return Money.of(randomBigDecimal(), currency);
    }

    @Override
    public Boolean randomBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    @Override
    public Integer randomInt(int start, int end) {
        return ThreadLocalRandom.current().nextInt(start, end);
    }

    @Override
    public Long randomLong(long start, long end) {
        return ThreadLocalRandom.current().nextLong(start, end);
    }

    @Override
    public Double randomDouble(double start, double end) {
        return ThreadLocalRandom.current().nextDouble(start, end);
    }

    @Override
    public LocalDate randomDate() {
        return LocalDate.now().plusDays(ThreadLocalRandom.current().nextBoolean()
                ? randomInt(0, 90) : -randomInt(0, 90));
    }

    @Override
    public LocalTime randomTime() {
        return LocalTime.now().plusHours(ThreadLocalRandom.current().nextBoolean()
                ? randomInt(0, 24) : -randomInt(0, 24));
    }

    @Override
    public LocalDateTime randomDateTime() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return LocalDateTime.now()
                .plusDays(r.nextBoolean() ? randomInt(0, 90) : -randomInt(0, 90))
                .plusHours(r.nextBoolean() ? randomInt(0, 24) : -randomInt(0, 24));
    }

    @Override
    public BigDecimal randomBigDecimal() {
        return randomBigDecimal(0, 1 << 16);
    }

    @Override
    public BigDecimal randomBigDecimal(double origin, double bound) {
        return randomBigDecimal(origin, bound, 2);
    }

    @Override
    public BigDecimal randomBigDecimal(double origin, double bound, int scale) {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(origin, bound))
                .setScale(scale, RoundingMode.HALF_UP);
    }

    @Override
    public String randomFirstName() {
        return selectRandom(firstNames);
    }

    @Override
    public String randomLastName() {
        return selectRandom(lastNames);
    }

    @Override
    public String randomCity() {
        return StringUtils.capitalize(selectRandom(cities));
    }

    @Override
    public String randomPhoneNumber() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder()
                .append("(")
                .append(random.nextInt(9) + 1);
        for (int i = 0; i < 2; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append(") ")
                .append(random.nextInt(9) + 1);
        for (int i = 0; i < 2; i++) {
            sb.append(random.nextInt(10));
        }
        sb.append("-");
        for (int i = 0; i < 4; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Override
    public String randomCountry() {
        return selectRandom(countries);
    }

    @Override
    public String randomAddress() {
        return selectRandom(streets);
    }

    @Override
    public String randomCurrency() {
        return selectRandom(currencies);
    }

    @Override
    public String randomState() {
        return selectRandom(states);
    }

    @Override
    public String randomStateCode() {
        return selectRandom(stateCodes);
    }

    @Override
    public String randomZipCode() {
        StringBuilder sb = new StringBuilder();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 5; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    @Override
    public String randomEmail() {
        String sb = randomFirstName().toLowerCase()
                    + "."
                    + randomLastName().toLowerCase()
                    + "@example.com";
        return sb.replace(' ', '.');
    }

    @Override
    public String randomLoreIpsum(int min, int max, boolean paragraphs) {
        return new LoreIpsum(min, max, paragraphs).generate();
    }

    @Override
    public String randomJson(int rootItems, int nestedItems) {
        ObjectNode root = jsonMapper.createObjectNode();
        ArrayNode users = root.putArray("users");

        IntStream.range(0, rootItems).forEach(value -> {
            ObjectNode u = users.addObject();

            u.put("email", randomEmail());
            u.put("firstName", randomFirstName());
            u.put("lastName", randomLastName());
            u.put("telephone", randomPhoneNumber());
            u.put("userName", randomFirstName().toLowerCase());

            ArrayNode addr = u.putArray("addresses");

            IntStream.range(0, nestedItems).forEach(n -> {
                ObjectNode a = addr.addObject();

                a.put("state", randomState());
                a.put("stateCode", randomStateCode());
                a.put("city", randomCity());
                a.put("country", randomCountry());
                a.put("zipCode", randomZipCode());
            });

        });

        return jsonMapper.writeValueAsString(root);
    }

    @Override
    public byte[] randomBytes(int min) {
        byte[] arr = new byte[min];
        ThreadLocalRandom.current().nextBytes(arr);
        return arr;
    }

    @Override
    public String randomString(int min, int max) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(random.nextInt(min, max))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Override
    public String randomString(int min) {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(min)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Override
    public String randomWord(int min) {
        StringBuilder sb = new StringBuilder();
        boolean vowelStart = true;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < min; i++) {
            if (vowelStart) {
                sb.append(VOWELS[random.nextInt(VOWELS.length)]);
            } else {
                sb.append(CONSONANTS[random.nextInt(CONSONANTS.length)]);
            }
            vowelStart = !vowelStart;
        }
        return sb.toString();
    }
}
