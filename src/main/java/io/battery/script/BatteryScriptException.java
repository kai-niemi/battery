package io.battery.script;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.springframework.data.util.Pair;

/**
 * Exception thrown when a battery script fails to parse (lexer or grammar violation)
 * or fails during evaluation. Carries the line and character position of the offending
 * token, when known.
 */
public class BatteryScriptException extends RuntimeException {
    public static BatteryScriptException from(String message, Parser parser, Token token) {
        return from(message, parser, token, null);
    }

    public static BatteryScriptException from(String message, Parser parser) {
        return from(message, parser, parser.getCurrentToken(), null);
    }

    public static BatteryScriptException from(String message, Parser parser, Throwable cause) {
        return from(message, parser, parser.getCurrentToken(), cause);
    }

    public static BatteryScriptException from(String message, Parser parser, Token token, Throwable cause) {
        parser.removeParseListeners();
        String fullInput = parser.getTokenStream().getTokenSource().getInputStream().toString();
        return new BatteryScriptException("%s at position %d:%d in:%n%s"
                .formatted(message, token.getLine(), token.getCharPositionInLine(), fullInput),
                cause, token.getLine(), token.getCharPositionInLine());
    }

    private final Pair<Integer, Integer> offendingTokenOffset;

    public BatteryScriptException(String message, int line, int charPosition) {
        super(message);
        this.offendingTokenOffset = Pair.of(line, charPosition);
    }

    public BatteryScriptException(String message, Throwable cause, int line, int charPosition) {
        super(message, cause);
        this.offendingTokenOffset = Pair.of(line, charPosition);
    }

    public BatteryScriptException(String message, Throwable cause, Pair<Integer, Integer> pair) {
        super(message, cause);
        this.offendingTokenOffset = pair;
    }

    public Pair<Integer, Integer> getOffendingTokenOffset() {
        return offendingTokenOffset;
    }
}
