package io.battery.util;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;

import org.springframework.boot.ansi.AnsiBackground;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.util.PropertyPlaceholderHelper;

/**
 * A print writer for shell output that expands ANSI placeholders in {@link #println(String)}
 * and the {@code format} and {@code printf} methods: {@code #(color)} for a foreground color by
 * name or {@link AnsiColorAlias alias}, {@code #bg(color)} for a background color and
 * {@code #st(style)} for a style, as in {@code "#(bright_red)failed#(d)"}. Unknown names reset
 * to the default color. The other print methods write text as is.
 */
public class AnsiPrintWriter extends PrintWriter {
    public static AnsiPrintWriter wrap(Writer target) {
        return new AnsiPrintWriter(target, true);
    }

    private final EnumSet<AnsiColor> ansiColor = EnumSet.allOf(AnsiColor.class);

    private final EnumSet<AnsiColorAlias> ansiColorAlias = EnumSet.allOf(AnsiColorAlias.class);

    private final EnumSet<AnsiBackground> ansiBackground = EnumSet.allOf(AnsiBackground.class);

    private final EnumSet<AnsiStyle> ansiStyle = EnumSet.allOf(AnsiStyle.class);

    public AnsiPrintWriter(Writer out) {
        super(out);
    }

    public AnsiPrintWriter(Writer out, boolean autoFlush) {
        super(out, autoFlush);
    }

    private String expand(String format) {
        return expandBg(expandFg(expandStyle(format)));
    }

    private String expandFg(String format) {
        return new PropertyPlaceholderHelper("#(", ")")
                .replacePlaceholders(format, placeholderName -> {
                    String name = placeholderName.toUpperCase();

                    Optional<AnsiColor> c = ansiColor.stream()
                            .filter(x -> x.name().equalsIgnoreCase(name))
                            .findFirst();
                    if (c.isPresent()) {
                        return AnsiOutput.encode(c.get());
                    }

                    Optional<AnsiColorAlias> ca = ansiColorAlias.stream()
                            .filter(x -> x.name().equalsIgnoreCase(name))
                            .findFirst();
                    if (ca.isPresent()) {
                        return AnsiOutput.encode(ca.get().getColor());
                    }

                    return c.map(AnsiOutput::encode)
                            .orElseGet(() -> AnsiOutput.encode(AnsiColor.DEFAULT));
                });
    }

    private String expandBg(String format) {
        return new PropertyPlaceholderHelper("#bg(", ")")
                .replacePlaceholders(format, placeholderName -> {
                    String name = placeholderName.toUpperCase();

                    Optional<AnsiBackground> bg = ansiBackground.stream()
                            .filter(x -> x.name().equalsIgnoreCase(name))
                            .findFirst();
                    return bg.map(AnsiOutput::encode)
                            .orElseGet(() -> AnsiOutput.encode(AnsiBackground.DEFAULT));
                });
    }

    private String expandStyle(String format) {
        return new PropertyPlaceholderHelper("#st(", ")")
                .replacePlaceholders(format, placeholderName -> {
                    String name = placeholderName.toUpperCase();
                    Optional<AnsiStyle> s = ansiStyle.stream()
                            .filter(x -> x.name().equalsIgnoreCase(name))
                            .findFirst();
                    return s.map(AnsiOutput::encode)
                            .orElseGet(() -> AnsiOutput.encode(AnsiColor.DEFAULT));
                });
    }

    @Override
    public void println(String x) {
        super.println(expand(x));
    }

    @Override
    public void write(String value) {
        super.write(value);
    }

    public PrintWriter format(String format, Object... args) {
        print(String.format(expand(format), args));
        return this;
    }

    @Override
    public PrintWriter format(Locale locale, String format, Object... args) {
        print(String.format(locale, expand(format), args));
        return this;
    }

    public AnsiPrintWriter whiteln(String s) {
        println("#(bright_white)%s#(d)".formatted(s));
        return this;
    }

    public AnsiPrintWriter redln(String s) {
        println("#(bright_red)%s#(d)".formatted(s));
        return this;
    }

    public AnsiPrintWriter yellowln(String s) {
        println("#(bright_yellow)%s#(d)".formatted(s));
        return this;
    }

    public AnsiPrintWriter greenln(String s) {
        println("#(bright_green)%s#(d)".formatted(s));
        return this;
    }

}
