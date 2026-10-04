package io.battery.util.wgs;

import java.math.BigDecimal;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@Tag("unit-test")
public class LatitudeTest {
    @Test
    public void testFactoryMethodsAndValidation() {
        Latitude lat1 = Latitude.fromDecimal(45.5);
        assertThat(lat1.toDegrees()).isEqualTo(45.5);

        Latitude lat2 = Latitude.fromDecimal(new BigDecimal("45.500"));
        assertThat(lat2.toDegrees()).isEqualTo(45.5);

        Latitude south = Latitude.fromDMS("48° 51′ 28.800″S");
        assertThat(south.toDegrees()).isNegative();

        Latitude dfSouth = Latitude.fromDMS("48.858° S");
        assertThat(dfSouth.toDegrees()).isNegative();

        assertThatThrownBy(() -> Latitude.fromDMS("invalid format"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Latitude(91.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Latitude(-91.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testRadiansCompareEqualsAndToString() {
        Latitude lat1 = new Latitude(new BigDecimal("30.0"));
        Latitude lat2 = new Latitude(30.0);
        Latitude lat3 = new Latitude(60.0);

        assertThat(lat1.toRadians()).isCloseTo(Math.PI / 6.0, within(1e-6));

        assertThat(lat1.compare(lat1, lat2)).isEqualTo(0);
        assertThat(lat1.compare(lat1, lat3)).isNegative();

        assertThat(lat1).isEqualTo(lat2);
        assertThat(lat1.hashCode()).isEqualTo(lat2.hashCode());
        assertThat(lat1).isNotEqualTo(null);
        assertThat(lat1).isNotEqualTo("other");
        assertThat(lat1).isNotEqualTo(lat3);

        String str = lat1.toString();
        assertThat(str).contains("coordinate=30.0");
    }
}
