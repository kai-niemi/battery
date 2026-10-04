package io.battery.util;

import org.springframework.boot.ansi.AnsiColor;

/**
 * Short aliases for ANSI colors in {@link AnsiPrintWriter} placeholders, such as {@code #(r)}
 * for red or {@code #(br)} for bright red, with {@code #(d)} resetting to the default color.
 */
public enum AnsiColorAlias {
    d(AnsiColor.DEFAULT),
    b(AnsiColor.BLACK),
    r(AnsiColor.RED),
    g(AnsiColor.GREEN),
    y(AnsiColor.YELLOW),
    l(AnsiColor.BLUE),
    m(AnsiColor.MAGENTA),
    c(AnsiColor.CYAN),
    w(AnsiColor.WHITE),

    bw(AnsiColor.BRIGHT_WHITE),
    bl(AnsiColor.BRIGHT_BLUE),
    bc(AnsiColor.BRIGHT_CYAN),
    bg(AnsiColor.BRIGHT_GREEN),
    bm(AnsiColor.BRIGHT_MAGENTA),
    br(AnsiColor.BRIGHT_RED),
    by(AnsiColor.BRIGHT_YELLOW),
    bb(AnsiColor.BRIGHT_BLACK);

    private final AnsiColor color;

    AnsiColorAlias(AnsiColor color) {
        this.color = color;
    }

    public AnsiColor getColor() {
        return color;
    }
}

