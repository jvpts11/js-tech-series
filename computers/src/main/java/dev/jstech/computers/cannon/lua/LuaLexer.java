/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import dev.jstech.computers.cannon.CannonError;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.lua.lib.LuaNumbers;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns Lua source into tokens.
 *
 * <p>Both comment forms, both quote characters, long strings with any number of equals signs, every
 * escape the language has and its two kinds of number are read here, so that a program written for
 * another Lua reads the same on this one.
 */
public final class LuaLexer {

    /** One line's worth of a comment, for an editor to colour: a long comment gives one per line. */
    public record Comment(int line, int column, int length) {
    }

    private final SourceFile source;
    private final DiagnosticBag diagnostics;
    private final List<Comment> comments = new ArrayList<>();
    private int index;
    private int line = 1;
    private int column = 1;

    public LuaLexer(final SourceFile source, final DiagnosticBag diagnostics) {
        this.source = source;
        this.diagnostics = diagnostics;
    }

    /** The comments the last {@link #tokenize()} passed over. */
    public List<Comment> comments() {
        return List.copyOf(this.comments);
    }

    private void comment(final int start, final int startLine, final int startColumn) {
        final String text = this.source.text().substring(start, this.index);
        final String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            final String piece = lines[i].endsWith("\r") ? lines[i].substring(0, lines[i].length() - 1) : lines[i];
            if (!piece.isEmpty()) {
                this.comments.add(new Comment(startLine + i, i == 0 ? startColumn : 1, piece.length()));
            }
        }
    }

    public List<LuaToken> tokenize() {
        final List<LuaToken> tokens = new ArrayList<>();
        if (this.source.length() > 0 && this.source.charAt(0) == '#') {
            // A first line beginning with a hash is a shebang, and is skipped as the language does.
            while (this.index < this.source.length() && this.peek() != '\n') {
                this.advance();
            }
        }
        while (true) {
            this.skipTrivia();
            if (this.index >= this.source.length()) {
                break;
            }
            final LuaToken token = this.scanToken();
            if (token != null) {
                tokens.add(token);
            }
        }
        tokens.add(LuaToken.of(LuaTokenKind.END_OF_FILE, "", this.line, this.column));
        return tokens;
    }

    private void skipTrivia() {
        while (this.index < this.source.length()) {
            final char c = this.peek();
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f' || c == 0x0B) {
                this.advance();
            } else if (c == '-' && this.peek(1) == '-') {
                final int start = this.index;
                final int startLine = this.line;
                final int startColumn = this.column;
                this.advance();
                this.advance();
                final int level = this.longBracketLevel();
                if (level >= 0) {
                    this.scanLongBracket(level, startLine, startColumn, "comment");
                } else {
                    while (this.index < this.source.length() && this.peek() != '\n') {
                        this.advance();
                    }
                }
                this.comment(start, startLine, startColumn);
            } else {
                return;
            }
        }
    }

    private LuaToken scanToken() {
        final int startIndex = this.index;
        final int startLine = this.line;
        final int startColumn = this.column;
        final char c = this.peek();
        if (Character.isLetter(c) || c == '_') {
            return this.scanWord(startIndex, startLine, startColumn);
        }
        if (Character.isDigit(c) || (c == '.' && Character.isDigit(this.peek(1)))) {
            return this.scanNumber(startIndex, startLine, startColumn);
        }
        if (c == '"' || c == '\'') {
            return this.scanString(startLine, startColumn);
        }
        if (c == '[') {
            final int level = this.longBracketLevel();
            if (level >= 0) {
                final String text = this.scanLongBracket(level, startLine, startColumn, "string");
                return new LuaToken(LuaTokenKind.STRING, this.textFrom(startIndex), text, startLine, startColumn);
            }
        }
        return this.scanSymbol(startIndex, startLine, startColumn);
    }

    private LuaToken scanWord(final int startIndex, final int startLine, final int startColumn) {
        while (this.index < this.source.length()
                && (Character.isLetterOrDigit(this.peek()) || this.peek() == '_')) {
            this.advance();
        }
        final String text = this.textFrom(startIndex);
        final LuaTokenKind keyword = LuaTokenKind.keyword(text);
        return LuaToken.of(keyword == null ? LuaTokenKind.NAME : keyword, text, startLine, startColumn);
    }

    private LuaToken scanNumber(final int startIndex, final int startLine, final int startColumn) {
        final boolean hex = this.peek() == '0' && (this.peek(1) == 'x' || this.peek(1) == 'X');
        if (hex) {
            this.advance();
            this.advance();
        }
        while (this.index < this.source.length()) {
            final char c = this.peek();
            final boolean exponent = hex ? (c == 'p' || c == 'P') : (c == 'e' || c == 'E');
            if (exponent && (this.peek(1) == '+' || this.peek(1) == '-')) {
                this.advance();
                this.advance();
                continue;
            }
            if (Character.isLetterOrDigit(c) || c == '.' || c == '_') {
                this.advance();
                continue;
            }
            break;
        }
        final String text = this.textFrom(startIndex);
        final Object value = hex ? hexNumber(text) : LuaNumbers.parse(text);
        if (value == null) {
            this.diagnostics.error(startLine, startColumn, CannonError.LUA_MALFORMED_NUMBER, text);
            return new LuaToken(LuaTokenKind.NUMBER, text, 0L, startLine, startColumn);
        }
        return new LuaToken(LuaTokenKind.NUMBER, text, value, startLine, startColumn);
    }

    /* A hexadecimal number: whole digits, and a real one when it has a point or a binary exponent. */
    private static Object hexNumber(final String text) {
        final String body = text.substring(2);
        if (body.indexOf('.') < 0 && body.indexOf('p') < 0 && body.indexOf('P') < 0) {
            return LuaNumbers.parse(text);
        }
        try {
            final String java = body.indexOf('p') < 0 && body.indexOf('P') < 0 ? "0x" + body + "p0" : "0x" + body;
            return Double.parseDouble(java);
        } catch (final NumberFormatException notANumber) {
            return null;
        }
    }

    private LuaToken scanString(final int startLine, final int startColumn) {
        final int startIndex = this.index;
        final char quote = this.advance();
        final StringBuilder text = new StringBuilder();
        while (true) {
            if (this.index >= this.source.length() || this.peek() == '\n') {
                this.diagnostics.error(startLine, startColumn, CannonError.LUA_UNTERMINATED_STRING);
                break;
            }
            final char c = this.peek();
            if (c == quote) {
                this.advance();
                break;
            }
            if (c == '\\') {
                this.scanEscape(text);
                continue;
            }
            text.append(this.advance());
        }
        return new LuaToken(LuaTokenKind.STRING, this.textFrom(startIndex), text.toString(), startLine, startColumn);
    }

    private void scanEscape(final StringBuilder into) {
        final int escapeLine = this.line;
        final int escapeColumn = this.column;
        this.advance();
        if (this.index >= this.source.length()) {
            return;
        }
        final char c = this.peek();
        switch (c) {
            case 'n' -> {
                into.append('\n');
                this.advance();
            }
            case 't' -> {
                into.append('\t');
                this.advance();
            }
            case 'r' -> {
                into.append('\r');
                this.advance();
            }
            case 'a' -> {
                into.append('');
                this.advance();
            }
            case 'b' -> {
                into.append('\b');
                this.advance();
            }
            case 'f' -> {
                into.append('\f');
                this.advance();
            }
            case 'v' -> {
                into.append('');
                this.advance();
            }
            case '\\', '"', '\'' -> into.append(this.advance());
            case '\n' -> {
                into.append('\n');
                this.advance();
            }
            case 'x' -> {
                this.advance();
                int value = 0;
                for (int i = 0; i < 2; i++) {
                    final int digit = Character.digit(this.peek(), 16);
                    if (digit < 0) {
                        this.diagnostics.error(escapeLine, escapeColumn, CannonError.LUA_BAD_ESCAPE, "x");
                        return;
                    }
                    value = value * 16 + digit;
                    this.advance();
                }
                into.append((char) value);
            }
            case 'z' -> {
                this.advance();
                while (this.index < this.source.length() && Character.isWhitespace(this.peek())) {
                    this.advance();
                }
            }
            case 'u' -> {
                this.advance();
                if (this.peek() != '{') {
                    this.diagnostics.error(escapeLine, escapeColumn, CannonError.LUA_BAD_ESCAPE, "u");
                    return;
                }
                this.advance();
                int value = 0;
                while (this.index < this.source.length() && this.peek() != '}') {
                    final int digit = Character.digit(this.peek(), 16);
                    if (digit < 0) {
                        this.diagnostics.error(escapeLine, escapeColumn, CannonError.LUA_BAD_ESCAPE, "u");
                        return;
                    }
                    value = value * 16 + digit;
                    this.advance();
                }
                this.advance();
                into.appendCodePoint(value);
            }
            default -> {
                if (Character.isDigit(c)) {
                    int value = 0;
                    for (int i = 0; i < 3 && Character.isDigit(this.peek()); i++) {
                        value = value * 10 + (this.advance() - '0');
                    }
                    if (value > 255) {
                        this.diagnostics.error(escapeLine, escapeColumn, CannonError.LUA_BAD_ESCAPE,
                                String.valueOf(value));
                        return;
                    }
                    into.append((char) value);
                    return;
                }
                this.diagnostics.error(escapeLine, escapeColumn, CannonError.LUA_BAD_ESCAPE, String.valueOf(c));
                this.advance();
            }
        }
    }

    /** The level of a long bracket starting here ({@code [[} is 0, {@code [==[} is 2), or -1 for none. */
    private int longBracketLevel() {
        if (this.peek() != '[') {
            return -1;
        }
        int level = 0;
        while (this.peek(1 + level) == '=') {
            level++;
        }
        return this.peek(1 + level) == '[' ? level : -1;
    }

    /** Reads a long bracket of that level, the opening bracket included, and gives back what it holds. */
    private String scanLongBracket(final int level, final int startLine, final int startColumn, final String what) {
        for (int i = 0; i < level + 2; i++) {
            this.advance();
        }
        // A newline right after the opening bracket is not part of the text.
        if (this.peek() == '\r') {
            this.advance();
        }
        if (this.peek() == '\n') {
            this.advance();
        }
        final StringBuilder text = new StringBuilder();
        while (true) {
            if (this.index >= this.source.length()) {
                this.diagnostics.error(startLine, startColumn, CannonError.LUA_UNTERMINATED_LONG, what);
                return text.toString();
            }
            if (this.peek() == ']') {
                int closing = 0;
                while (this.peek(1 + closing) == '=') {
                    closing++;
                }
                if (closing == level && this.peek(1 + closing) == ']') {
                    for (int i = 0; i < level + 2; i++) {
                        this.advance();
                    }
                    return text.toString();
                }
            }
            final char c = this.advance();
            if (c != '\r') {
                text.append(c);
            }
        }
    }

    private LuaToken scanSymbol(final int startIndex, final int startLine, final int startColumn) {
        final char c = this.advance();
        final LuaTokenKind kind = switch (c) {
            case '+' -> LuaTokenKind.PLUS;
            case '-' -> LuaTokenKind.MINUS;
            case '*' -> LuaTokenKind.STAR;
            case '/' -> this.take('/') ? LuaTokenKind.DOUBLE_SLASH : LuaTokenKind.SLASH;
            case '%' -> LuaTokenKind.PERCENT;
            case '^' -> LuaTokenKind.CARET;
            case '#' -> LuaTokenKind.HASH;
            case '=' -> this.take('=') ? LuaTokenKind.EQUAL : LuaTokenKind.ASSIGN;
            case '~' -> this.take('=') ? LuaTokenKind.NOT_EQUAL : null;
            case '<' -> this.take('=') ? LuaTokenKind.LESS_EQUAL : LuaTokenKind.LESS;
            case '>' -> this.take('=') ? LuaTokenKind.GREATER_EQUAL : LuaTokenKind.GREATER;
            case '(' -> LuaTokenKind.OPEN_PAREN;
            case ')' -> LuaTokenKind.CLOSE_PAREN;
            case '{' -> LuaTokenKind.OPEN_BRACE;
            case '}' -> LuaTokenKind.CLOSE_BRACE;
            case '[' -> LuaTokenKind.OPEN_BRACKET;
            case ']' -> LuaTokenKind.CLOSE_BRACKET;
            case ';' -> LuaTokenKind.SEMICOLON;
            case ':' -> this.take(':') ? LuaTokenKind.DOUBLE_COLON : LuaTokenKind.COLON;
            case ',' -> LuaTokenKind.COMMA;
            case '.' -> this.take('.') ? (this.take('.') ? LuaTokenKind.ELLIPSIS : LuaTokenKind.CONCAT)
                    : LuaTokenKind.DOT;
            default -> null;
        };
        if (kind == null) {
            this.diagnostics.error(startLine, startColumn, CannonError.LUA_UNEXPECTED_CHARACTER,
                    this.textFrom(startIndex));
            return null;
        }
        return LuaToken.of(kind, this.textFrom(startIndex), startLine, startColumn);
    }

    private boolean take(final char wanted) {
        if (this.peek() == wanted) {
            this.advance();
            return true;
        }
        return false;
    }

    private char peek() {
        return this.source.charAt(this.index);
    }

    private char peek(final int ahead) {
        return this.source.charAt(this.index + ahead);
    }

    private char advance() {
        final char c = this.source.charAt(this.index++);
        if (c == '\n') {
            this.line++;
            this.column = 1;
        } else {
            this.column++;
        }
        return c;
    }

    private String textFrom(final int start) {
        return this.source.text().substring(start, this.index);
    }
}
