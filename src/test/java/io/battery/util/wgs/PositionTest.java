package io.battery.util.wgs;

import java.util.stream.IntStream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@Tag("unit-test")
public class PositionTest {
    @Test
    public void testLatitudeToFromDMS() {
        IntStream.rangeClosed(0, 90 - 1).forEach(
                value -> IntStream.rangeClosed(2, 60).forEach(f -> {
                    double fraction = 1.0 / f;
                    Latitude before = Latitude.fromDecimal(value + fraction);
                    Latitude after = Latitude.fromDMS(before.toDMS());
                    assertThat(after).isEqualTo(before);
                }));
        IntStream.rangeClosed(-90, 0).forEach(
                value -> IntStream.rangeClosed(2, 60).forEach(f -> {
                    double fraction = 1.0 / f;
                    Latitude before = Latitude.fromDecimal(value + fraction);
                    Latitude after = Latitude.fromDMS(before.toDMS());
                    assertThat(after).isEqualTo(before);
                }));
    }

    @Test
    public void testLongitudeToFromDMS() {
        IntStream.rangeClosed(0, 180 - 1).forEach(
                value -> IntStream.rangeClosed(2, 60).forEach(f -> {
                    double fraction = 1.0 / f;
                    Longitude before = Longitude.fromDecimal(value + fraction);
                    Longitude after = Longitude.fromDMS(before.toDMS());
                    assertThat(after).isEqualTo(before);
                }));
        IntStream.rangeClosed(-180, 0).forEach(
                value -> IntStream.rangeClosed(2, 60).forEach(f -> {
                    double fraction = 1.0 / f;
                    Longitude before = Longitude.fromDecimal(value + fraction);
                    Longitude after = Longitude.fromDMS(before.toDMS());
                    assertThat(after).isEqualTo(before);
                }));
    }

    @Test
    public void testDistanceBetweenDMSCoordinates() {
        Latitude lat = Latitude.fromDMS("38° 53′ 52.800″N");
        Longitude lon = Longitude.fromDMS("77° 2′ 13.920″W");

        Latitude lat2 = Latitude.fromDMS("48° 51′ 28.800″N");
        Longitude lon2 = Longitude.fromDMS("2° 17′ 38.400″E");

        Position washington = Position.of(lat, lon);
        Position paris = Position.of(lat2, lon2);

        double distanceBetween = Position.distanceBetween(washington, paris);
        double bearingBetween = Position.bearingBetween(washington, paris);

        System.out.println(washington.toDMS());
        System.out.println(paris.toDMS());
        System.out.println("Distance between: " + distanceBetween + " km");
        System.out.println("Initial bearing: " + bearingBetween);
    }

    @Test
    public void testDistanceBetweenDFCoordinates() {
        Latitude lat = Latitude.fromDMS("38.898° N");
        Longitude lon = Longitude.fromDMS("77.0372° W");

        Latitude lat2 = Latitude.fromDMS("48.858° N");
        Longitude lon2 = Longitude.fromDMS("2.294° E");

        Position washington = Position.of(lat, lon);
        Position paris = Position.of(lat2, lon2);

        double distanceBetween = Position.distanceBetween(washington, paris);
        double bearingBetween = Position.bearingBetween(washington, paris);

        System.out.println(washington.toDMS());
        System.out.println(paris.toDMS());
        System.out.println("Distance between: " + distanceBetween + " km");
        System.out.println("Initial bearing: " + bearingBetween);
    }

    @Test
    public void testPositionAccessorsAndValidation() {
        Position pos = Position.of(12.34, 56.78);
        assertThat(pos.getLatitude().toDegrees()).isCloseTo(12.34, within(1e-4));
        assertThat(pos.getLongitude().toDegrees()).isCloseTo(56.78, within(1e-4));
        assertThat(pos.toDMS()).isNotNull();

        assertThatThrownBy(() -> new Position(null, Longitude.fromDecimal(0)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Position(Latitude.fromDecimal(0), null))
                .isInstanceOf(NullPointerException.class);
    }
}
