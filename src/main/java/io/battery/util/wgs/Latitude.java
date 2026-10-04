package io.battery.util.wgs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A WGS-84 geodetic latitude with a ISO-6709 string representation.
 */
public class Latitude implements Comparator<Latitude>, Coordinate {
    private static final Pattern LATITUDE_DMS
            = Pattern.compile("^([0-8]?[0-9]|90)°(\\s[0-5]?[0-9])′?(\\s[0-6]?[0-9](\\.[0-9]+)?)″([NS])");

    private static final Pattern LATITUDE_DF
            = Pattern.compile("^([0-8]?[0-9]|90)(\\.[0-9]+)?°?\\s([NS])");

    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_DOWN;

    public static Latitude fromDecimal(double coordinate) {
        return fromDecimal(new BigDecimal("" + coordinate)
                .setScale(3, ROUNDING_MODE));
    }

    public static Latitude fromDecimal(BigDecimal coordinate) {
        return new Latitude(coordinate.doubleValue());
    }

    public static Latitude fromDMS(String coordinate) {
        Matcher matcher = LATITUDE_DMS.matcher(coordinate);
        if (matcher.matches()) {
            return fromDMS(matcher);
        } else {
            matcher = LATITUDE_DF.matcher(coordinate);
            if (matcher.matches()) {
                return fromDF(matcher);
            }
        }
        throw new IllegalArgumentException("Not valid DMS format: " + coordinate);
    }

    private static Latitude fromDMS(Matcher matcher) {
        int d = Integer.parseInt(matcher.group(1));
        int m = Integer.parseInt(matcher.group(2).trim());
        double s = Double.parseDouble(matcher.group(3).replace(",", ".").trim());
        BigDecimal bd = new BigDecimal(d)
                .add(new BigDecimal(m)
                        .divide(new BigDecimal("60"), 5, ROUNDING_MODE))
                .add(new BigDecimal("" + s)
                        .divide(new BigDecimal("3600"), 5, ROUNDING_MODE));
        String sign = matcher.group(5);
        return new Latitude("N".equalsIgnoreCase(sign) ? bd : bd.negate());
    }

    private static Latitude fromDF(Matcher matcher) {
        int d = Integer.parseInt(matcher.group(1));
        double m = Double.parseDouble(matcher.group(2).trim());
        BigDecimal bd = new BigDecimal("" + d).add(new BigDecimal("" + m));
        String sign = matcher.group(3);
        return new Latitude("N".equalsIgnoreCase(sign) ? bd : bd.negate());
    }

    private final double coordinate;

    public Latitude(BigDecimal coordinate) {
        this(coordinate.doubleValue());
    }

    public Latitude(double coordinate) {
        if (coordinate > 90 || coordinate < -90) {
            throw new IllegalArgumentException("Latitude must be 90.0 > N < -90.0");
        }
        this.coordinate = coordinate;
    }

    @Override
    public double toDegrees() {
        return coordinate;
    }

    public double toRadians() {
        return Math.toRadians(coordinate);
    }

    @Override
    public int compare(Latitude o1, Latitude o2) {
        return Double.compare(o1.coordinate, o2.coordinate);
    }

    @Override
    @JsonValue
    public String toDMS() {
        BigDecimal d = new BigDecimal("" + coordinate);
        BigDecimal fractions = d.subtract(new BigDecimal(d.intValue()));
        BigDecimal m = fractions.multiply(new BigDecimal(60));
        double s = m.subtract(new BigDecimal(m.intValue()))
                .multiply(new BigDecimal(60))
                .doubleValue();
        return String.format(Locale.ROOT, "%d° %d′ %.3f″%s",
                Math.abs(d.intValue()),
                Math.abs(m.intValue()),
                Math.abs(s),
                coordinate >= 0 ? "N" : "S");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Latitude latitude = (Latitude) o;
        return Objects.equals(coordinate, latitude.coordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(coordinate);
    }

    @Override
    public String toString() {
        return "Latitude{" +
               "coordinate=" + coordinate +
               ", dms=" + toDMS() +
               '}';
    }
}
