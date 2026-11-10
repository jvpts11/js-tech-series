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
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.EnumSet;
import java.util.Set;

/**
 * The words version 3 of the language brought that the versions before it kept back for it, so no program takes them
 * as names: C#'s try, catch, finally, throw, goto and const, and INTERCAL's comefrom, abstainfrom and reinstate.
 *
 * <p>A program that writes one as a name is told so at the word. A catch or a finally with no try before it is
 * reported at the word and passed over whole, blocks and all, so the rest of the method is still read.
 */
@TextHolder
final class LanguageGaps {

    private final TokenCursor cursor;
    private final DiagnosticBag diagnostics;

    private static final Set<TokenKind> LATER = EnumSet.of(TokenKind.TRY, TokenKind.CATCH, TokenKind.FINALLY,
            TokenKind.THROW, TokenKind.GOTO, TokenKind.CONST, TokenKind.COMEFROM, TokenKind.ABSTAINFROM,
            TokenKind.REINSTATE);
    private static final TextKey NAME_INSTEAD = TextKey.of("jsc.sigma.language_gaps.name_instead",
            "give the name another word");

    LanguageGaps(final TokenCursor cursor, final DiagnosticBag diagnostics) {
        this.cursor = cursor;
        this.diagnostics = diagnostics;
    }

    /** Whether the word is reserved for the later version, and so no name. */
    static boolean reserved(final TokenKind kind) {
        return LATER.contains(kind);
    }

    /** Reports a reserved word written where a name goes. */
    static void refuseAsName(final DiagnosticBag diagnostics, final Token word) {
        refuse(diagnostics, word, NAME_INSTEAD);
    }

    /**
     * Whether the statement being read begins with a catch or a finally that no try stands before, which it then
     * reports and passes over whole, blocks and all.
     */
    boolean passesStatement(final StatementParser statements) {
        final Token word = this.cursor.peek();
        if (!word.is(TokenKind.CATCH) && !word.is(TokenKind.FINALLY)) {
            return false;
        }
        this.diagnostics.error(word.line(), word.column(), SigmaError.EXPECTED_TOKEN, TokenKind.TRY.describe(),
                word.describe());
        while (this.cursor.check(TokenKind.CATCH) || this.cursor.check(TokenKind.FINALLY)) {
            this.cursor.advance();
            if (this.cursor.check(TokenKind.LEFT_PAREN)) {
                this.skipPast(TokenKind.RIGHT_PAREN);
            }
            if (this.cursor.check(TokenKind.LEFT_BRACE)) {
                statements.parseBlock();
            }
        }
        return true;
    }

    private static void refuse(final DiagnosticBag diagnostics, final Token at, final TextKey instead) {
        diagnostics.error(at.line(), at.column(), SigmaError.NOT_IN_THE_LANGUAGE, Text.literal(at.text()),
                instead.text());
    }

    /*
     * Runs past the next {@code end} at this depth of brackets, so a call's own parentheses are passed over whole. It
     * stops short of a brace that closes the block the statement stands in, which is the block's to read.
     */
    private void skipPast(final TokenKind end) {
        int depth = 0;
        while (!this.cursor.atEnd()) {
            final TokenKind kind = this.cursor.peek().kind();
            if (depth == 0 && kind == TokenKind.RIGHT_BRACE) {
                return;
            }
            this.cursor.advance();
            if (kind == TokenKind.LEFT_PAREN || kind == TokenKind.LEFT_BRACE) {
                depth++;
            } else if (kind == TokenKind.RIGHT_PAREN || kind == TokenKind.RIGHT_BRACE) {
                depth--;
            }
            if (depth == 0 && kind == end) {
                return;
            }
        }
    }
}
