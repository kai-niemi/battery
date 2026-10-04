package io.battery.util;

import java.io.StringWriter;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit-test")
public class AnsiPrintWriterTest {
    @BeforeEach
    public void setup() {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
    }

    @AfterEach
    public void teardown() {
        AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT);
    }

    @Test
    public void testWrapAndPrintMethods() {
        StringWriter sw = new StringWriter();
        AnsiPrintWriter writer = AnsiPrintWriter.wrap(sw);

        writer.println("Hello #(red)World#(d)!");
        writer.println("Background #(bg:blue)test#(d)"); // test fallback if any
        writer.println("Styles #st(bold)Bold#st(default) and #bg(green)green bg#bg(default)");
        writer.println("Aliases #(r)red#(d) #(bw)bright white#(d) #(unknown_color)fallback#(d)");
        writer.write("raw write");
        writer.println();

        String output = sw.toString();
        assertThat(output).contains("Hello");
        assertThat(output).contains("raw write");
    }

    @Test
    public void testFormatMethods() {
        StringWriter sw = new StringWriter();
        AnsiPrintWriter writer = new AnsiPrintWriter(sw, true);

        writer.format("Formatted %s #(green)%d#(d)%n", "number", 42);
        writer.format(Locale.GERMANY, "Locale float %.2f#(d)%n", 3.14);

        String output = sw.toString();
        assertThat(output).contains("Formatted number");
        assertThat(output).contains("Locale float 3,14");
    }

    @Test
    public void testColorHelperMethods() {
        StringWriter sw = new StringWriter();
        AnsiPrintWriter writer = new AnsiPrintWriter(sw);

        writer.whiteln("white text");
        writer.redln("red text");
        writer.yellowln("yellow text");
        writer.greenln("green text");

        String output = sw.toString();
        assertThat(output).contains("white text");
        assertThat(output).contains("red text");
        assertThat(output).contains("yellow text");
        assertThat(output).contains("green text");
    }
}
