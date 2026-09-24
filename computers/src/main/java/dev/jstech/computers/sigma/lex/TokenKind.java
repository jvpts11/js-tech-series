/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lex;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Every kind of token the language has.
 *
 * <p>The keyword set is closed on purpose: a word that is not here is an identifier, so a program
 * never stops compiling because a later version of the language claimed one of the player's names.
 * Each kind carries how it is written, which is what a diagnostic quotes back. A kind that has no one
 * spelling, a number or a name, is described instead, in words read in the player's language.
 */
public enum TokenKind {

    INT_LITERAL(Described.INT_LITERAL),
    LONG_LITERAL(Described.LONG_LITERAL),
    FLOAT_LITERAL(Described.FLOAT_LITERAL),
    DOUBLE_LITERAL(Described.DOUBLE_LITERAL),
    STRING_LITERAL(Described.STRING_LITERAL),
    INTERPOLATED_STRING(Described.INTERPOLATED_STRING),
    CHAR_LITERAL(Described.CHAR_LITERAL),
    IDENTIFIER(Described.IDENTIFIER),

    NAMESPACE("namespace", true),
    USING("using", true),
    CLASS("class", true),
    INTERFACE("interface", true),
    ENUM("enum", true),
    STRUCT("struct", true),
    RECORD("record", true),
    DELEGATE("delegate", true),
    EVENT("event", true),
    NEW("new", true),
    DISPOSE("dispose", true),
    THIS("this", true),
    BASE("base", true),
    STATIC("static", true),
    VIRTUAL("virtual", true),
    OVERRIDE("override", true),
    ABSTRACT("abstract", true),
    PUBLIC("public", true),
    PRIVATE("private", true),
    PROTECTED("protected", true),
    READONLY("readonly", true),
    IF("if", true),
    ELSE("else", true),
    WHILE("while", true),
    FOR("for", true),
    FOREACH("foreach", true),
    LOCK("lock", true),
    IN("in", true),
    OUT("out", true),
    DO("do", true),
    SWITCH("switch", true),
    CASE("case", true),
    DEFAULT("default", true),
    BREAK("break", true),
    CONTINUE("continue", true),
    RETURN("return", true),
    VOID("void", true),
    VAR("var", true),
    NULL("null", true),
    TRUE("true", true),
    FALSE("false", true),
    IS("is", true),
    AS("as", true),
    INT("int", true),
    LONG("long", true),
    FLOAT("float", true),
    DOUBLE("double", true),
    BOOL("bool", true),
    STRING("string", true),
    CHAR("char", true),
    OBJECT("object", true),

    LEFT_BRACE("{"),
    RIGHT_BRACE("}"),
    LEFT_PAREN("("),
    RIGHT_PAREN(")"),
    LEFT_BRACKET("["),
    RIGHT_BRACKET("]"),
    SEMICOLON(";"),
    COMMA(","),
    DOT("."),
    COLON(":"),
    QUESTION("?"),
    ARROW("=>"),

    ASSIGN("="),
    PLUS("+"),
    MINUS("-"),
    STAR("*"),
    SLASH("/"),
    PERCENT("%"),
    PLUS_PLUS("++"),
    MINUS_MINUS("--"),
    PLUS_ASSIGN("+="),
    MINUS_ASSIGN("-="),
    STAR_ASSIGN("*="),
    SLASH_ASSIGN("/="),
    PERCENT_ASSIGN("%="),
    AMPERSAND_ASSIGN("&="),
    PIPE_ASSIGN("|="),
    CARET_ASSIGN("^="),
    SHIFT_LEFT_ASSIGN("<<="),
    SHIFT_RIGHT_ASSIGN(">>="),
    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS("<"),
    LESS_EQUAL("<="),
    GREATER(">"),
    GREATER_EQUAL(">="),
    AND_AND("&&"),
    OR_OR("||"),
    NOT("!"),
    AMPERSAND("&"),
    PIPE("|"),
    CARET("^"),
    TILDE("~"),
    SHIFT_LEFT("<<"),
    SHIFT_RIGHT(">>"),

    END_OF_FILE(Described.END_OF_FILE);

    private static final Map<String, TokenKind> KEYWORDS;

    static {
        final Map<String, TokenKind> keywords = new HashMap<>();
        for (final TokenKind kind : values()) {
            if (kind.keyword) {
                keywords.put(kind.text, kind);
            }
        }
        KEYWORDS = Collections.unmodifiableMap(keywords);
    }

    private final String text;
    private final boolean keyword;

    /*
     * What a kind with no one spelling is called, or null for one that is spelled. Kept as text and not as its key:
     * the sentences are all declared in Described, and a key held by only some of the constants would leave the
     * language generator a constant with none to read.
     */
    private final Text description;

    TokenKind(final String text) {
        this(text, false);
    }

    TokenKind(final String text, final boolean keyword) {
        this.text = text;
        this.keyword = keyword;
        this.description = null;
    }

    TokenKind(final TextKey description) {
        this.text = description.english();
        this.keyword = false;
        this.description = description.text();
    }

    /** The kind of the reserved word spelled {@code word}, or {@code null} if it is a plain name. */
    public static TokenKind keyword(final String word) {
        return KEYWORDS.get(word);
    }

    /** How this kind is written, for the punctuation and the keywords; the English description for the rest. */
    public String text() {
        return this.text;
    }

    /** Whether this kind is one of the language's reserved words. */
    public boolean isKeyword() {
        return this.keyword;
    }

    /** How a diagnostic names this kind: quoted when it has one spelling, described when it is a class. */
    public Text describe() {
        return this.description != null ? this.description : Text.literal("'" + this.text + "'");
    }

    /** What the kinds with no one spelling are called in a diagnostic, as in "expected a name but found ...". */
    @TextHolder
    private static final class Described {

        static final TextKey INT_LITERAL = TextKey.of("jsc.sigma.token_kind.int_literal", "an integer");
        static final TextKey LONG_LITERAL = TextKey.of("jsc.sigma.token_kind.long_literal", "a long integer");
        static final TextKey FLOAT_LITERAL = TextKey.of("jsc.sigma.token_kind.float_literal", "a float");
        static final TextKey DOUBLE_LITERAL = TextKey.of("jsc.sigma.token_kind.double_literal", "a double");
        static final TextKey STRING_LITERAL = TextKey.of("jsc.sigma.token_kind.string_literal", "a string");
        static final TextKey INTERPOLATED_STRING = TextKey.of("jsc.sigma.token_kind.interpolated_string",
                "an interpolated string");
        static final TextKey CHAR_LITERAL = TextKey.of("jsc.sigma.token_kind.char_literal", "a character");
        static final TextKey IDENTIFIER = TextKey.of("jsc.sigma.token_kind.identifier", "a name");
        static final TextKey END_OF_FILE = TextKey.of("jsc.sigma.token_kind.end_of_file", "the end of the file");

        private Described() {
        }
    }
}
