package io.battery.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a SQL script into statements at semicolons, ignoring semicolons inside quoted strings,
 * quoted identifiers, dollar-quoted strings (as used for PostgreSQL function bodies) and
 * comments, which are kept with their statement.
 */
public abstract class SqlStatements {
    private SqlStatements() {
    }

    public static List<String> parse(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean singleQuote = false;
        boolean doubleQuote = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            char next = i + 1 < script.length() ? script.charAt(i + 1) : '\0';

            if (lineComment) {
                current.append(c);
                if (c == '\n') {
                    lineComment = false;
                }
                continue;
            }

            if (blockComment) {
                current.append(c);
                if (c == '*' && next == '/') {
                    current.append(next);
                    i++;
                    blockComment = false;
                }
                continue;
            }

            if (!singleQuote && !doubleQuote) {
                if (c == '-' && next == '-') {
                    current.append(c).append(next);
                    i++;
                    lineComment = true;
                    continue;
                }

                if (c == '/' && next == '*') {
                    current.append(c).append(next);
                    i++;
                    blockComment = true;
                    continue;
                }

                // A dollar-quoted string such as $$...$$ or $body$...$body$, unless the $ is
                // part of an identifier, is kept as is up to the same closing tag
                String tag = i > 0 && isIdentifierPart(script.charAt(i - 1)) ? null : dollarQuoteTag(script, i);
                if (tag != null) {
                    int end = script.indexOf(tag, i + tag.length());
                    if (end < 0) {
                        throw new IllegalArgumentException("SQL script ends inside a dollar-quoted string");
                    }
                    current.append(script, i, end + tag.length());
                    i = end + tag.length() - 1;
                    continue;
                }
            }

            if (c == '\'' && !doubleQuote) {
                current.append(c);

                // SQL escapes a quote as ''
                if (singleQuote && next == '\'') {
                    current.append(next);
                    i++;
                } else {
                    singleQuote = !singleQuote;
                }
                continue;
            }

            if (c == '"' && !singleQuote) {
                current.append(c);

                // Escaped quoted identifier: ""
                if (doubleQuote && next == '"') {
                    current.append(next);
                    i++;
                } else {
                    doubleQuote = !doubleQuote;
                }
                continue;
            }

            if (c == ';' && !singleQuote && !doubleQuote) {
                addIfNotBlank(statements, current);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (singleQuote || doubleQuote || blockComment) {
            throw new IllegalArgumentException(
                    "SQL script ends inside a string, identifier, or block comment"
            );
        }

        addIfNotBlank(statements, current);
        return statements;
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /**
     * @return the opening tag of a dollar-quoted string at the index, such as {@code $$} or
     * {@code $body$}, or null if there's none (as for a positional parameter like {@code $1})
     */
    private static String dollarQuoteTag(String script, int index) {
        if (script.charAt(index) != '$') {
            return null;
        }
        int end = index + 1;
        // A tag starts like an identifier, so it can't start with a digit
        if (end < script.length() && (Character.isLetter(script.charAt(end)) || script.charAt(end) == '_')) {
            while (end < script.length()
                   && (Character.isLetterOrDigit(script.charAt(end)) || script.charAt(end) == '_')) {
                end++;
            }
        }
        return end < script.length() && script.charAt(end) == '$'
                ? script.substring(index, end + 1)
                : null;
    }

    private static void addIfNotBlank(List<String> statements, StringBuilder statement) {
        String sql = statement.toString().trim();
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
    }
}
