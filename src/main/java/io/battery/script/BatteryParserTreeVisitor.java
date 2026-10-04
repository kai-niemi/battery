package io.battery.script;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.springframework.core.NestedExceptionUtils;

import io.battery.script.atom.ArrayAtom;
import io.battery.script.atom.AtomValue;
import io.battery.script.atom.NamespaceAtom;
import io.battery.script.atom.NullAtom;
import io.battery.script.support.ReflectionSupport;
import io.battery.script.support.VariableScope;
import static io.battery.script.atom.AtomValue.VOID;

/**
 * Main ANTLR4 parse tree visitor for the battery script grammar. Interprets the parse tree
 * directly: evaluates operators and literals, executes if/while/for/foreach statements and
 * invokes methods and fields of external objects via reflection.
 * <p>
 * Fork statements run their block asynchronously on a virtual thread in a cloned visitor,
 * and join statements wait for completion.
 * <p>
 * Variables are lexically scoped: a variable is local to the block that first assigns it,
 * while assigning an existing variable of an enclosing block updates it. Loop variables are
 * local to their loop. A fork operates on an isolated snapshot of the variables visible at
 * the fork statement, so its assignments are not visible to the parent or other forks, and
 * it returns results through its future. Since forks run on other threads, they don't
 * participate in any thread-bound Spring transaction.
 */
public class BatteryParserTreeVisitor extends BatteryParserBaseVisitor<AtomValue> {
    // Fork blocks typically perform blocking I/O, so each runs on its own virtual thread
    private static final Executor FORK_EXECUTOR
            = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("battery-fork-", 0).factory());

    private static String stripQuotes(String s) {
        return s.replaceAll("(^')|(^\")|('$)|(\"$)", "");
    }

    private static boolean isOctalDigit(char c) {
        return c >= '0' && c <= '7';
    }

    /**
     * Decodes the escape sequences accepted by the lexer's ESCAPE_SEQUENCE fragment in a single
     * pass, so that an escaped backslash followed by 'u' (as in "\\u0041") is not read as a
     * unicode escape.
     */
    private static String unescape(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c != '\\' || i == s.length()) {
                sb.append(c);
                continue;
            }
            // A unicode escaped backslash may precede a simple or octal escape, as in Java
            if (s.startsWith("u005c", i) && i + 5 < s.length()
                && ("btnfr\"'\\".indexOf(s.charAt(i + 5)) >= 0 || isOctalDigit(s.charAt(i + 5)))) {
                i += 5;
            }
            char e = s.charAt(i++);
            switch (e) {
                case 'b' -> sb.append('\b');
                case 't' -> sb.append('\t');
                case 'n' -> sb.append('\n');
                case 'f' -> sb.append('\f');
                case 'r' -> sb.append('\r');
                case '"', '\'', '\\' -> sb.append(e);
                case 'u' -> {
                    while (s.charAt(i) == 'u') {
                        i++;
                    }
                    sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                }
                default -> {
                    if (!isOctalDigit(e)) {
                        sb.append('\\').append(e);
                        break;
                    }
                    // Up to three octal digits, where a three digit escape starts with 0-3
                    int maxDigits = e <= '3' ? 3 : 2;
                    int start = i - 1;
                    while (i < s.length() && i - start < maxDigits && isOctalDigit(s.charAt(i))) {
                        i++;
                    }
                    sb.append((char) Integer.parseInt(s.substring(start, i), 8));
                }
            }
        }
        return sb.toString();
    }

    private static String stripBlockQuotes(String s) {
        // Drops the line break after the opening quotes and before the closing quotes,
        // whether it's a Unix or Windows line ending
        s = s.replaceFirst("^\"\"\"[ \t]*\r?\n", "");
        s = s.replaceFirst("\r?\n?\"\"\"$", "");
        return s;
    }

    private VariableScope<AtomValue> localVars;

    private final VariableScope<AtomValue> externalVars;

    private final Parser parser;

    BatteryParserTreeVisitor(Parser parser,
                             Map<String, Object> initialState,
                             Map<String, Object> externalState) {
        this.parser = parser;
        this.localVars = new VariableScope<>();
        this.externalVars = new VariableScope<>();

        initialState.keySet()
                .forEach(k -> localVars.assign(k, AtomValue.of(initialState.get(k))));

        externalState.keySet()
                .forEach(k -> externalVars.assign(k, AtomValue.of(externalState.get(k))));

        this.externalVars.seal();
    }

    private BatteryParserTreeVisitor(BatteryParserTreeVisitor parent) {
        this.parser = parent.parser;
        // Isolated copy of the visible variables, so forks can't affect each other or the parent
        this.localVars = parent.localVars.snapshot();
        this.externalVars = new VariableScope<>(parent.externalVars);
    }

    public Map<String, Object> getGlobalVariables() {
        return localVars.getVariables(s -> !VariableScope.isLocalVariable(s));
    }

    private Optional<AtomValue> lookupVariable(String name) {
        if (externalVars.contains(name)) {
            return Optional.of(AtomValue.of(externalVars.get(name)));
        }

        if (localVars.contains(name)) {
            return Optional.of(AtomValue.of(localVars.get(name)));
        }

        return Optional.empty();
    }

    private String tokenDisplayName(Token token) {
        return tokenDisplayName(token.getType());
    }

    private String tokenDisplayName(int type) {
        return BatteryParser.VOCABULARY.getDisplayName(type);
    }

    private BatteryScriptException illegalOperation(Token op, AtomValue right,
                                                    Throwable cause) {
        return BatteryScriptException.from("Operator %s cannot be applied to '%s'"
                        .formatted(tokenDisplayName(op),
                                typeName(right)),
                parser, op, cause);
    }

    private static String typeName(AtomValue value) {
        // Null and void atoms have no class to report
        return value.asObject() == null ? "null" : value.asClass().getSimpleName();
    }

    private BatteryScriptException illegalOperation(Token op, AtomValue left, AtomValue right,
                                                    Throwable cause) {
        return BatteryScriptException.from("Operator %s cannot be applied to '%s', '%s'"
                        .formatted(tokenDisplayName(op),
                                typeName(left),
                                typeName(right)),
                parser, op, cause);
    }

    //
    // Atoms
    //

    @Override
    public AtomValue visitIdentifierAtom(BatteryParser.IdentifierAtomContext ctx) {
        String id = ctx.identifier().getText();
        return lookupVariable(id)
                .orElseGet(() -> new NamespaceAtom(id));
    }

    @Override
    public AtomValue visitStringAtom(BatteryParser.StringAtomContext ctx) {
        TerminalNode stringLiteral = ctx.string_literal().STRING_LITERAL();
        if (stringLiteral != null) {
            String s = stringLiteral.getText();
            s = unescape(stripQuotes(s));
            return AtomValue.of(s);
        } else {
            // Text blocks are kept raw, since they typically embed SQL that may contain backslashes
            TerminalNode textBlock = ctx.string_literal().TEXT_BLOCK();
            String s = textBlock.getText();
            s = stripBlockQuotes(s);
            return AtomValue.of(s);
        }
    }

    @Override
    public AtomValue visitIntAtom(BatteryParser.IntAtomContext ctx) {
        return AtomValue.of(Integer.parseInt(ctx.getText()));
    }

    @Override
    public AtomValue visitDoubleAtom(BatteryParser.DoubleAtomContext ctx) {
        return AtomValue.of(Double.valueOf(ctx.getText()));
    }

    @Override
    public AtomValue visitBigDecimalAtom(BatteryParser.BigDecimalAtomContext ctx) {
        String literal = ctx.getText().replace("bd", "");
        return AtomValue.of(new BigDecimal(literal));
    }

    @Override
    public AtomValue visitBooleanAtom(BatteryParser.BooleanAtomContext ctx) {
        return AtomValue.of(Boolean.valueOf(ctx.getText()));
    }

    @Override
    public AtomValue visitArrayAtom(BatteryParser.ArrayAtomContext ctx) {
        List<AtomValue> atomList = new ArrayList<>();

        BatteryParser.ArrayContext array = ctx.array();
        BatteryParser.Expression_listContext listContext = array.expression_list();

        for (BatteryParser.ExpressionContext c : listContext.expression()) {
            atomList.add(this.visit(c));
        }

        Object[] values = atomList.stream().map(AtomValue::asObject).toArray();

        return AtomValue.of(values);
    }

    @Override
    public AtomValue visitMapAtom(BatteryParser.MapAtomContext ctx) {
        List<Object> atomList = new ArrayList<>();

        BatteryParser.MapContext array = ctx.map();
        BatteryParser.Expression_listContext mapContext = array.expression_list();

        for (BatteryParser.ExpressionContext c : mapContext.expression()) {
            atomList.add(this.visit(c).asObject());
        }

        if (atomList.size() % 2 != 0) {
            throw new IllegalArgumentException("Map must have an even number of tuples, got: " + atomList.size());
        }

        Map<Object, Object> map = new LinkedHashMap<>();

        for (Iterator<Object> iterator = atomList.iterator(); iterator.hasNext(); ) {
            Object next = iterator.next();
            map.put(next, iterator.next());
        }

        return AtomValue.of(map);
    }

    @Override
    public AtomValue visitListAtom(BatteryParser.ListAtomContext ctx) {
        List<Object> atomList = new ArrayList<>();

        BatteryParser.ListContext array = ctx.list();
        BatteryParser.Expression_listContext listContext = array.expression_list();

        for (BatteryParser.ExpressionContext c : listContext.expression()) {
            atomList.add(this.visit(c).asObject());
        }

        List<Object> values = atomList.stream().toList();

        return AtomValue.of(values);
    }

    @Override
    public AtomValue visitSetAtom(BatteryParser.SetAtomContext ctx) {
        List<Object> atomList = new ArrayList<>();

        BatteryParser.SetContext set = ctx.set();
        BatteryParser.Expression_listContext setContext = set.expression_list();

        for (BatteryParser.ExpressionContext c : setContext.expression()) {
            atomList.add(this.visit(c).asObject());
        }

        Set<?> values = new HashSet<>(atomList);

        return AtomValue.of(values);
    }

    @Override
    public AtomValue visitNullAtom(BatteryParser.NullAtomContext ctx) {
        return AtomValue.of(null);
    }

    @Override
    public AtomValue visitArrayElementExpr(BatteryParser.ArrayElementExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        AtomValue right = this.visit(ctx.expression(1));

        if (left instanceof ArrayAtom) {
            ArrayAtom arrayAtom = (ArrayAtom) left;
            Object value = arrayAtom.asObject()[right.asInteger()];
            return AtomValue.of(value);
        }

        throw BatteryScriptException.from("Operand '%s' is not an array"
                .formatted(tokenDisplayName(ctx.expression(0).getStart())), parser, ctx.getStop());
    }

    // Quoted value of a date, time or datetime literal such as {d '2020-01-01'}
    private static final Pattern QUOTED_VALUE_PATTERN = Pattern.compile("'([^']+)'");

    @Override
    public AtomValue visitDateAtom(BatteryParser.DateAtomContext ctx) {
        String literal = ctx.date_literal().getText();
        Matcher matcher = QUOTED_VALUE_PATTERN.matcher(literal);
        if (matcher.find()) {
            String dateString = matcher.group(1);
            return AtomValue.of(LocalDate.parse(dateString));
        }
        throw BatteryScriptException.from("Bad date literal '%s'"
                .formatted(literal), parser, ctx.getStop());
    }

    @Override
    public AtomValue visitTimeAtom(BatteryParser.TimeAtomContext ctx) {
        String literal = ctx.time_literal().getText();
        Matcher matcher = QUOTED_VALUE_PATTERN.matcher(literal);
        if (matcher.find()) {
            String timeString = matcher.group(1);
            return AtomValue.of(LocalTime.parse(timeString));
        }
        throw BatteryScriptException.from("Bad time literal '%s'"
                .formatted(literal), parser, ctx.getStop());
    }

    @Override
    public AtomValue visitDateTimeAtom(BatteryParser.DateTimeAtomContext ctx) {
        String literal = ctx.datetime_literal().getText();
        Matcher matcher = QUOTED_VALUE_PATTERN.matcher(literal);
        if (matcher.find()) {
            String dateTimeString = matcher.group(1);
            return AtomValue.of(LocalDateTime.parse(dateTimeString.replace(" ", "T")));
        }
        throw BatteryScriptException.from("Bad datetime literal '%s'"
                .formatted(literal), parser, ctx.getStop());
    }

    //
    // Blocks and expressions
    //

    /**
     * Visits a parse tree node, converting runtime errors into script exceptions carrying
     * the position of the innermost node where they occurred, rather than end of input.
     */
    @Override
    public AtomValue visit(ParseTree tree) {
        try {
            return super.visit(tree);
        } catch (BatteryScriptException | ReturnException e) {
            throw e;
        } catch (RuntimeException e) {
            // Retain a script exception wrapped by other exceptions, such as fork completions
            for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof BatteryScriptException scriptException) {
                    throw scriptException;
                }
            }
            Token token = tree instanceof ParserRuleContext ruleContext
                    ? ruleContext.getStart()
                    : tree instanceof TerminalNode terminalNode ? terminalNode.getSymbol() : null;
            if (token == null) {
                throw e;
            }
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw BatteryScriptException.from(message, parser, token, e);
        }
    }

    @Override
    public AtomValue visitScript(BatteryParser.ScriptContext ctx) {
        try {
            return super.visitScript(ctx);
        } catch (ReturnException e) {
            // A top-level return ends the script with the returned value as last result
            localVars.root().assignOrReplace(Constants.LAST_RESULT_VAR, e.getValue());
            return e.getValue();
        }
    }

    @Override
    public AtomValue visitBlock(BatteryParser.BlockContext ctx) {
        AtomValue lastResult = null;
        for (BatteryParser.StatementContext stmtCtx : ctx.statement()) {
            lastResult = visit(stmtCtx);
        }
        return lastResult;
    }

    @Override
    public AtomValue visitExpression_statement(BatteryParser.Expression_statementContext ctx) {
        AtomValue atomValue = visit(ctx.expression());

        if (!(atomValue instanceof NullAtom)) {
            // The last result is global (or fork-global), regardless of the block it occurs in
            localVars.root().assignOrReplace(Constants.LAST_RESULT_VAR, atomValue);
        }

        return atomValue;
    }

    /**
     * Rejects assigning a registered namespace name, since external variables take precedence
     * and the assigned value could never be read back.
     */
    private void checkAssignable(String name, Token token) {
        if (externalVars.contains(name)) {
            throw BatteryScriptException.from("Cannot assign '%s', which is a registered namespace"
                    .formatted(name), parser, token);
        }
    }

    @Override
    public AtomValue visitAssignExpr(BatteryParser.AssignExprContext ctx) {
        String id = ctx.identifier().getText();
        checkAssignable(id, ctx.identifier().getStart());

        AtomValue atomValue = this.visit(ctx.expression());

        localVars.assign(id, atomValue);

        return atomValue;
    }

    @Override
    public AtomValue visitUnaryMinusExpr(BatteryParser.UnaryMinusExprContext ctx) {
        AtomValue value = this.visit(ctx.expression());
        try {
            return value.negate();
        } catch (UnsupportedOperationException e) {
            throw illegalOperation(ctx.MINUS().getSymbol(), value, e);
        }
    }

    //
    // Arithmetic operators
    //

    @Override
    public AtomValue visitMultiplicationExpr(BatteryParser.MultiplicationExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        AtomValue right = this.visit(ctx.expression(1));

        try {
            return switch (ctx.op.getType()) {
                case BatteryParser.MULT -> left.multiply(right);
                case BatteryParser.DIV -> left.divide(right);
                case BatteryParser.MOD -> left.remainder(right);
                default -> throw new UnsupportedOperationException();
            };
        } catch (UnsupportedOperationException e) {
            throw illegalOperation(ctx.op, left, right, e);
        }
    }

    @Override
    public AtomValue visitAdditiveExpr(BatteryParser.AdditiveExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        AtomValue right = this.visit(ctx.expression(1));

        try {
            return switch (ctx.op.getType()) {
                case BatteryParser.PLUS -> left.plus(right);
                case BatteryParser.MINUS -> left.minus(right);
                default -> throw new UnsupportedOperationException();
            };
        } catch (UnsupportedOperationException e) {
            throw illegalOperation(ctx.op, left, right, e);
        }
    }

    @Override
    public AtomValue visitPowExpr(BatteryParser.PowExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        AtomValue right = this.visit(ctx.expression(1));

        try {
            return left.pow(right);
        } catch (UnsupportedOperationException e) {
            throw illegalOperation(ctx.POW().getSymbol(), left, right, e);
        }
    }

    //
    // Relational operators
    //

    @Override
    public AtomValue visitParExpr(BatteryParser.ParExprContext ctx) {
        return super.visit(ctx.expression());
    }

    @Override
    public AtomValue visitRelationalExpr(BatteryParser.RelationalExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        AtomValue right = this.visit(ctx.expression(1));

        try {
            return switch (ctx.op.getType()) {
                case BatteryParser.LT -> AtomValue.of(left.isLessThan(right));
                case BatteryParser.LE -> AtomValue.of(left.isLessThanOrEqualTo(right));
                case BatteryParser.GT -> AtomValue.of(left.isGreaterThan(right));
                case BatteryParser.GE -> AtomValue.of(left.isGreaterThanOrEqualTo(right));
                default -> throw new UnsupportedOperationException();
            };
        } catch (UnsupportedOperationException e) {
            throw illegalOperation(ctx.op, left, right, e);
        }
    }

    @Override
    public AtomValue visitEqualityExpr(BatteryParser.EqualityExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        AtomValue right = this.visit(ctx.expression(1));

        try {
            return switch (ctx.op.getType()) {
                case BatteryParser.EQ -> AtomValue.of(isEqual(left, right));
                case BatteryParser.NE -> AtomValue.of(!isEqual(left, right));
                default -> throw new UnsupportedOperationException();
            };
        } catch (UnsupportedOperationException e) {
            throw illegalOperation(ctx.op, left, right, e);
        }
    }

    private static boolean isEqual(AtomValue left, AtomValue right) {
        // Null is only equal to null, so atoms need not handle null operands
        if (left instanceof NullAtom || right instanceof NullAtom) {
            return left instanceof NullAtom && right instanceof NullAtom;
        }
        return left.isEqualTo(right);
    }

    @Override
    public AtomValue visitInExpr(BatteryParser.InExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        AtomValue right = this.visit(ctx.expression(1));

        try {
            return AtomValue.of(right.in(left));
        } catch (UnsupportedOperationException e) {
            throw illegalOperation(ctx.IN().getSymbol(), left, right, e);
        }
    }

    @Override
    public AtomValue visitNotInExpr(BatteryParser.NotInExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        AtomValue right = this.visit(ctx.expression(1));

        try {
            return AtomValue.of(!right.in(left));
        } catch (UnsupportedOperationException e) {
            throw illegalOperation(ctx.IN().getSymbol(), left, right, e);
        }
    }

    //
    // Logical operators
    //

    @Override
    public AtomValue visitAndExpr(BatteryParser.AndExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        if (!left.asBoolean()) {
            return left;
        }
        return this.visit(ctx.expression(1));
    }

    @Override
    public AtomValue visitOrExpr(BatteryParser.OrExprContext ctx) {
        AtomValue left = this.visit(ctx.expression(0));
        if (left.asBoolean()) {
            return left;
        }
        return this.visit(ctx.expression(1));
    }

    @Override
    public AtomValue visitNotExpr(BatteryParser.NotExprContext ctx) {
        AtomValue value = this.visit(ctx.expression());
        return AtomValue.of(!value.asBoolean());
    }

    //
    // Branch loop statements
    //

    @Override
    public AtomValue visitIf_statement(BatteryParser.If_statementContext ctx) {
        for (BatteryParser.Condition_blockContext condCtx : ctx.condition_block()) {
            AtomValue condition = visit(condCtx.expression());
            if (condition != null && condition.asBoolean()) {
                return visit(condCtx.statement_block());
            }
        }

        if (ctx.statement_block() != null) {
            return visit(ctx.statement_block());
        }

        return VOID;
    }

    @Override
    public AtomValue visitWhile_statement(BatteryParser.While_statementContext ctx) {
        AtomValue value = this.visit(ctx.expression());

        while (value.asBoolean()) {
            this.visit(ctx.statement_block());
            value = this.visit(ctx.expression());
        }

        return VOID;
    }

    @Override
    public AtomValue visitForeach_statement(BatteryParser.Foreach_statementContext ctx) {
        AtomValue value = this.visit(ctx.expression());

        // Loop scope for the implicit loop variables, shadowing those of any enclosing loop
        VariableScope<AtomValue> prevScope = this.localVars;
        this.localVars = new VariableScope<>(prevScope);

        int index = 0;
        try {
            for (Object element : value.asIterable()) {
                localVars.define(Constants.LOOP_INDEX_KEYWORD,
                        AtomValue.of(element));
                localVars.define(Constants.FOR_EACH_INDEX_KEYWORD,
                        AtomValue.of(index++));
                this.visit(ctx.statement_block());
            }
        } catch (UnsupportedOperationException e) {
            throw BatteryScriptException.from("Branch loop '%s' cannot be applied to '%s'"
                            .formatted(tokenDisplayName(BatteryParser.FOREACH), Objects.toString(value)),
                    parser, ctx.getStop(), e);
        } finally {
            this.localVars = prevScope;
        }

        return VOID;
    }

    @Override
    public AtomValue visitFor_statement(BatteryParser.For_statementContext ctx) {
        AtomValue initialState = this.visit(ctx.expression(0));
        AtomValue terminalState = this.visit(ctx.expression(1));

        String loopVariable = ctx.identifier().getText();
        checkAssignable(loopVariable, ctx.identifier().getStart());

        int from = initialState.asInteger();
        int to = terminalState.asInteger();

        // Loop scope for the loop variable, shadowing any variable with the same name
        VariableScope<AtomValue> prevScope = this.localVars;
        this.localVars = new VariableScope<>(prevScope);
        try {
            IntStream.rangeClosed(from, to).forEach(value -> {
                localVars.define(loopVariable, AtomValue.of(value));
                this.visit(ctx.statement_block());
            });
        } finally {
            this.localVars = prevScope;
        }

        return VOID;
    }

    @Override
    public AtomValue visitStatement_block(BatteryParser.Statement_blockContext ctx) {
        if (ctx.block() != null) {
            VariableScope<AtomValue> prevScope = this.localVars;
            try {
                this.localVars = new VariableScope<>(prevScope);
                return this.visit(ctx.block());
            } finally {
                this.localVars = prevScope;
            }
        }

        return visit(ctx.statement());
    }

    //
    // Fork-join concurrency statements
    //

    private static class ReturnException extends RuntimeException {
        private final AtomValue value;

        public ReturnException(AtomValue value) {
            super(null, null, false, false);
            this.value = value;
        }

        public AtomValue getValue() {
            return value;
        }
    }

    @Override
    public AtomValue visitReturning(BatteryParser.ReturningContext ctx) {
        AtomValue value = this.visit(ctx.expression());
        throw new ReturnException(value); // Enables short-circuits
    }

    @Override
    public AtomValue visitFork_statement(BatteryParser.Fork_statementContext ctx) {
        // Clone the visitor on the forking thread, so it captures the scope at the fork statement
        BatteryParserTreeVisitor threadVisitor = new BatteryParserTreeVisitor(this);

        CompletableFuture<AtomValue> futureValue = CompletableFuture.supplyAsync(
                () -> threadVisitor.visit(ctx.statement_block()), FORK_EXECUTOR
        ).handle((atomValue, throwable) -> {
            if (throwable != null) {
                Throwable cause = NestedExceptionUtils.getMostSpecificCause(throwable);
                if (cause instanceof ReturnException) {
                    return ((ReturnException) cause).getValue();
                }
                if (throwable instanceof CompletionException) {
                    throw (CompletionException) throwable.fillInStackTrace();
                }
                throw new CompletionException(throwable);
            }
            return atomValue;
        });

        String futureName = ctx.identifier().getText();
        checkAssignable(futureName, ctx.identifier().getStart());
        AtomValue futureAtomValue = AtomValue.of(futureValue);

        localVars.assign(futureName, futureAtomValue);

        return futureAtomValue;
    }

    @Override
    public AtomValue visitJoining(BatteryParser.JoiningContext ctx) {
        final List<CompletableFuture<?>> allFutures = new ArrayList<>();

        if (ctx.identifier() != null) {
            final String id = ctx.identifier().getText();

            AtomValue v = lookupVariable(id).orElseThrow(() ->
                    BatteryScriptException.from("Cannot resolve symbol '" + id + "'", parser,
                            ctx.identifier().getStop()));

            allFutures.add((CompletableFuture<?>) v.asObject());
        }

        BatteryParser.ArrayContext array = ctx.array();
        if (array != null) {
            BatteryParser.Expression_listContext listContext = array.expression_list();
            for (BatteryParser.ExpressionContext c : listContext.expression()) {
                AtomValue v = this.visit(c);
                allFutures.add((CompletableFuture<?>) v.asObject());
            }
        }

        if (!allFutures.isEmpty()) {
            CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[] {})).join();
        }

        return VOID;
    }

    //
    // External and built-in functions
    //

    @Override
    public AtomValue visitNestedExpr(BatteryParser.NestedExprContext ctx) {
        final AtomValue objectRef = this.visit(ctx.expression());
        String id = ctx.identifier().getText();

        // Check for namespace atom for special treatment
        if (objectRef instanceof NamespaceAtom) {
            return new NamespaceAtom((NamespaceAtom) objectRef, id);
        }

        // Otherwise assume its field access
        Object rv = ReflectionSupport.accessField(objectRef.asObject(), objectRef.asClass(), id);

        return AtomValue.of(rv);
    }

    @Override
    public AtomValue visitMethodCallExpr(BatteryParser.MethodCallExprContext ctx) {
        final AtomValue objectRef = this.visit(ctx.expression());

        final List<Object> args = new ArrayList<>();
        if (ctx.expression_list() != null) {
            for (BatteryParser.ExpressionContext exprCtx : ctx.expression_list().expression()) {
                args.add(this.visit(exprCtx).asObject());
            }
        }

        String methodName = ctx.identifier().getText();

        Class<?> clazz;
        Object target;

        if (objectRef instanceof NamespaceAtom) {
            NamespaceAtom namespaceAtom = new NamespaceAtom((NamespaceAtom) objectRef, methodName);
            clazz = namespaceAtom.asClass();
            target = null;
        } else {
            clazz = objectRef.asClass();
            target = objectRef.asObject();
        }

        try {
            Object rv = ReflectionSupport.invoke(target, clazz, methodName, args);
            return AtomValue.of(rv);
        } catch (Exception e) {
            throw BatteryScriptException.from("Error invoking method '%s' on '%s'"
                            .formatted(methodName, clazz.getName()),
                    parser, ctx.getStop(), e);
        }

    }
}
