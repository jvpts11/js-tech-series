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
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.ast.TypeRef;
import dev.jstech.computers.sigma.lex.Token;
import dev.jstech.computers.sigma.lex.TokenKind;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads what a method does: one method per shape of statement.
 *
 * <p>Almost every shape begins with a word that says which it is, so a statement is decided on one token
 * and read from there. The exception is the one at the bottom, where a type followed by a name declares
 * something and anything else is a value being worked out for what it does; telling those two apart is
 * the one place the grammar genuinely needs to look further than the next token.
 */
final class StatementParser {

    private final TokenCursor cursor;
    private final DiagnosticBag diagnostics;
    private final TypeParser types;
    private final ExpressionParser expressions;

    StatementParser(final TokenCursor cursor, final DiagnosticBag diagnostics, final TypeParser types,
                    final ExpressionParser expressions) {
        this.cursor = cursor;
        this.diagnostics = diagnostics;
        this.types = types;
        this.expressions = expressions;
    }

    IStmt.Block parseBlock() {
        final Token start = this.cursor.peek();
        final List<IStmt> statements = new ArrayList<>();
        if (!this.cursor.expect(TokenKind.LEFT_BRACE)) {
            return new IStmt.Block(statements, start.line(), start.column());
        }
        while (!this.cursor.check(TokenKind.RIGHT_BRACE) && !this.cursor.atEnd()) {
            final int before = this.cursor.at();
            final IStmt statement = this.parseStatement();
            if (statement != null) {
                statements.add(statement);
            }
            if (this.cursor.at() == before) {
                this.cursor.advance();
            }
        }
        this.cursor.expect(TokenKind.RIGHT_BRACE);
        return new IStmt.Block(statements, start.line(), start.column());
    }

    /* Blocks and the statements that hold statements read themselves a level down, so each counts once. */
    private IStmt parseStatement() {
        if (!this.cursor.descend()) {
            return null;
        }
        try {
            return this.statement();
        } finally {
            this.cursor.ascend();
        }
    }

    private IStmt statement() {
        final Token start = this.cursor.peek();
        switch (start.kind()) {
            case LEFT_BRACE:
                return this.parseBlock();
            case SEMICOLON:
                this.cursor.advance();
                return new IStmt.Empty(start.line(), start.column());
            case IF:
                return this.parseIf();
            case WHILE:
                return this.parseWhile();
            case DO:
                return this.parseDoWhile();
            case FOR:
                return this.parseFor();
            case FOREACH:
                return this.parseForEach();
            case SWITCH:
                return this.parseSwitch();
            case BREAK:
                this.cursor.advance();
                this.cursor.expect(TokenKind.SEMICOLON);
                return new IStmt.Break(start.line(), start.column());
            case CONTINUE:
                this.cursor.advance();
                this.cursor.expect(TokenKind.SEMICOLON);
                return new IStmt.Continue(start.line(), start.column());
            case RETURN:
                return this.parseReturn();
            case DISPOSE:
                return this.parseDispose();
            case LOCK:
                return this.parseLock();
            default:
                return this.parseDeclarationOrExpressionStatement();
        }
    }

    private IStmt parseLock() {
        final Token start = this.cursor.advance();
        this.cursor.expect(TokenKind.LEFT_PAREN);
        final IExpr target = this.expressions.parseExpression();
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        final IStmt body = this.parseStatement();
        return new IStmt.Lock(target, body, start.line(), start.column());
    }

    private IStmt parseIf() {
        final Token start = this.cursor.advance();
        this.cursor.expect(TokenKind.LEFT_PAREN);
        final IExpr condition = this.expressions.parseExpression();
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        final IStmt then = this.parseStatement();
        IStmt otherwise = null;
        if (this.cursor.match(TokenKind.ELSE)) {
            otherwise = this.parseStatement();
        }
        return new IStmt.If(condition, then, otherwise, start.line(), start.column());
    }

    private IStmt parseWhile() {
        final Token start = this.cursor.advance();
        this.cursor.expect(TokenKind.LEFT_PAREN);
        final IExpr condition = this.expressions.parseExpression();
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        final IStmt body = this.parseStatement();
        return new IStmt.While(condition, body, start.line(), start.column());
    }

    private IStmt parseDoWhile() {
        final Token start = this.cursor.advance();
        final IStmt body = this.parseStatement();
        this.cursor.expect(TokenKind.WHILE);
        this.cursor.expect(TokenKind.LEFT_PAREN);
        final IExpr condition = this.expressions.parseExpression();
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IStmt.DoWhile(body, condition, start.line(), start.column());
    }

    private IStmt parseFor() {
        final Token start = this.cursor.advance();
        this.cursor.expect(TokenKind.LEFT_PAREN);
        final List<IStmt> initializers = new ArrayList<>();
        if (!this.cursor.check(TokenKind.SEMICOLON)) {
            if (this.looksLikeDeclaration()) {
                initializers.add(this.parseLocalDeclaration(false));
            } else {
                do {
                    final Token at = this.cursor.peek();
                    final IExpr expression = this.expressions.parseExpression();
                    if (expression != null) {
                        initializers.add(new IStmt.ExprStmt(expression, at.line(), at.column()));
                    }
                } while (this.cursor.match(TokenKind.COMMA));
            }
        }
        this.cursor.expect(TokenKind.SEMICOLON);
        final IExpr condition = this.cursor.check(TokenKind.SEMICOLON)
                ? null : this.expressions.parseExpression();
        this.cursor.expect(TokenKind.SEMICOLON);
        final List<IExpr> updates = new ArrayList<>();
        if (!this.cursor.check(TokenKind.RIGHT_PAREN)) {
            do {
                final IExpr update = this.expressions.parseExpression();
                if (update != null) {
                    updates.add(update);
                }
            } while (this.cursor.match(TokenKind.COMMA));
        }
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        final IStmt body = this.parseStatement();
        return new IStmt.For(initializers, condition, updates, body, start.line(), start.column());
    }

    private IStmt parseForEach() {
        final Token start = this.cursor.advance();
        this.cursor.expect(TokenKind.LEFT_PAREN);
        final TypeRef type = this.types.parseTypeRef();
        final String name = this.cursor.expectIdentifier();
        this.cursor.expect(TokenKind.IN);
        final IExpr source = this.expressions.parseExpression();
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        final IStmt body = this.parseStatement();
        return new IStmt.ForEach(type, name, source, body, start.line(), start.column());
    }

    private IStmt parseSwitch() {
        final Token start = this.cursor.advance();
        this.cursor.expect(TokenKind.LEFT_PAREN);
        final IExpr value = this.expressions.parseExpression();
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        final List<IStmt.SwitchSection> sections = new ArrayList<>();
        if (this.cursor.expect(TokenKind.LEFT_BRACE)) {
            while (!this.cursor.check(TokenKind.RIGHT_BRACE) && !this.cursor.atEnd()) {
                final int before = this.cursor.at();
                final IStmt.SwitchSection section = this.parseSwitchSection();
                if (section != null) {
                    sections.add(section);
                }
                if (this.cursor.at() == before) {
                    this.cursor.advance();
                }
            }
            this.cursor.expect(TokenKind.RIGHT_BRACE);
        }
        return new IStmt.Switch(value, sections, start.line(), start.column());
    }

    private IStmt.SwitchSection parseSwitchSection() {
        final Token start = this.cursor.peek();
        final List<IExpr> labels = new ArrayList<>();
        boolean fallback = false;
        while (this.cursor.check(TokenKind.CASE) || this.cursor.check(TokenKind.DEFAULT)) {
            if (this.cursor.match(TokenKind.CASE)) {
                final IExpr label = this.expressions.parseExpression();
                if (label != null) {
                    labels.add(label);
                }
            } else {
                this.cursor.advance();
                fallback = true;
            }
            this.cursor.expect(TokenKind.COLON);
        }
        if (labels.isEmpty() && !fallback) {
            this.diagnostics.error(start.line(), start.column(),
                    SigmaError.EXPECTED_TOKEN, TokenKind.CASE.describe(), start.describe());
            return null;
        }
        final List<IStmt> statements = new ArrayList<>();
        while (!this.cursor.check(TokenKind.CASE) && !this.cursor.check(TokenKind.DEFAULT)
                && !this.cursor.check(TokenKind.RIGHT_BRACE) && !this.cursor.atEnd()) {
            final int before = this.cursor.at();
            final IStmt statement = this.parseStatement();
            if (statement != null) {
                statements.add(statement);
            }
            if (this.cursor.at() == before) {
                this.cursor.advance();
            }
        }
        return new IStmt.SwitchSection(labels, fallback, statements, start.line(), start.column());
    }

    private IStmt parseReturn() {
        final Token start = this.cursor.advance();
        final IExpr value = this.cursor.check(TokenKind.SEMICOLON)
                ? null : this.expressions.parseExpression();
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IStmt.Return(value, start.line(), start.column());
    }

    private IStmt parseDispose() {
        final Token start = this.cursor.advance();
        final IExpr target = this.expressions.parseExpression();
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IStmt.Dispose(target, start.line(), start.column());
    }

    private IStmt parseDeclarationOrExpressionStatement() {
        if (this.looksLikeDeclaration()) {
            return this.parseLocalDeclaration(true);
        }
        final Token start = this.cursor.peek();
        final IExpr expression = this.expressions.parseExpression();
        if (expression == null) {
            return null;
        }
        if (!ExpressionParser.isStatementExpression(expression)) {
            this.diagnostics.error(start.line(), start.column(), SigmaError.NOT_A_STATEMENT);
        }
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IStmt.ExprStmt(expression, start.line(), start.column());
    }

    private IStmt parseLocalDeclaration(final boolean terminated) {
        final Token start = this.cursor.peek();
        final TypeRef type = this.types.parseTypeRef();
        final String name = this.cursor.expectIdentifier();
        IExpr initializer = null;
        if (this.cursor.match(TokenKind.ASSIGN)) {
            initializer = this.expressions.parseExpression();
        }
        if (terminated) {
            this.cursor.expect(TokenKind.SEMICOLON);
        }
        return new IStmt.LocalDecl(type, name, initializer, start.line(), start.column());
    }

    /*
     * A type followed by a name is a declaration; anything else at the head of a statement is an
     * expression. This is the one place the grammar genuinely needs more than one token of lookahead.
     */
    private boolean looksLikeDeclaration() {
        final int after = this.types.scanType(this.cursor.at());
        return after > this.cursor.at() && this.cursor.kindAt(after) == TokenKind.IDENTIFIER;
    }
}
