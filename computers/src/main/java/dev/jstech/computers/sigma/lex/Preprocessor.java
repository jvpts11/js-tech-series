/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lex;

import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.SigmaError;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * C#'s directives, in Σ and Σ#: {@code #if}, {@code #elif}, {@code #else}, {@code #endif}, {@code #define},
 * {@code #undef}, {@code #error} and {@code #warning} (computers/docs/design/sigma.md).
 *
 * <p>A directive is a line of its own, starting with {@code #}. A condition is made of symbols, {@code true} and
 * {@code false}, {@code !}, {@code &&}, {@code ||}, {@code ==}, {@code !=} and brackets; a symbol is true when it is
 * defined. The compiler defines the instruction set the build is for, the machine's era and {@code SIGMA_3}, and a
 * project defines its own; {@code #define} and {@code #undef} change them for the rest of the file. The lines a
 * condition leaves out are passed over whole, as if they were not there, and directives inside them still count, so
 * an {@code #if} left out still has its {@code #endif}.
 */
public final class Preprocessor {

    private final DiagnosticBag diagnostics;
    private final Set<String> symbols;
    private final Deque<Branch> open = new ArrayDeque<>();
    /** Every directive line, which an editor paints as a word of the language and a version check finds. */
    private final List<Directive> directives = new ArrayList<>();
    /** Every line a condition left out, which an editor paints dimmed. */
    private final List<Integer> leftOut = new ArrayList<>();

    /** One directive, where it starts and how long its line is. */
    public record Directive(String word, int line, int column, int length) {
    }

    /**
     * One {@code #if} still open: whether any of its branches has been taken yet, whether the lines now are, whether
     * the lines around it are at all, and whether its {@code #else} has been met.
     */
    private record Branch(boolean taken, boolean active, boolean outerActive, boolean sawElse, int line, int column) {
    }

    public Preprocessor(final DiagnosticBag diagnostics, final Set<String> defined) {
        this.diagnostics = diagnostics;
        this.symbols = new LinkedHashSet<>(defined);
    }

    /** Whether the lines here are read: none of the open conditions left them out. */
    public boolean active() {
        return this.open.isEmpty() || this.open.peek().active();
    }

    /** Takes note that a line was left out. */
    void leftOut(final int line) {
        this.leftOut.add(line);
    }

    /** Every directive line of the file. */
    public List<Directive> directives() {
        return List.copyOf(this.directives);
    }

    /** Every line a condition left out. */
    public List<Integer> leftOutLines() {
        return List.copyOf(this.leftOut);
    }

    /** Reports an {@code #if} the file never closed, once it has been read to its end. */
    void finish() {
        for (final Branch branch : this.open) {
            this.diagnostics.error(branch.line(), branch.column(), SigmaError.UNCLOSED_DIRECTIVE);
        }
        this.open.clear();
    }

    /**
     * Reads one directive line, the {@code #} first, at that line and column. A directive in lines left out still
     * counts where it opens or closes a condition, and nothing else of it does.
     */
    void directive(final String text, final int line, final int column) {
        final String body = stripComment(text.substring(1)).trim();
        final int space = indexOfSpace(body);
        final String word = (space < 0 ? body : body.substring(0, space)).toLowerCase(Locale.ROOT);
        final String rest = space < 0 ? "" : body.substring(space + 1).trim();
        this.directives.add(new Directive(word, line, column, text.length()));
        final boolean reading = this.active();
        switch (word) {
            case "if" -> {
                final boolean holds = reading && this.condition(rest, line, column, word);
                this.open.push(new Branch(holds, holds, reading, false, line, column));
            }
            case "elif", "else" -> this.otherBranch(word, rest, line, column);
            case "endif" -> {
                if (this.open.isEmpty()) {
                    this.diagnostics.error(line, column, SigmaError.UNOPENED_DIRECTIVE, word);
                } else {
                    this.open.pop();
                }
            }
            case "define", "undef" -> {
                if (!reading) {
                    return;
                }
                if (!isSymbol(rest)) {
                    this.diagnostics.error(line, column, SigmaError.BAD_DIRECTIVE, body);
                } else if (word.equals("define")) {
                    this.symbols.add(rest);
                } else {
                    this.symbols.remove(rest);
                }
            }
            case "error" -> {
                if (reading) {
                    this.diagnostics.error(line, column, SigmaError.DIRECTIVE_SAYS, rest);
                }
            }
            case "warning" -> {
                if (reading) {
                    this.diagnostics.warning(line, column, SigmaError.DIRECTIVE_SAYS, rest);
                }
            }
            default -> {
                if (reading) {
                    this.diagnostics.error(line, column, SigmaError.BAD_DIRECTIVE, body);
                }
            }
        }
    }

    /* An #elif or an #else: taken when no branch before it was and, for #elif, its own condition holds. */
    private void otherBranch(final String word, final String rest, final int line, final int column) {
        if (this.open.isEmpty()) {
            this.diagnostics.error(line, column, SigmaError.UNOPENED_DIRECTIVE, word);
            return;
        }
        final Branch before = this.open.pop();
        if (before.sawElse()) {
            this.diagnostics.error(line, column, SigmaError.ELSE_TWICE, word);
        }
        final boolean others = before.outerActive() && !before.taken();
        final boolean holds = others && (word.equals("else") || this.condition(rest, line, column, word));
        this.open.push(new Branch(before.taken() || holds, holds, before.outerActive(),
                before.sawElse() || word.equals("else"), before.line(), before.column()));
    }

    private boolean condition(final String text, final int line, final int column, final String word) {
        final Condition reading = new Condition(text);
        final Boolean value = reading.read();
        if (value == null) {
            this.diagnostics.error(line, column, SigmaError.BAD_CONDITION, text, word);
            return false;
        }
        return value;
    }

    private static boolean isSymbol(final String text) {
        if (text.isEmpty() || !(Character.isLetter(text.charAt(0)) || text.charAt(0) == '_')) {
            return false;
        }
        for (int i = 1; i < text.length(); i++) {
            if (!Character.isLetterOrDigit(text.charAt(i)) && text.charAt(i) != '_') {
                return false;
            }
        }
        return true;
    }

    private static int indexOfSpace(final String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /* A directive may end in a line comment, as C#'s may. */
    private static String stripComment(final String text) {
        final int comment = text.indexOf("//");
        return comment < 0 ? text : text.substring(0, comment);
    }

    /** A condition read by descent: or of ands of comparisons of what may be negated, the tightest last. */
    private final class Condition {

        private final String text;
        private int at;
        private boolean broken;

        Condition(final String text) {
            this.text = text;
        }

        /** What the condition comes to, or null when it cannot be read. */
        Boolean read() {
            final boolean value = this.or();
            this.skipSpaces();
            return this.broken || this.text.isBlank() || this.at < this.text.length() ? null : value;
        }

        private boolean or() {
            boolean value = this.and();
            while (this.take("||")) {
                value |= this.and();
            }
            return value;
        }

        private boolean and() {
            boolean value = this.equality();
            while (this.take("&&")) {
                value &= this.equality();
            }
            return value;
        }

        private boolean equality() {
            boolean value = this.unary();
            while (true) {
                if (this.take("==")) {
                    value = value == this.unary();
                } else if (this.take("!=")) {
                    value = value != this.unary();
                } else {
                    return value;
                }
            }
        }

        private boolean unary() {
            if (this.take("!")) {
                return !this.unary();
            }
            if (this.take("(")) {
                final boolean value = this.or();
                if (!this.take(")")) {
                    this.broken = true;
                }
                return value;
            }
            this.skipSpaces();
            final int start = this.at;
            while (this.at < this.text.length()
                    && (Character.isLetterOrDigit(this.text.charAt(this.at)) || this.text.charAt(this.at) == '_')) {
                this.at++;
            }
            final String name = this.text.substring(start, this.at);
            if (!isSymbol(name)) {
                this.broken = true;
                return false;
            }
            return switch (name) {
                case "true" -> true;
                case "false" -> false;
                default -> Preprocessor.this.symbols.contains(name);
            };
        }

        private boolean take(final String symbol) {
            this.skipSpaces();
            if (this.text.startsWith(symbol, this.at) && !(symbol.equals("!") && this.text.startsWith("!=", this.at))) {
                this.at += symbol.length();
                return true;
            }
            return false;
        }

        private void skipSpaces() {
            while (this.at < this.text.length() && Character.isWhitespace(this.text.charAt(this.at))) {
                this.at++;
            }
        }
    }
}
