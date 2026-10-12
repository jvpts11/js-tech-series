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
 * The words C# has that a later version of the language brings: try, catch, finally, throw, goto and const.
 *
 * <p>They are reserved now, so no program takes them as names, and a program that writes them is told so at the word,
 * with what to write until then, rather than failing on whatever the parser makes of them. Each statement they begin
 * is passed over whole, so the rest of the method is still read.
 */
@TextHolder
final class LanguageGaps {

    private final TokenCursor cursor;
    private final DiagnosticBag diagnostics;

    private static final Set<TokenKind> LATER = EnumSet.of(TokenKind.TRY, TokenKind.CATCH, TokenKind.FINALLY,
            TokenKind.THROW, TokenKind.GOTO, TokenKind.CONST);
    private static final TextKey EXCEPTIONS_INSTEAD = TextKey.of("jsc.sigma.language_gaps.exceptions_instead",
            "a mistake halts the program, and a call that can fail answers instead");
    private static final TextKey GOTO_INSTEAD = TextKey.of("jsc.sigma.language_gaps.goto_instead",
            "write a loop, and leave it with break or continue");
    private static final TextKey CONST_INSTEAD = TextKey.of("jsc.sigma.language_gaps.const_instead",
            "write a plain variable");
    private static final TextKey CONST_FIELD_INSTEAD = TextKey.of("jsc.sigma.language_gaps.const_field_instead",
            "write a static readonly field");
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

    /** Whether the word being read is a const before a declaration, which it then reports and passes over. */
    boolean passesConst(final boolean member) {
        if (!this.cursor.check(TokenKind.CONST)) {
            return false;
        }
        refuse(this.diagnostics, this.cursor.advance(), member ? CONST_FIELD_INSTEAD : CONST_INSTEAD);
        return true;
    }

    /**
     * Whether the statement being read begins with a try, a catch, a finally, a throw or a goto, which it then reports
     * and passes over whole, blocks and all.
     */
    boolean passesStatement(final StatementParser statements) {
        final TokenKind kind = this.cursor.peek().kind();
        if (kind == TokenKind.TRY || kind == TokenKind.CATCH || kind == TokenKind.FINALLY) {
            refuse(this.diagnostics, this.cursor.peek(), EXCEPTIONS_INSTEAD);
            while (this.cursor.check(TokenKind.TRY) || this.cursor.check(TokenKind.CATCH)
                    || this.cursor.check(TokenKind.FINALLY)) {
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
        if (kind == TokenKind.THROW || kind == TokenKind.GOTO) {
            refuse(this.diagnostics, this.cursor.advance(), kind == TokenKind.THROW ? EXCEPTIONS_INSTEAD
                    : GOTO_INSTEAD);
            this.skipPast(TokenKind.SEMICOLON);
            return true;
        }
        return false;
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
