/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lex;

import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.SourceFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Turns source text into tokens.
 *
 * <p>It never throws and never stops early: a character it cannot read becomes a diagnostic and the
 * scan carries on, so one stray symbol does not hide the twenty real mistakes after it. The list it
 * returns always ends with the end-of-file token, which is what lets the parser look ahead without
 * checking bounds.
 */
public final class Lexer {

    private final SourceFile source;
    private final DiagnosticBag diagnostics;
    /** The directives, with the symbols the build defined, which decide which lines are read at all. */
    private final Preprocessor preprocessor;

    private int index;
    private int line;
    private int column;
    /** Whether something other than spaces came on this line yet: a directive is the first thing on its line. */
    private boolean lineStarted;

    /*
     * The digits of the smallest int and long without their minus, one past the largest of each. Read alone they
     * fit no type; the parser takes them only straight after a minus, as -2147483648, and refuses them anywhere
     * else, which is the one way to write those two numbers.
     */
    private static final String INT_MIN_DIGITS = "2147483648";
    private static final String LONG_MIN_DIGITS = "9223372036854775808";

    public Lexer(final SourceFile source, final DiagnosticBag diagnostics) {
        this(source, diagnostics, 1, 1);
    }

    /** A lexer for a whole file, with the symbols the build defines for its directives. */
    public Lexer(final SourceFile source, final DiagnosticBag diagnostics, final Set<String> defined) {
        this(source, diagnostics, 1, 1, defined);
    }

    /**
     * A lexer for a piece cut out of a larger file: its first character is reported at that line and
     * column, so a mistake inside it points at the place it sits in the file.
     */
    public Lexer(final SourceFile source, final DiagnosticBag diagnostics, final int startLine,
                 final int startColumn) {
        this(source, diagnostics, startLine, startColumn, Set.of());
    }

    private Lexer(final SourceFile source, final DiagnosticBag diagnostics, final int startLine,
                  final int startColumn, final Set<String> defined) {
        this.source = source;
        this.diagnostics = diagnostics;
        this.preprocessor = new Preprocessor(diagnostics, defined);
        this.line = startLine;
        this.column = startColumn;
    }

    /** Reads the whole file. The last token is always {@link TokenKind#END_OF_FILE}. */
    public List<Token> tokenize() {
        final List<Token> tokens = new ArrayList<>();
        while (true) {
            this.skipTrivia();
            if (this.index >= this.source.length()) {
                break;
            }
            final Token token = this.scanToken();
            if (token != null) {
                tokens.add(token);
                this.lineStarted = true;
            }
        }
        this.preprocessor.finish();
        tokens.add(Token.of(TokenKind.END_OF_FILE, "", this.line, this.column));
        return tokens;
    }

    /** The directives of the file and the lines they left out, once it has been read. */
    public Preprocessor preprocessor() {
        return this.preprocessor;
    }

    // Whitespace and both comment forms carry no meaning, so they are consumed between tokens.
    private void skipTrivia() {
        while (this.index < this.source.length()) {
            final char c = this.peek();
            if (c == '\n') {
                this.advance();
                this.lineStarted = false;
            } else if (c == '#' && !this.lineStarted) {
                this.directiveLine();
            } else if (c == ' ' || c == '\t' || c == '\r' || c == '\f' || c == 0x0B) {
                this.advance();
            } else if (c == '/' && this.peek(1) == '/') {
                while (this.index < this.source.length() && this.peek() != '\n') {
                    this.advance();
                }
            } else if (c == '/' && this.peek(1) == '*') {
                this.skipBlockComment();
            } else {
                return;
            }
        }
    }

    /*
     * A directive, read to the end of its line, and then every line it leaves out passed over whole, but for the
     * directives among them, which still open and close conditions.
     */
    private void directiveLine() {
        this.readDirective();
        while (!this.preprocessor.active() && this.index < this.source.length()) {
            // Past the end of the line just read, onto the next, whose first character decides what it is.
            this.advance();
            int ahead = this.index;
            while (ahead < this.source.length() && (this.source.text().charAt(ahead) == ' '
                    || this.source.text().charAt(ahead) == '\t')) {
                ahead++;
            }
            if (ahead < this.source.length() && this.source.text().charAt(ahead) == '#') {
                while (this.index < ahead) {
                    this.advance();
                }
                this.readDirective();
            } else {
                this.preprocessor.leftOut(this.line);
                this.restOfLine();
            }
        }
    }

    /* The directive that starts here, read where it starts. */
    private void readDirective() {
        final int startLine = this.line;
        final int startColumn = this.column;
        this.preprocessor.directive(this.restOfLine(), startLine, startColumn);
    }

    /* The text from here to the end of the line, the line break left for whoever comes next. */
    private String restOfLine() {
        final int start = this.index;
        while (this.index < this.source.length() && this.peek() != '\n') {
            this.advance();
        }
        return this.source.text().substring(start, this.index);
    }

    private void skipBlockComment() {
        final int startLine = this.line;
        final int startColumn = this.column;
        this.advance();
        this.advance();
        while (true) {
            if (this.index >= this.source.length()) {
                this.diagnostics.error(startLine, startColumn, SigmaError.UNTERMINATED_COMMENT);
                return;
            }
            if (this.peek() == '*' && this.peek(1) == '/') {
                this.advance();
                this.advance();
                return;
            }
            this.advance();
        }
    }

    private Token scanToken() {
        final int startIndex = this.index;
        final int startLine = this.line;
        final int startColumn = this.column;
        final char c = this.peek();

        if (Character.isLetter(c) || c == '_') {
            return this.scanWord(startIndex, startLine, startColumn);
        }
        // A real may start at its point, as .5 does in C#.
        if (Character.isDigit(c) || (c == '.' && Character.isDigit(this.peek(1)))) {
            return this.scanNumber(startIndex, startLine, startColumn);
        }
        if (c == '"') {
            return this.scanString(startLine, startColumn);
        }
        if (c == '$' && this.peek(1) == '"') {
            return this.scanInterpolated(startLine, startColumn);
        }
        if (c == '\'') {
            return this.scanChar(startLine, startColumn);
        }
        return this.scanOperator(startIndex, startLine, startColumn);
    }

    /** A piece of code inside an interpolated string, with where it starts so its own mistakes point home. */
    public record Hole(String code, int line, int column) {
    }

    /**
     * A string with holes in it: {@code $"Total: {count} items"}.
     *
     * <p>The token's value is the list of its parts in order, each a String of plain text (escapes
     * already read) or a {@link Hole} holding the code between one pair of braces, untouched, for the
     * parser to read as an expression. Two braces in a row are one brace of text.
     */
    private Token scanInterpolated(final int startLine, final int startColumn) {
        final int startIndex = this.index;
        this.advance();
        this.advance();
        final List<Object> parts = new ArrayList<>();
        final StringBuilder text = new StringBuilder();
        while (true) {
            if (this.index >= this.source.length() || this.peek() == '\n') {
                this.diagnostics.error(startLine, startColumn, SigmaError.UNTERMINATED_STRING);
                break;
            }
            final char c = this.peek();
            if (c == '"') {
                this.advance();
                break;
            }
            if (c == '{' && this.peek(1) == '{') {
                this.advance();
                this.advance();
                text.append('{');
                continue;
            }
            if (c == '}' && this.peek(1) == '}') {
                this.advance();
                this.advance();
                text.append('}');
                continue;
            }
            if (c == '{') {
                if (!text.isEmpty()) {
                    parts.add(text.toString());
                    text.setLength(0);
                }
                if (!this.scanHole(parts)) {
                    // The hole ran to the end of the line and was reported, so the string is not reported as well.
                    break;
                }
                continue;
            }
            text.append(c == '\\' ? this.scanEscape() : this.advance());
        }
        if (!text.isEmpty() || parts.isEmpty()) {
            parts.add(text.toString());
        }
        return new Token(TokenKind.INTERPOLATED_STRING, this.source.text().substring(startIndex, this.index),
                List.copyOf(parts), startLine, startColumn);
    }

    /**
     * Reads one hole, from the opening brace to the matching closing one, and adds it to {@code parts}.
     * An unclosed hole is reported here; the result says whether it was closed.
     */
    private boolean scanHole(final List<Object> parts) {
        this.advance();
        final int holeLine = this.line;
        final int holeColumn = this.column;
        final StringBuilder code = new StringBuilder();
        int depth = 1;
        while (this.index < this.source.length() && this.peek() != '\n') {
            final char inner = this.peek();
            if (inner == '{') {
                depth++;
            } else if (inner == '}') {
                depth--;
                if (depth == 0) {
                    break;
                }
            }
            code.append(this.advance());
        }
        final boolean closed = this.index < this.source.length() && this.peek() == '}';
        if (closed) {
            this.advance();
        } else {
            this.diagnostics.error(holeLine, holeColumn, SigmaError.UNTERMINATED_STRING);
        }
        parts.add(new Hole(code.toString(), holeLine, holeColumn));
        return closed;
    }

    private Token scanWord(final int startIndex, final int startLine, final int startColumn) {
        while (this.index < this.source.length()
                && (Character.isLetterOrDigit(this.peek()) || this.peek() == '_')) {
            this.advance();
        }
        final String text = this.textFrom(startIndex);
        final TokenKind keyword = TokenKind.keyword(text);
        if (keyword == null) {
            return Token.of(TokenKind.IDENTIFIER, text, startLine, startColumn);
        }
        if (keyword == TokenKind.TRUE || keyword == TokenKind.FALSE) {
            return new Token(keyword, text, keyword == TokenKind.TRUE, startLine, startColumn);
        }
        return Token.of(keyword, text, startLine, startColumn);
    }

    /*
     * A number is digits, an optional fractional part, and an optional suffix naming its type. A
     * letter that is not one of the suffixes is part of the mistake, so it is consumed with it and
     * the whole run is quoted back: "12abc" reads better than "12" followed by a stray name.
     */
    private Token scanNumber(final int startIndex, final int startLine, final int startColumn) {
        while (this.index < this.source.length() && Character.isDigit(this.peek())) {
            this.advance();
        }
        boolean real = false;
        if (this.peek() == '.' && Character.isDigit(this.peek(1))) {
            real = true;
            this.advance();
            while (this.index < this.source.length() && Character.isDigit(this.peek())) {
                this.advance();
            }
        }
        final String digits = this.textFrom(startIndex);
        char suffix = '\0';
        if (Character.isLetter(this.peek())) {
            suffix = this.peek();
            this.advance();
        }
        /*
         * A point after a whole number is a mistake only when no member's name follows it: 10.ToString() is a
         * call on the number, and 1.2.3 is a number nobody can read.
         */
        if (Character.isLetterOrDigit(this.peek()) || this.peek() == '_'
                || (this.peek() == '.' && !real && !startsAName(this.peek(1)))) {
            while (this.index < this.source.length()
                    && (Character.isLetterOrDigit(this.peek()) || this.peek() == '_'
                    || (this.peek() == '.' && !startsAName(this.peek(1))))) {
                this.advance();
            }
            this.diagnostics.error(startLine, startColumn, SigmaError.MALFORMED_NUMBER, this.textFrom(startIndex));
            return new Token(TokenKind.INT_LITERAL, this.textFrom(startIndex), 0, startLine, startColumn);
        }
        final String text = this.textFrom(startIndex);
        return this.numberToken(digits, suffix, real, text, startLine, startColumn);
    }

    private Token numberToken(final String digits, final char suffix, final boolean real,
                              final String text, final int startLine, final int startColumn) {
        try {
            switch (suffix) {
                case 'f':
                case 'F':
                    return new Token(TokenKind.FLOAT_LITERAL, text, floatOf(digits), startLine, startColumn);
                case 'd':
                case 'D':
                    return new Token(TokenKind.DOUBLE_LITERAL, text, doubleOf(digits), startLine, startColumn);
                case 'l':
                case 'L':
                    if (real) {
                        break;
                    }
                    if (digits.equals(LONG_MIN_DIGITS)) {
                        return new Token(TokenKind.LONG_LITERAL, text, Long.MIN_VALUE, startLine, startColumn);
                    }
                    return new Token(TokenKind.LONG_LITERAL, text, Long.parseLong(digits), startLine, startColumn);
                case '\0':
                    if (!real && digits.equals(INT_MIN_DIGITS)) {
                        return new Token(TokenKind.INT_LITERAL, text, Integer.MIN_VALUE, startLine, startColumn);
                    }
                    return real
                            ? new Token(TokenKind.DOUBLE_LITERAL, text, doubleOf(digits), startLine, startColumn)
                            : new Token(TokenKind.INT_LITERAL, text, Integer.parseInt(digits),
                                    startLine, startColumn);
                default:
                    break;
            }
        } catch (final NumberFormatException tooBigForItsType) {
            // Reported below, the same way as a suffix that does not fit the number.
        }
        this.diagnostics.error(startLine, startColumn, SigmaError.MALFORMED_NUMBER, text);
        return new Token(TokenKind.INT_LITERAL, text, 0, startLine, startColumn);
    }

    /* Java turns a literal too large or too small for a float into infinity or zero; here that is a mistake. */
    private static float floatOf(final String digits) {
        final float value = Float.parseFloat(digits);
        if (Float.isInfinite(value) || value == 0f && hasNonZeroDigit(digits)) {
            throw new NumberFormatException(digits);
        }
        return value;
    }

    private static double doubleOf(final String digits) {
        final double value = Double.parseDouble(digits);
        if (Double.isInfinite(value) || value == 0d && hasNonZeroDigit(digits)) {
            throw new NumberFormatException(digits);
        }
        return value;
    }

    private static boolean hasNonZeroDigit(final String digits) {
        return digits.chars().anyMatch(digit -> digit >= '1' && digit <= '9');
    }

    private static boolean startsAName(final char c) {
        return Character.isLetter(c) || c == '_';
    }

    /*
     * A string's token holds what it means and, as its text, what was typed, quotes and escapes and all, which is
     * what a mistake quotes back and what an editor paints the width of.
     */
    private Token scanString(final int startLine, final int startColumn) {
        final int startIndex = this.index;
        this.advance();
        final StringBuilder value = new StringBuilder();
        while (true) {
            if (this.index >= this.source.length() || this.peek() == '\n') {
                this.diagnostics.error(startLine, startColumn, SigmaError.UNTERMINATED_STRING);
                break;
            }
            final char c = this.peek();
            if (c == '"') {
                this.advance();
                break;
            }
            value.append(c == '\\' ? this.scanEscape() : this.advance());
        }
        return new Token(TokenKind.STRING_LITERAL, this.textFrom(startIndex), value.toString(), startLine,
                startColumn);
    }

    private Token scanChar(final int startLine, final int startColumn) {
        final int startIndex = this.index;
        this.advance();
        if (this.index >= this.source.length() || this.peek() == '\'' || this.peek() == '\n') {
            if (this.peek() == '\'') {
                this.advance();
            }
            this.diagnostics.error(startLine, startColumn, SigmaError.INVALID_CHARACTER_LITERAL);
            return new Token(TokenKind.CHAR_LITERAL, this.textFrom(startIndex), '\0', startLine, startColumn);
        }
        final char value = this.peek() == '\\' ? this.scanEscape() : this.advance();
        if (this.peek() == '\'') {
            this.advance();
        } else {
            while (this.index < this.source.length() && this.peek() != '\'' && this.peek() != '\n') {
                this.advance();
            }
            if (this.peek() == '\'') {
                this.advance();
            }
            this.diagnostics.error(startLine, startColumn, SigmaError.INVALID_CHARACTER_LITERAL);
        }
        return new Token(TokenKind.CHAR_LITERAL, this.textFrom(startIndex), value, startLine, startColumn);
    }

    private char scanEscape() {
        final int escapeLine = this.line;
        final int escapeColumn = this.column;
        this.advance();
        // A backslash that is the last thing in the file escapes nothing; reading on would step past the text's end.
        if (this.index >= this.source.length()) {
            this.diagnostics.error(escapeLine, escapeColumn, SigmaError.UNTERMINATED_STRING);
            return '\0';
        }
        // Nor does one at the end of a line: the line still ends there, and the string with it, unclosed.
        if (this.peek() == '\n') {
            return '\0';
        }
        final char c = this.advance();
        switch (c) {
            case 'n':
                return '\n';
            case 't':
                return '\t';
            case 'r':
                return '\r';
            case '0':
                return '\0';
            case '\\':
                return '\\';
            case '"':
                return '"';
            case '\'':
                return '\'';
            default:
                this.diagnostics.error(escapeLine, escapeColumn, SigmaError.UNKNOWN_ESCAPE, String.valueOf(c));
                return c;
        }
    }

    // Longest match wins, so "<<=" is never read as "<<" followed by "=".
    private Token scanOperator(final int startIndex, final int startLine, final int startColumn) {
        final char c = this.advance();
        final TokenKind kind;
        switch (c) {
            case '{': kind = TokenKind.LEFT_BRACE; break;
            case '}': kind = TokenKind.RIGHT_BRACE; break;
            case '(': kind = TokenKind.LEFT_PAREN; break;
            case ')': kind = TokenKind.RIGHT_PAREN; break;
            case '[': kind = TokenKind.LEFT_BRACKET; break;
            case ']': kind = TokenKind.RIGHT_BRACKET; break;
            case ';': kind = TokenKind.SEMICOLON; break;
            case ',': kind = TokenKind.COMMA; break;
            case '.': kind = TokenKind.DOT; break;
            case ':': kind = TokenKind.COLON; break;
            case '?': kind = TokenKind.QUESTION; break;
            case '~': kind = TokenKind.TILDE; break;
            case '=': kind = this.take('=') ? TokenKind.EQUAL
                    : this.take('>') ? TokenKind.ARROW : TokenKind.ASSIGN; break;
            case '!': kind = this.take('=') ? TokenKind.NOT_EQUAL : TokenKind.NOT; break;
            case '+': kind = this.take('+') ? TokenKind.PLUS_PLUS
                    : this.take('=') ? TokenKind.PLUS_ASSIGN : TokenKind.PLUS; break;
            case '-': kind = this.take('-') ? TokenKind.MINUS_MINUS
                    : this.take('=') ? TokenKind.MINUS_ASSIGN
                    : this.take('>') ? TokenKind.POINTER_ARROW : TokenKind.MINUS; break;
            case '*': kind = this.take('=') ? TokenKind.STAR_ASSIGN : TokenKind.STAR; break;
            case '/': kind = this.take('=') ? TokenKind.SLASH_ASSIGN : TokenKind.SLASH; break;
            case '%': kind = this.take('=') ? TokenKind.PERCENT_ASSIGN : TokenKind.PERCENT; break;
            case '^': kind = this.take('=') ? TokenKind.CARET_ASSIGN : TokenKind.CARET; break;
            case '&': kind = this.take('&') ? TokenKind.AND_AND
                    : this.take('=') ? TokenKind.AMPERSAND_ASSIGN : TokenKind.AMPERSAND; break;
            case '|': kind = this.take('|') ? TokenKind.OR_OR
                    : this.take('=') ? TokenKind.PIPE_ASSIGN : TokenKind.PIPE; break;
            case '<': kind = this.shiftOrCompare(true); break;
            case '>': kind = this.shiftOrCompare(false); break;
            default:
                this.diagnostics.error(startLine, startColumn, SigmaError.UNEXPECTED_CHARACTER,
                        String.valueOf(c));
                return null;
        }
        return Token.of(kind, this.textFrom(startIndex), startLine, startColumn);
    }

    private TokenKind shiftOrCompare(final boolean left) {
        final char same = left ? '<' : '>';
        if (this.take(same)) {
            if (this.take('=')) {
                return left ? TokenKind.SHIFT_LEFT_ASSIGN : TokenKind.SHIFT_RIGHT_ASSIGN;
            }
            return left ? TokenKind.SHIFT_LEFT : TokenKind.SHIFT_RIGHT;
        }
        if (this.take('=')) {
            return left ? TokenKind.LESS_EQUAL : TokenKind.GREATER_EQUAL;
        }
        return left ? TokenKind.LESS : TokenKind.GREATER;
    }

    private boolean take(final char expected) {
        if (this.peek() == expected) {
            this.advance();
            return true;
        }
        return false;
    }

    private String textFrom(final int startIndex) {
        return this.source.text().substring(startIndex, this.index);
    }

    private char peek() {
        return this.source.charAt(this.index);
    }

    private char peek(final int ahead) {
        return this.source.charAt(this.index + ahead);
    }

    private char advance() {
        final char c = this.source.charAt(this.index);
        this.index++;
        if (c == '\n') {
            this.line++;
            this.column = 1;
        } else {
            this.column++;
        }
        return c;
    }
}
