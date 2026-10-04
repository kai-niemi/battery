package io.battery.util.wgs;

import java.math.BigDecimal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@Tag("unit-test")
public class LongitudeTest {
    @Test
    public void testFactoryMethodsAndValidation() {
        Longitude lon1 = Longitude.fromDecimal(45.5);
        assertThat(lon1.toDegrees()).isEqualTo(45.5);

        Longitude lon2 = Longitude.fromDecimal(new BigDecimal("45.500"));
        assertThat(lon2.toDegrees()).isEqualTo(45.5);

        Longitude east = Longitude.fromDMS("2° 17′ 38.400″E");
        assertThat(east.toDegrees()).isPositive();

        Longitude dfEast = Longitude.fromDMS("2.294° E");
        assertThat(dfEast.toDegrees()).isPositive();

        assertThatThrownBy(() -> Longitude.fromDMS("invalid format"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Longitude(181.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Longitude(-181.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRadiansCompareEqualsAndToString() {
        Longitude lon1 = new Longitude(new BigDecimal("30.0"));
        Longitude lon2 = new Longitude(30.0);
        Longitude lon3 = new Longitude(60.0);

        assertThat(lon1.toRadians()).isCloseTo(Math.PI / 6.0, within(1e-6));

        assertThat(lon1.compare(lon1, lon2)).isEqualTo(0);
        assertThat(lon1.compare(lon1, lon3)).isNegative();

        assertThat(lon1).isEqualTo(lon2);
        assertThat(lon1.hashCode()).isEqualTo(lon2.hashCode());
        assertThat(lon1).isNotEqualTo(null);
        assertThat(lon1).isNotEqualTo("other");
        assertThat(lon1).isNotEqualTo(lon3);

        String str = lon1.toString();
        assertThat(str).contains("coordinate=30.0");
    }
}
