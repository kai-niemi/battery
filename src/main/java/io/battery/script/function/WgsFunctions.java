package io.battery.script.function;

import java.util.concurrent.ThreadLocalRandom;

import io.battery.util.wgs.Coordinate;
import io.battery.util.wgs.Latitude;
import io.battery.util.wgs.Longitude;
import io.battery.util.wgs.Position;

/**
 * WGS-84 geo functions in the {@code wgs} namespace for building positions, computing
 * great-circle distances, converting coordinates and generating random coordinates.
 * A mixin of default methods.
 */
public interface WgsFunctions {
    String NAMESPACE = "wgs";

    @Description(value = """
            Creates a WGS position from a latitude and longitude.
            """, volatility = Volatility.Immutable)
    default Position toPos(Latitude lat, Longitude lon) {
        return Position.of(lat, lon);
    }

    @Description(value = """
            Calculates the great circle distance between two WGS positions.
            """, volatility = Volatility.Immutable)
    default double distance(Position from, Position to) {
        return Position.distanceBetween(from, to);
    }

    @Description(value = """
            Converts a WGS coordinate to DMS format.
            """, volatility = Volatility.Immutable)
    default String toDMS(Coordinate coordinate) {
        return coordinate.toDMS();
    }

    @Description(value = """
            Converts a latitude DMS to a latitude.
            """, volatility = Volatility.Immutable)
    default Latitude dmsToLatitude(String dms) {
        return Latitude.fromDMS(dms);
    }

    @Description(value = """
            Converts a longitude DMS to a longitude.
            """, volatility = Volatility.Immutable)
    default Longitude dmsToLongitude(String dms) {
        return Longitude.fromDMS(dms);
    }

    @Description(value = """
            Converts a decimal latitude to a latitude.
            """, volatility = Volatility.Immutable)
    default Latitude decimalToLatitude(double lat) {
        return Latitude.fromDecimal(lat);
    }

    @Description(value = """
            Converts a decimal longitude to a longitude.
            """, volatility = Volatility.Immutable)
    default Longitude decimalToLongitude(double lon) {
        return Longitude.fromDecimal(lon);
    }

    @Description(value = """
            Returns a random latitude in decimal format.
            """, volatility = Volatility.Volatile)
    default Latitude randomLatitude() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Latitude.fromDecimal(random.nextBoolean()
                ? random.nextDouble(0, 90)
                : -random.nextDouble(0, 90));
    }

    @Description(value = """
            Returns a random longitude in decimal format.
            """, volatility = Volatility.Volatile)
    default Longitude randomLongitude() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return Longitude.fromDecimal(random.nextBoolean()
                ? random.nextDouble(0, 180)
                : -random.nextDouble(0, 180));
    }
}
