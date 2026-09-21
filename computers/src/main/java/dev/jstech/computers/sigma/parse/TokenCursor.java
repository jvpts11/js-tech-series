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

    TokenCursor(final List<Token> tokens, final DiagnosticBag diagnostics) {
        this.tokens = new ArrayList<>(tokens);
        this.diagnostics = diagnostics;
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
        this.diagnostics.error(found.line(), found.column(),
                SigmaError.EXPECTED_TOKEN, kind.describe(), found.describe());
        return false;
    }

    String expectIdentifier() {
        if (this.check(TokenKind.IDENTIFIER)) {
            return this.advance().text();
        }
        final Token found = this.peek();
        this.diagnostics.error(found.line(), found.column(),
                SigmaError.EXPECTED_TOKEN, TokenKind.IDENTIFIER.describe(), found.describe());
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
