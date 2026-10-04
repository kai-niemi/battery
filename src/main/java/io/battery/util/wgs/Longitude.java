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
 * A WGS-84 geodetic longitude with a ISO-6709 string representation.
 */
public class Longitude implements Comparator<Longitude>, Coordinate {
    private static final Pattern LONGITUDE_DMS
            = Pattern.compile("^([0-9]{1,2}|1[0-7][0-9]|180)°(\\s[0-5]?[0-9])′?(\\s[0-6]?[0-9](\\.[0-9]+)?)″([EW])");

    private static final Pattern LONGITUDE_DF
            = Pattern.compile("^([0-9]{1,2}|1[0-7][0-9]|180)(\\.[0-9]+)?°?\\s([EW])");

    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_DOWN;

    public static Longitude fromDecimal(double coordinate) {
        return fromDecimal(new BigDecimal("" + coordinate)
                .setScale(3, ROUNDING_MODE));
    }

    public static Longitude fromDecimal(BigDecimal coordinate) {
        return new Longitude(coordinate);
    }

    public static Longitude fromDMS(String coordinate) {
        Matcher matcher = LONGITUDE_DMS.matcher(coordinate);
        if (matcher.matches()) {
            return fromDMS(matcher);
        } else {
            matcher = LONGITUDE_DF.matcher(coordinate);
            if (matcher.matches()) {
                return fromDF(matcher);
            }
        }
        throw new IllegalArgumentException("Not valid DMS format: " + coordinate);
    }

    private static Longitude fromDMS(Matcher matcher) {
        int d = Integer.parseInt(matcher.group(1));
        int m = Integer.parseInt(matcher.group(2).trim());
        double s = Double.parseDouble(matcher.group(3).replace(",", ".").trim());
        BigDecimal bd = new BigDecimal(d)
                .add(new BigDecimal(m)
                        .divide(new BigDecimal("60"), 5, ROUNDING_MODE))
                .add(new BigDecimal("" + s)
                        .divide(new BigDecimal("3600"), 5, ROUNDING_MODE));
        String sign = matcher.group(5);
        return new Longitude("E".equalsIgnoreCase(sign) ? bd : bd.negate());
    }

    private static Longitude fromDF(Matcher matcher) {
        int d = Integer.parseInt(matcher.group(1));
        double m = Double.parseDouble(matcher.group(2).trim());
        BigDecimal bd = new BigDecimal("" + d).add(new BigDecimal("" + m));
        String sign = matcher.group(3);
        return new Longitude("E".equalsIgnoreCase(sign) ? bd : bd.negate());
    }

    private final double degrees;

    public Longitude(BigDecimal degrees) {
        this(degrees.doubleValue());
    }

    public Longitude(double degrees) {
        if (degrees > 180 || degrees < -180) {
            throw new IllegalArgumentException("Longitude must be 180.0 > N < -180.0");
        }
        this.degrees = degrees;
    }

    @Override
    public double toDegrees() {
        return degrees;
    }

    public double toRadians() {
        return Math.toRadians(degrees);
    }

    @Override
    public int compare(Longitude o1, Longitude o2) {
        return Double.compare(o1.degrees, o2.degrees);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Longitude longitude = (Longitude) o;
        return Objects.equals(degrees, longitude.degrees);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(degrees);
    }

    @Override
    @JsonValue
    public String toDMS() {
        BigDecimal d = new BigDecimal("" + degrees);
        double fractions = d.subtract(new BigDecimal(d.intValue())).doubleValue();
        BigDecimal m = new BigDecimal(fractions).multiply(new BigDecimal(60));
        double s = m.subtract(new BigDecimal(m.intValue()))
                .multiply(new BigDecimal(60))
                .doubleValue();
        return String.format(Locale.ROOT, "%d° %d′ %.3f″%s",
                Math.abs(d.intValue()),
                Math.abs(m.intValue()),
                Math.abs(s),
                d.doubleValue() >= 0 ? "E" : "W");
    }

    @Override
    public String toString() {
        return "Longitude{" +
               "coordinate=" + degrees +
               ", dms=" + toDMS() +
               '}';
    }
}
