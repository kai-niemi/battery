package io.battery.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ansi.AnsiColor;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class AnsiColorAliasTest {
    @Test
    public void testAllAliasesMapToColors() {
        for (AnsiColorAlias alias : AnsiColorAlias.values()) {
            assertThat(alias.getColor()).isNotNull();
        }

        assertThat(AnsiColorAlias.d.getColor()).isEqualTo(AnsiColor.DEFAULT);
        assertThat(AnsiColorAlias.b.getColor()).isEqualTo(AnsiColor.BLACK);
        assertThat(AnsiColorAlias.r.getColor()).isEqualTo(AnsiColor.RED);
        assertThat(AnsiColorAlias.g.getColor()).isEqualTo(AnsiColor.GREEN);
        assertThat(AnsiColorAlias.y.getColor()).isEqualTo(AnsiColor.YELLOW);
        assertThat(AnsiColorAlias.l.getColor()).isEqualTo(AnsiColor.BLUE);
        assertThat(AnsiColorAlias.m.getColor()).isEqualTo(AnsiColor.MAGENTA);
        assertThat(AnsiColorAlias.c.getColor()).isEqualTo(AnsiColor.CYAN);
        assertThat(AnsiColorAlias.w.getColor()).isEqualTo(AnsiColor.WHITE);

        assertThat(AnsiColorAlias.bw.getColor()).isEqualTo(AnsiColor.BRIGHT_WHITE);
        assertThat(AnsiColorAlias.bl.getColor()).isEqualTo(AnsiColor.BRIGHT_BLUE);
        assertThat(AnsiColorAlias.bc.getColor()).isEqualTo(AnsiColor.BRIGHT_CYAN);
        assertThat(AnsiColorAlias.bg.getColor()).isEqualTo(AnsiColor.BRIGHT_GREEN);
        assertThat(AnsiColorAlias.bm.getColor()).isEqualTo(AnsiColor.BRIGHT_MAGENTA);
        assertThat(AnsiColorAlias.br.getColor()).isEqualTo(AnsiColor.BRIGHT_RED);
        assertThat(AnsiColorAlias.by.getColor()).isEqualTo(AnsiColor.BRIGHT_YELLOW);
        assertThat(AnsiColorAlias.bb.getColor()).isEqualTo(AnsiColor.BRIGHT_BLACK);
    }
}
