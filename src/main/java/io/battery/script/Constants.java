package io.battery.script;

import java.util.HashSet;
import java.util.Set;

/**
 * Constants used by battery script, including the names of implicit variables such as
 * the last expression result and the foreach loop element and index. The foreach loop
 * variables are reserved and cannot be assigned by scripts.
 */
public abstract class Constants {
    /**
     * Top result for unterminated expressions/statements.
     */
    public static final String LAST_RESULT_VAR = "result";

    /**
     * for-loop index variable.
     */
    public static final String FOR_EACH_INDEX_KEYWORD = "_forEachIndex";

    /**
     * Loop current index variable.
     */
    public static final String LOOP_INDEX_KEYWORD = "_x";

    public static final Set<String> RESERVED_KEYWORDS = new HashSet<>();

    static {
        Constants.RESERVED_KEYWORDS.add(Constants.FOR_EACH_INDEX_KEYWORD);
        Constants.RESERVED_KEYWORDS.add(Constants.LOOP_INDEX_KEYWORD);
    }

    private Constants() {
    }
}
