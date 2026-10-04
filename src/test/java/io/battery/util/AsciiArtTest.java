package io.battery.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class AsciiArtTest {
    @Test
    public void testArtRepresentations() {
        assertThat(AsciiArt.happy()).isEqualTo("(ʘ‿ʘ)");
        assertThat(AsciiArt.shrug()).isEqualTo("¯\\_(ツ)_/¯");
        assertThat(AsciiArt.flipTableGently()).isEqualTo("(╯°□°)╯︵ ┻━┻");
        assertThat(AsciiArt.flipTableRoughly()).isEqualTo("(ノಠ益ಠ)ノ彡┻━┻");
    }
}
