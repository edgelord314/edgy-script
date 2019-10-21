package de.edgelord.edgyscript.e80.interpreter;

import de.edgelord.edgyscript.e80.interpreter.token.Token;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Tokenizer {

    /**
     * !! Only works with arguments, as otherwise the function name will be considered a variable
     *
     * @param tokens teh arguments to a function
     * @return a corresponding list of {@link Value}s to the given list of {@link Token}s.
     */
    public static List<Value> evaluateTokens(List<Token> tokens, boolean... spaceSeparatorMode) {
        List<Value> values = new ArrayList<>();
        List<Token> currentTokens = new LinkedList<>();

        if (spaceSeparatorMode.length > 0) {
            if (spaceSeparatorMode[0]) {
                for (Token token : tokens) {
                    values.add(token.toValue());
                }
                return values;
            }
        }

        for (Token token : tokens) {
            String tokenValue = token.getValue();
            if (Interpreter.isSplitChar(tokenValue)) {
                addToValueList(values, currentTokens);
                currentTokens.clear();
            } else {
                currentTokens.add(token);
            }
        }

        if (currentTokens.size() > 0) {

            addToValueList(values, currentTokens);
        }

        return values;
    }

    private static void addToValueList(List<Value> values, List<Token> currentTokens) {
        if (currentTokens.size() == 1) {
            values.add(currentTokens.get(0).toValue());
        } else {
            values.add(evaluateSingle(currentTokens));
        }
    }

    public static Value evaluateSingle(List<Token> tokens) {
        StringBuilder value = new StringBuilder();

        for (Token token : tokens) {
            value.append(token.getValueForJS());
        }

        return new JSPoweredValue(value.toString());
    }
}