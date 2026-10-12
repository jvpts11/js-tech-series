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

/**
 * What C# has and the language has not, said for what it is when a program writes it as C# does.
 *
 * <p>try, throw, goto and const are not words of the language, so a program may still use them as names. Written
 * the way C# writes them, though, they read as names and fail with a message about something else entirely. These
 * shapes cannot be names at work, so they are told apart by what follows the word, and each refusal says what to
 * write instead, as the smaller language's refusals do.
 */
@TextHolder
final class LanguageGaps {

    private final TokenCursor cursor;
    private final DiagnosticBag diagnostics;
    private final TypeParser types;

    private static final TextKey EXCEPTIONS = TextKey.of("jsc.sigma.language_gaps.exceptions", "exceptions");
    private static final TextKey EXCEPTIONS_INSTEAD = TextKey.of("jsc.sigma.language_gaps.exceptions_instead",
            "a mistake halts the program, and a call that can fail answers instead");
    private static final TextKey GOTO_INSTEAD = TextKey.of("jsc.sigma.language_gaps.goto_instead",
            "write a loop, and leave it with break or continue");
    private static final TextKey CONST_INSTEAD = TextKey.of("jsc.sigma.language_gaps.const_instead",
            "write a plain variable");
    private static final TextKey CONST_FIELD_INSTEAD = TextKey.of("jsc.sigma.language_gaps.const_field_instead",
            "write a static readonly field");

    LanguageGaps(final TokenCursor cursor, final DiagnosticBag diagnostics, final TypeParser types) {
        this.cursor = cursor;
        this.diagnostics = diagnostics;
        this.types = types;
    }

    /** Whether the word being read is a const before a declaration, which it then reports and passes over. */
    boolean passesConst(final boolean member) {
        if (!this.wordIs("const") || !this.types.isTypeStart(this.cursor.kindAhead(1))) {
            return false;
        }
        this.refuse(this.cursor.advance(), Text.literal("const"), member ? CONST_FIELD_INSTEAD : CONST_INSTEAD);
        return true;
    }

    /**
     * Whether the statement being read is a try, a throw or a goto as C# writes it, which it then reports and passes
     * over whole, so the rest of the method is still read.
     */
    boolean passesStatement(final StatementParser statements) {
        if ((this.wordIs("try") || this.wordIs("finally")) && this.cursor.kindAhead(1) == TokenKind.LEFT_BRACE) {
            this.refuse(this.cursor.advance(), EXCEPTIONS.text(), EXCEPTIONS_INSTEAD);
            statements.parseBlock();
            while (this.wordIs("catch") || this.wordIs("finally")) {
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
        if (this.wordIs("throw") && this.ends(TokenKind.NEW, TokenKind.SEMICOLON)) {
            this.refuse(this.cursor.advance(), EXCEPTIONS.text(), EXCEPTIONS_INSTEAD);
            this.skipPast(TokenKind.SEMICOLON);
            return true;
        }
        if (this.wordIs("goto") && this.ends(TokenKind.CASE, TokenKind.DEFAULT)) {
            this.refuse(this.cursor.advance(), Text.literal("goto"), GOTO_INSTEAD);
            this.skipPast(TokenKind.SEMICOLON);
            return true;
        }
        return false;
    }

    private boolean wordIs(final String word) {
        final Token token = this.cursor.peek();
        return token.kind() == TokenKind.IDENTIFIER && word.equals(token.text());
    }

    /*
     * Whether what follows the word can only be the C# statement: one of these words, or a name and then the end of
     * the statement, as in throw failure; or goto again;, which as a declaration would name a type no program has.
     */
    private boolean ends(final TokenKind first, final TokenKind second) {
        final TokenKind next = this.cursor.kindAhead(1);
        return next == first || next == second
                || next == TokenKind.IDENTIFIER && this.cursor.kindAhead(2) == TokenKind.SEMICOLON;
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

    private void refuse(final Token at, final Text what, final TextKey instead) {
        this.diagnostics.error(at.line(), at.column(), SigmaError.NOT_IN_THE_LANGUAGE, what, instead.text());
    }
}
