package de.edgelord.edgyscript.e80.interpreter.token;

import de.edgelord.edgyscript.e80.interpreter.Interpreter;
import de.edgelord.edgyscript.e80.interpreter.LinkedValue;
import de.edgelord.edgyscript.e80.interpreter.Value;
import de.edgelord.edgyscript.e80.interpreter.token.tokens.InlineToken;
import de.edgelord.edgyscript.e80.interpreter.token.tokens.SpecialToken;
import de.edgelord.edgyscript.e80.interpreter.token.tokens.ValueToken;
import de.edgelord.edgyscript.e80.script.ScriptException;

import java.io.Serializable;

public abstract class Token implements Serializable {

    public abstract String getID();
    public abstract String getValue();
    public abstract void setValue(String newValue);
    public abstract Value toValue();
    public abstract boolean isEqualSign();
    public abstract boolean isBracket();

    public boolean isDelimiterSymbol() {
        return false;
    }

    private ValueType valueType;

    public Token(ValueType valueType) {
        this.valueType = valueType;
    }

    public double getNumber() {
        double val;
        try {
            val = Double.parseDouble(getValue());
        } catch (Exception e) {
            throw new ScriptException("Cannot use \"" + getValue() + "\" as a number!");
        }
        return val;
    }

    public int getInt() {
        int val;
        try {
            val = Integer.parseInt(getValue());
        } catch (Exception e) {
            throw new ScriptException("Cannot use \"" + getValue() + "\" as an Integer!");
        }
        return val;
    }

    public boolean getBoolean() {
        boolean val;
        try {
            val = Boolean.parseBoolean(getValue());
        } catch (Exception e) {
            throw new ScriptException("Cannot use \"" + getValue() + "\" as a boolean!");
        }
        return val;
    }

    public boolean isBoolean() {
        return getValue().equalsIgnoreCase("true") || getValue().equalsIgnoreCase("false");
    }

    public boolean isNumeric() {
        char[] chars = getValue().toCharArray();

        for (char c : chars) {
            if (!(Character.isDigit(c) || c == '.')) {
                return false;
            }
        }
        return true;
    }

    public ValueType getValueType() {
        if (valueType == ValueType.AUTO) {

            if (getValue() == null) {
                return ValueType.NUMBER;
            } else if (isBoolean()) {
                return ValueType.BOOLEAN;
            } else if (isNumeric() || Interpreter.isOperator(getValue())) {
                return ValueType.NUMBER;
            } else {
                return ValueType.STRING;
            }
        } else {
            return valueType;
        }
    }

    public String getValueForJS() {
        switch (getValueType()) {
            case NUMBER:
            case BOOLEAN:
                return getValue();
            case STRING:

                String valueForJS = getValue().replace("\"", "\\\"");
                return "\"" + valueForJS + "\"";
        }
        return null;
    }

    public static Token getToken(String s, Type type, ValueType valueType) {
        switch (type) {
            case SPECIAL:
            case SPLIT:

                if (Interpreter.isKeyWord(s) || Interpreter.isOperator(s) || Interpreter.isSplitChar(s)) {
                    return new SpecialToken(s, valueType);
                } else {
                    if (s.contains(":")) {
                        String[] parts = s.split(":");

                        if (parts.length < 2) {
                            return new LinkedValue(s);
                        }
                        return new LinkedValue(parts[1], ValueType.getType(parts[0]));
                    } else {
                        return new LinkedValue(s, ValueType.AUTO);
                    }
                }
            case VALUE:
                return new ValueToken(s, valueType);
            case INLINE:
                return new InlineToken(s, valueType);
        }

        return null;
    }

    /**
     * Sets {@link #valueType}.
     *
     * @param valueType the new value of {@link #valueType}
     */
    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public enum ValueType {
        NUMBER,
        STRING,
        BOOLEAN,
        AUTO;

        public static ValueType getType(String typeName) {
            switch (typeName.toLowerCase()) {
                case "number":
                    return NUMBER;
                case "string":
                    return STRING;
                case "boolean":
                case "bool":
                    return BOOLEAN;
            }

            throw new ScriptException("Variable type " + typeName + " does not exist!");
        }
    }

    public enum Type {
        // The type of token that stores a special value (function name, keyword, operator, ...)
        SPECIAL,
        // the type of token that stores a direct value
        VALUE,
        // the type of token that stores a split char (,)
        SPLIT,
        // the type of token that stores an inline value (z.b. [md5Hash "Hello World!"] -> md5Hash "Hello World!")
        INLINE
    }
}
