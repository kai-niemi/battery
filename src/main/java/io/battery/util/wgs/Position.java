package io.battery.util.wgs;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents a World Geodetic System (WGS) coordinate.
 */
public class Position {
    public static Position of(double latitude, double longitude) {
        return new Position(Latitude.fromDecimal(latitude), Longitude.fromDecimal(longitude));
    }

    public static Position of(Latitude latitude, Longitude longitude) {
        return new Position(latitude, longitude);
    }

    private static final double EARTH_MEAN_RADIUS_KM = 6371.2;

    /**
     * Calculates the haversine (great-circle) distance in meters between two points on a
     * spherical object, such as the earth (actually earth is an ellipsoid, but whatever).
     * <p>
     * <a href="https://en.wikipedia.org/wiki/Great-circle_distance">Great Circle Distance</a>
     * <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine Formula</a>
     *
     * @param from from position
     * @param to   to position
     * @return the distance in km
     */
    public static double distanceBetween(Position from, Position to) {
        double fromLat = from.getLatitude().toRadians();
        double toLat = to.getLatitude().toRadians();
        double fromLon = from.getLongitude().toRadians();
        double toLon = to.getLongitude().toRadians();

        double latDelta = toLat - fromLat;
        double lonDelta = toLon - fromLon;
        double a = Math.sin(latDelta / 2) * Math.sin(latDelta / 2)
                   + Math.cos(fromLat) * Math.cos(toLat)
                     * Math.sin(lonDelta / 2) * Math.sin(lonDelta / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_MEAN_RADIUS_KM * c;
    }

    /**
     * Calculates the initial bearing between two positions on a celestial body.
     *
     * @param from the start position
     * @param to   the destination position
     * @return the bearing in degrees (0-360)
     */
    public static double bearingBetween(Position from, Position to) {
        double fromLat = from.getLatitude().toRadians();
        double toLat = to.getLatitude().toRadians();
        double fromLon = from.getLongitude().toRadians();
        double toLon = to.getLongitude().toRadians();

        double lonDelta = toLon - fromLon;
        double x = Math.cos(fromLat) * Math.sin(toLat)
                   - Math.sin(fromLat) * Math.cos(toLat) * Math.cos(lonDelta);
        double y = Math.sin(lonDelta) * Math.cos(toLat);
        double d = Math.atan2(y, x);
        double degrees = Math.toDegrees(d);

        return (((2 * 180 * degrees / 360) % 360) + 360) % 360;
    }

    private final Latitude latitude;

    private final Longitude longitude;

    public Position(Latitude latitude, Longitude longitude) {
        Objects.requireNonNull(latitude);
        Objects.requireNonNull(longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Latitude getLatitude() {
        return latitude;
    }

    public Longitude getLongitude() {
        return longitude;
    }

    @JsonValue
    public String toDMS() {
        return latitude.toDMS() + " " + longitude.toDMS();
    }
}
