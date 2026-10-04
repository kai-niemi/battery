package io.battery.util.wgs;

/**
 * A geographic coordinate that specifies a point on the surface of the earth.
 */
public interface Coordinate {
    /**
     * @return the coordinate as decimal fractions of a degree
     */
    double toDegrees();

    /**
     * See <a href="https://en.wikipedia.org/wiki/Degree_(angle)#Subdivisions">Subdivisions</a>
     *
     * @return the coordinate as sexagesimal unit divided into degrees, minute, seconds (DMS)
     */
    String toDMS();
}
