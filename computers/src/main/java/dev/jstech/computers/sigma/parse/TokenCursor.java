/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.parse;

import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.lex.Token;
import dev.jstech.computers.sigma.lex.TokenKind;
import java.util.ArrayList;
import java.util.List;

/**
 * Where the reading has got to in the tokens, and the small ways of moving.
 *
 * <p>Everything that reads the grammar reads it through one of these, and there is one of these per
 * file being read. That is what lets the grammar be written in pieces: a declaration, a statement and
 * an expression are separate things to read, but they are reading the same file at the same place, and
 * a piece that moved the cursor has moved it for whoever it hands back to.
 *
 * <p>Nothing here throws and nothing here skips. Asking past the end gives back the end, over and over,
 * so a file that stops in the middle of something is read to a stop instead of falling over.
 */
final class TokenCursor {

    private final List<Token> tokens;
    private final DiagnosticBag diagnostics;
    private int position;
    /** How many constructs the one being read sits inside. */
    private int depth;
    /**
     * Whether the file was refused as too deep. Every construct still open then finds the end where its closing
     * token should be, and saying so for each of them would bury the one thing worth saying under hundreds of lines.
     */
    private boolean refused;

    /**
     * How deep one construct may sit inside others before a file is refused as too deep to read.
     *
     * <p>Far past anything written by hand, and low enough that the deepest reading of all, brackets inside
     * brackets, which passes through every level of operator precedence each time, stays well inside a thread's
     * stack; the stages after this one walk the tree it builds, which is never deeper.
     */
    static final int MAX_DEPTH = 128;

    TokenCursor(final List<Token> tokens, final DiagnosticBag diagnostics) {
        this(tokens, diagnostics, 0);
    }

    /** One that starts that deep already, for a piece of a file read on its own inside another construct. */
    TokenCursor(final List<Token> tokens, final DiagnosticBag diagnostics, final int depth) {
        this.tokens = new ArrayList<>(tokens);
        this.diagnostics = diagnostics;
        this.depth = depth;
    }

    /** How deep the reading is, for a piece read on its own to start from. */
    int depth() {
        return this.depth;
    }

    /**
     * Goes one construct deeper, or refuses to.
     *
     * <p>The grammar is read by methods calling each other as deep as the file nests, so a file of a few thousand
     * opening brackets took the reading past the end of its stack, which is a crash rather than a mistake to point
     * at. Past the limit the file is said to be too deep, once, and the reading jumps to the end, where every
     * construct being read stops; the caller hands back nothing for what it was about to read.
     */
    boolean descend() {
        if (this.depth >= MAX_DEPTH) {
            if (this.position < this.tokens.size() - 1) {
                final Token here = this.peek();
                this.diagnostics.error(here.line(), here.column(), SigmaError.NESTING_TOO_DEEP, MAX_DEPTH);
                this.position = this.tokens.size() - 1;
                this.refused = true;
            }
            return false;
        }
        this.depth++;
        return true;
    }

    /** Comes back out of a construct {@link #descend()} went into. */
    void ascend() {
        this.depth--;
    }

    /** Where the reading has got to, for the scans that look ahead by index. */
    int at() {
        return this.position;
    }

    boolean atEnd() {
        return this.peek().is(TokenKind.END_OF_FILE);
    }

    Token peek() {
        return this.tokens.get(Math.min(this.position, this.tokens.size() - 1));
    }

    /** The kind of the token at that place in the file, or the end when the place is past it. */
    TokenKind kindAt(final int index) {
        return this.tokens.get(Math.min(Math.max(index, 0), this.tokens.size() - 1)).kind();
    }

    /** The kind of the token that many places past the one being read. */
    TokenKind kindAhead(final int ahead) {
        return this.kindAt(this.position + ahead);
    }

    boolean check(final TokenKind kind) {
        return this.peek().is(kind);
    }

    boolean match(final TokenKind kind) {
        if (this.check(kind)) {
            this.advance();
            return true;
        }
        return false;
    }

    TokenKind matchAny(final TokenKind[] kinds) {
        for (final TokenKind kind : kinds) {
            if (this.check(kind)) {
                this.advance();
                return kind;
            }
        }
        return null;
    }

    Token advance() {
        final Token token = this.peek();
        if (this.position < this.tokens.size() - 1) {
            this.position++;
        }
        return token;
    }

    /*
     * Reports and does not consume, so the caller decides how to recover rather than losing a token
     * that might be the start of the next good construct.
     */
    boolean expect(final TokenKind kind) {
        if (this.check(kind)) {
            this.advance();
            return true;
        }
        final Token found = this.peek();
        if (!this.refused) {
            this.diagnostics.error(found.line(), found.column(),
                    SigmaError.EXPECTED_TOKEN, kind.describe(), found.describe());
        }
        return false;
    }

    String expectIdentifier() {
        if (this.check(TokenKind.IDENTIFIER)) {
            return this.advance().text();
        }
        final Token found = this.peek();
        if (!this.refused) {
            this.diagnostics.error(found.line(), found.column(),
                    SigmaError.EXPECTED_TOKEN, TokenKind.IDENTIFIER.describe(), found.describe());
        }
        return found.text();
    }

    /**
     * Puts a different token where the reading is, without moving.
     *
     * <p>One token can hold two things the grammar wants separately, and the only one is the pair of
     * closing angles that ends two type arguments at once. The first is taken and the second is left
     * here in its place, so the reading goes on as though they had been written apart.
     */
    void replaceHere(final Token token) {
        this.tokens.set(this.position, token);
    }
}
