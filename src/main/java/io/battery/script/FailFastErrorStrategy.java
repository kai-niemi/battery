package io.battery.script;

import java.util.BitSet;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.FailedPredicateException;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

/**
 * ANTLR4 error strategy and error listener that fails fast on any lexer or parser error
 * by throwing a {@link BatteryScriptException}, without attempting recovery.
 */
public class FailFastErrorStrategy extends DefaultErrorStrategy implements ANTLRErrorListener {
    /**
     * Make sure we don't attempt to recover from problems in subrules.
     */
    @Override
    public void sync(Parser recognizer) {
    }

    @Override
    public void recover(Parser parser, RecognitionException e) {
        for (ParserRuleContext context = parser.getContext(); context != null; context = context.getParent()) {
            context.exception = e;
        }
        throw BatteryScriptException.from(e.toString(), parser);
    }

    @Override
    public Token recoverInline(Parser parser) throws RecognitionException {
        InputMismatchException e = new InputMismatchException(parser);
        for (ParserRuleContext context = parser.getContext(); context != null; context = context.getParent()) {
            context.exception = e;
        }

        String msg = "Mismatched input " + getTokenErrorDisplay(e.getOffendingToken())
                     + ". Expecting one of: " + e.getExpectedTokens().toString(parser.getVocabulary());

        throw BatteryScriptException.from(msg, parser, e.getOffendingToken());
    }

    @Override
    public void reportError(Parser recognizer, RecognitionException e) {
        if (!inErrorRecoveryMode(recognizer)) {
            if (e instanceof NoViableAltException) {
                reportNoViableAlternative(recognizer, (NoViableAltException) e);
            } else if (e instanceof InputMismatchException) {
                reportInputMismatch(recognizer, (InputMismatchException) e);
            } else if (e instanceof FailedPredicateException) {
                reportFailedPredicate(recognizer, (FailedPredicateException) e);
            } else {
                recognizer.removeParseListeners();
                recognizer.notifyErrorListeners(e.getOffendingToken(), e.getMessage(), e);
            }
        }
    }

    @Override
    protected void reportNoViableAlternative(Parser parser, NoViableAltException cause) {
        String msg = "No viable alternative input for "
                     + getTokenErrorDisplay(cause.getOffendingToken())
                     + ". Expecting one of: " + cause.getExpectedTokens().toString(parser.getVocabulary());
        throw BatteryScriptException.from(msg, parser);
    }

    @Override
    protected void reportInputMismatch(Parser parser, InputMismatchException cause) {
        String msg = "Mismatched input " + getTokenErrorDisplay(cause.getOffendingToken())
                     + ". Expecting one of: " + cause.getExpectedTokens().toString(parser.getVocabulary());
        throw BatteryScriptException.from(msg, parser);
    }

    @Override
    public void reportMissingToken(Parser parser) {
        String msg = "Missing " + getExpectedTokens(parser).toString(parser.getVocabulary())
                     + " at " + getTokenErrorDisplay(parser.getCurrentToken());
        throw BatteryScriptException.from(msg, parser);
    }

    @Override
    protected void reportUnwantedToken(Parser parser) {
        String msg = "Unwanted token " + getTokenErrorDisplay(parser.getCurrentToken())
                     + " expected " + getExpectedTokens(parser).toString(parser.getVocabulary());
        throw BatteryScriptException.from(msg, parser);
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        recognizer.removeErrorListeners();
        throw new BatteryScriptException(msg + " at pos " + line + ":" + charPositionInLine,
                line, charPositionInLine);
    }

    @Override
    public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact,
                                BitSet ambigAlts, ATNConfigSet configs) {
    }

    @Override
    public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                            BitSet conflictingAlts, ATNConfigSet configs) {
    }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
                                         int prediction,
                                         ATNConfigSet configs) {
    }
}
