/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import java.util.HashMap;
import java.util.Map;

/** Every kind of token Lua source is made of. */
public enum LuaTokenKind {
    NAME("a name"),
    NUMBER("a number"),
    STRING("a string"),
    AND("and", true),
    BREAK("break", true),
    DO("do", true),
    ELSE("else", true),
    ELSEIF("elseif", true),
    END("end", true),
    FALSE("false", true),
    FOR("for", true),
    FUNCTION("function", true),
    GOTO("goto", true),
    IF("if", true),
    IN("in", true),
    LOCAL("local", true),
    NIL("nil", true),
    NOT("not", true),
    OR("or", true),
    REPEAT("repeat", true),
    RETURN("return", true),
    THEN("then", true),
    TRUE("true", true),
    UNTIL("until", true),
    WHILE("while", true),
    PLUS("+"),
    MINUS("-"),
    STAR("*"),
    SLASH("/"),
    DOUBLE_SLASH("//"),
    PERCENT("%"),
    CARET("^"),
    HASH("#"),
    EQUAL("=="),
    NOT_EQUAL("~="),
    LESS_EQUAL("<="),
    GREATER_EQUAL(">="),
    LESS("<"),
    GREATER(">"),
    ASSIGN("="),
    OPEN_PAREN("("),
    CLOSE_PAREN(")"),
    OPEN_BRACE("{"),
    CLOSE_BRACE("}"),
    OPEN_BRACKET("["),
    CLOSE_BRACKET("]"),
    DOUBLE_COLON("::"),
    SEMICOLON(";"),
    COLON(":"),
    COMMA(","),
    DOT("."),
    CONCAT(".."),
    ELLIPSIS("..."),
    END_OF_FILE("<eof>");

    private static final Map<String, LuaTokenKind> KEYWORDS = new HashMap<>();

    static {
        for (final LuaTokenKind kind : values()) {
            if (kind.keyword) {
                KEYWORDS.put(kind.text, kind);
            }
        }
    }

    private final String text;
    private final boolean keyword;

    LuaTokenKind(final String text) {
        this(text, false);
    }

    LuaTokenKind(final String text, final boolean keyword) {
        this.text = text;
        this.keyword = keyword;
    }

    /** The keyword this word is, or null when it is a name. */
    public static LuaTokenKind keyword(final String word) {
        return KEYWORDS.get(word);
    }

    public boolean isKeyword() {
        return this.keyword;
    }

    /** How the kind is written, for a message. */
    public String describe() {
        return this.text;
    }
}
