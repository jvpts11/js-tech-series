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
import dev.jstech.computers.sigma.ast.TypeRef;
import dev.jstech.computers.sigma.lex.Token;
import dev.jstech.computers.sigma.lex.TokenKind;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reads how a type is written down.
 *
 * <p>The smallest piece of the grammar and the one every other piece asks for: a declaration names the
 * type of what it declares, a statement names the type of a local, an expression names a type in a cast
 * and after {@code is}. It asks nothing back.
 *
 * <p>It also looks ahead without reading, which is what tells a declaration from an expression and a
 * conversion from a value in brackets. Looking never reports, so a guess that turns out wrong costs
 * nothing and leaves nothing behind.
 */
final class TypeParser {

    private final TokenCursor cursor;
    private final DiagnosticBag diagnostics;

    private static final Set<TokenKind> BUILT_IN_TYPES = EnumSet.of(
            TokenKind.INT, TokenKind.LONG, TokenKind.FLOAT, TokenKind.DOUBLE,
            TokenKind.BOOL, TokenKind.STRING, TokenKind.CHAR, TokenKind.OBJECT);

    TypeParser(final TokenCursor cursor, final DiagnosticBag diagnostics) {
        this.cursor = cursor;
        this.diagnostics = diagnostics;
    }

    /** Whether that word is one of the types the language has of its own. */
    boolean isBuiltIn(final TokenKind kind) {
        return BUILT_IN_TYPES.contains(kind);
    }

    boolean isTypeStart(final TokenKind kind) {
        return kind == TokenKind.IDENTIFIER || kind == TokenKind.VAR || BUILT_IN_TYPES.contains(kind);
    }

    TypeRef parseReturnType() {
        if (this.cursor.check(TokenKind.VOID)) {
            final Token word = this.cursor.advance();
            return TypeRef.named("void", word.line(), word.column());
        }
        return this.parseTypeRef();
    }

    TypeRef parseTypeRef() {
        final Token start = this.cursor.peek();
        if (!this.isTypeStart(start.kind())) {
            this.diagnostics.error(start.line(), start.column(), SigmaError.EXPECTED_TYPE, start.describe());
            return null;
        }
        this.cursor.advance();
        // A type may be named with its namespace in front: Tools.Counter is one name with dots in it.
        final StringBuilder name = new StringBuilder(start.text());
        while (start.kind() == TokenKind.IDENTIFIER && this.cursor.check(TokenKind.DOT)
                && this.cursor.kindAhead(1) == TokenKind.IDENTIFIER) {
            this.cursor.advance();
            name.append('.').append(this.cursor.advance().text());
        }
        final List<TypeRef> arguments = new ArrayList<>();
        if (this.cursor.check(TokenKind.LESS)) {
            this.cursor.advance();
            do {
                final TypeRef argument = this.parseTypeRef();
                if (argument == null) {
                    break;
                }
                arguments.add(argument);
            } while (this.cursor.match(TokenKind.COMMA));
            this.closeTypeArguments();
        }
        int arrayRank = 0;
        while (this.cursor.check(TokenKind.LEFT_BRACKET) && this.cursor.kindAhead(1) == TokenKind.RIGHT_BRACKET) {
            this.cursor.advance();
            this.cursor.advance();
            arrayRank++;
        }
        return new TypeRef(name.toString(), arguments, arrayRank, start.line(), start.column());
    }

    /*
     * Looks past a type without reporting anything, and answers where it ends, or -1 if what is
     * there is not a type at all. Used only to tell a declaration from an expression.
     */
    int scanType(final int from) {
        int at = from;
        if (!this.isTypeStart(this.cursor.kindAt(at))) {
            return -1;
        }
        final boolean named = this.cursor.kindAt(at) == TokenKind.IDENTIFIER;
        at++;
        while (named && this.cursor.kindAt(at) == TokenKind.DOT
                && this.cursor.kindAt(at + 1) == TokenKind.IDENTIFIER) {
            at += 2;
        }
        if (this.cursor.kindAt(at) == TokenKind.LESS) {
            int depth = 1;
            at++;
            while (depth > 0) {
                final TokenKind kind = this.cursor.kindAt(at);
                if (kind == TokenKind.END_OF_FILE) {
                    return -1;
                }
                if (kind == TokenKind.LESS) {
                    depth++;
                } else if (kind == TokenKind.GREATER) {
                    depth--;
                } else if (kind == TokenKind.SHIFT_RIGHT) {
                    depth -= 2;
                } else if (kind != TokenKind.COMMA && kind != TokenKind.LEFT_BRACKET
                        && kind != TokenKind.RIGHT_BRACKET && !this.isTypeStart(kind)) {
                    return -1;
                }
                at++;
                if (depth < 0) {
                    return -1;
                }
            }
        }
        while (this.cursor.kindAt(at) == TokenKind.LEFT_BRACKET
                && this.cursor.kindAt(at + 1) == TokenKind.RIGHT_BRACKET) {
            at += 2;
        }
        return at;
    }

    /*
     * "Map<string, List<int>>" ends on one token holding two closing angles, so the first one is
     * taken here and the token is left behind as the second.
     */
    private void closeTypeArguments() {
        if (this.cursor.match(TokenKind.GREATER)) {
            return;
        }
        if (this.cursor.check(TokenKind.SHIFT_RIGHT)) {
            final Token shift = this.cursor.peek();
            this.cursor.replaceHere(Token.of(TokenKind.GREATER, ">", shift.line(), shift.column() + 1));
            return;
        }
        this.cursor.expect(TokenKind.GREATER);
    }
}
