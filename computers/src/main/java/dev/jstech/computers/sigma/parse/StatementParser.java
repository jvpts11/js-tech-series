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
import dev.jstech.computers.sigma.ast.IDecl;
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
    private final LanguageGaps gaps;

    /** INTERCAL's two words over a place, which are words only where a statement starts with one and a place. */
    private static final String IGNORE = "ignore";
    private static final String REMEMBER = "remember";
    /** The word a block that may hold pointers starts with. */
    private static final String UNSAFE = "unsafe";

    StatementParser(final TokenCursor cursor, final DiagnosticBag diagnostics, final TypeParser types,
                    final ExpressionParser expressions) {
        this.cursor = cursor;
        this.diagnostics = diagnostics;
        this.types = types;
        this.expressions = expressions;
        this.gaps = new LanguageGaps(cursor, diagnostics);
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
            case CONST:
                return this.parseLocalDeclaration(true, this.cursor.advance());
            case TRY:
                return this.parseTry();
            case THROW:
                return this.parseThrow();
            case GOTO:
                return this.parseGoto();
            case COMEFROM:
                this.cursor.advance();
                return new IStmt.ComeFrom(this.labelThenSemicolon(), start.line(), start.column());
            case ABSTAINFROM, REINSTATE:
                this.cursor.advance();
                return new IStmt.Abstain(this.labelThenSemicolon(), start.is(TokenKind.REINSTATE), start.line(),
                        start.column());
            case IDENTIFIER:
                if (this.cursor.kindAhead(1) == TokenKind.COLON) {
                    return this.parseLabeled();
                }
                if (this.placeWordAhead()) {
                    return this.parseIgnore();
                }
                // unsafe and a block, a word only there: nothing else written with it can be followed by one.
                if (UNSAFE.equals(start.text()) && this.cursor.kindAhead(1) == TokenKind.LEFT_BRACE) {
                    this.cursor.advance();
                    return new IStmt.Unsafe(this.parseBlock(), start.line(), start.column());
                }
                return this.parseDeclarationOrExpressionStatement();
            default:
                // A word kept for a later version is reported and passed over.
                if (this.gaps.passesStatement(this)) {
                    return null;
                }
                return this.parseDeclarationOrExpressionStatement();
        }
    }

    /* A label and the statement it marks: "done: Console.PrintLine("x");". */
    private IStmt parseLabeled() {
        final Token name = this.cursor.advance();
        this.cursor.advance();
        // A label at the end of a block marks nothing, which C# refuses, and so does this.
        final IStmt marked = this.cursor.check(TokenKind.RIGHT_BRACE) ? null : this.parseStatement();
        if (marked == null) {
            final Token at = this.cursor.peek();
            this.diagnostics.error(at.line(), at.column(), SigmaError.EXPECTED_STATEMENT, at.describe());
        }
        return new IStmt.Labeled(name.text(), marked == null ? new IStmt.Empty(name.line(), name.column()) : marked,
                name.line(), name.column());
    }

    /* "goto done;", "goto case 3;" or "goto default;". */
    private IStmt parseGoto() {
        final Token start = this.cursor.advance();
        if (this.cursor.match(TokenKind.CASE)) {
            final IExpr value = this.expressions.parseExpression();
            this.cursor.expect(TokenKind.SEMICOLON);
            return new IStmt.Goto(IStmt.Goto.Target.CASE, null, value, start.line(), start.column());
        }
        if (this.cursor.match(TokenKind.DEFAULT)) {
            this.cursor.expect(TokenKind.SEMICOLON);
            return new IStmt.Goto(IStmt.Goto.Target.DEFAULT, null, null, start.line(), start.column());
        }
        return new IStmt.Goto(IStmt.Goto.Target.LABEL, this.labelThenSemicolon(), null, start.line(),
                start.column());
    }

    private String labelThenSemicolon() {
        final String label = this.cursor.expectIdentifier();
        this.cursor.expect(TokenKind.SEMICOLON);
        return label;
    }

    /*
     * Whether the statement starts with ignore or remember and a place: the two words are words only there, so a
     * program that has a variable called ignore, as one written before them may, still reads it as a variable.
     */
    private boolean placeWordAhead() {
        final String word = this.cursor.peek().text();
        final TokenKind next = this.cursor.kindAhead(1);
        return (IGNORE.equals(word) || REMEMBER.equals(word))
                && (next == TokenKind.IDENTIFIER || next == TokenKind.THIS);
    }

    /* "ignore count;" or "remember this.count;": the place is any value the checker then holds to being one. */
    private IStmt parseIgnore() {
        final Token start = this.cursor.advance();
        final IExpr place = this.expressions.parseExpression();
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IStmt.Ignore(place, REMEMBER.equals(start.text()), start.line(), start.column());
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
        if (this.cursor.check(TokenKind.VAR) && this.cursor.kindAhead(1) == TokenKind.LEFT_PAREN) {
            return this.parseDeconstruct();
        }
        if (this.localFunctionAhead()) {
            return this.parseLocalFunction();
        }
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
        return this.parseLocalDeclaration(terminated, null);
    }

    /**
     * A local declaration; a constant one has had its {@code const} read already, and is placed at that word.
     *
     * @param constant the {@code const} it was written with, or null for a local that can change
     */
    private IStmt parseLocalDeclaration(final boolean terminated, final Token constant) {
        final Token start = constant == null ? this.cursor.peek() : constant;
        final TypeRef type = this.types.parseTypeRef();
        final String name = this.cursor.expectIdentifier();
        IExpr initializer = null;
        if (this.cursor.match(TokenKind.ASSIGN)) {
            initializer = this.expressions.parseExpression();
        }
        if (terminated) {
            this.cursor.expect(TokenKind.SEMICOLON);
        }
        return new IStmt.LocalDecl(type, name, initializer, constant != null, start.line(), start.column());
    }

    /*
     * A type followed by a name is a declaration; anything else at the head of a statement is an
     * expression. This is the one place the grammar genuinely needs more than one token of lookahead.
     */
    private boolean looksLikeDeclaration() {
        return this.types.typedNameAhead();
    }

    /* A type or void, a name and a bracket: a method declared in a method, which nothing else starts like. */
    private boolean localFunctionAhead() {
        final int at = this.cursor.at();
        final int after = this.cursor.kindAt(at) == TokenKind.VOID ? at + 1 : this.types.scanType(at);
        return after > at && this.cursor.kindAt(after) == TokenKind.IDENTIFIER
                && this.cursor.kindAt(after + 1) == TokenKind.LEFT_PAREN;
    }

    /* "try { } catch (IOException failed) { } catch { } finally { }": a catch or a finally at least. */
    private IStmt parseTry() {
        final Token start = this.cursor.advance();
        final IStmt.Block body = this.parseBlock();
        final List<IStmt.Catch> catches = new ArrayList<>();
        while (this.cursor.check(TokenKind.CATCH)) {
            final Token word = this.cursor.advance();
            TypeRef type = null;
            String name = null;
            if (this.cursor.match(TokenKind.LEFT_PAREN)) {
                type = this.types.parseTypeRef();
                if (this.cursor.check(TokenKind.IDENTIFIER)) {
                    name = this.cursor.advance().text();
                }
                this.cursor.expect(TokenKind.RIGHT_PAREN);
            }
            catches.add(new IStmt.Catch(type, name, this.parseBlock(), word.line(), word.column()));
        }
        final IStmt.Block cleanup = this.cursor.match(TokenKind.FINALLY) ? this.parseBlock() : null;
        if (catches.isEmpty() && cleanup == null) {
            final Token at = this.cursor.peek();
            this.diagnostics.error(at.line(), at.column(), SigmaError.EXPECTED_TOKEN, TokenKind.CATCH.describe(),
                    at.describe());
        }
        return new IStmt.Try(body, catches, cleanup, start.line(), start.column());
    }

    /* "throw new IOException("full");", or "throw;" in a catch, which throws on the exception it took. */
    private IStmt parseThrow() {
        final Token start = this.cursor.advance();
        final IExpr value = this.cursor.check(TokenKind.SEMICOLON) ? null : this.expressions.parseExpression();
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IStmt.Throw(value, start.line(), start.column());
    }

    /* "var (count, name) = Find();": a tuple taken apart into a new local for each of its places. */
    private IStmt parseDeconstruct() {
        final Token start = this.cursor.advance();
        this.cursor.advance();
        final List<IExpr.Name> names = new ArrayList<>();
        do {
            final Token name = this.cursor.peek();
            if (!name.is(TokenKind.IDENTIFIER)) {
                this.cursor.expectIdentifier();
                break;
            }
            this.cursor.advance();
            names.add(new IExpr.Name(name.text(), name.line(), name.column()));
        } while (this.cursor.match(TokenKind.COMMA));
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        this.cursor.expect(TokenKind.ASSIGN);
        final IExpr value = this.expressions.parseExpression();
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IStmt.Deconstruct(names, value, start.line(), start.column());
    }

    /* "int Twice(int x) { return x * 2; }" or "int Twice(int x) => x * 2;". */
    private IStmt parseLocalFunction() {
        final Token start = this.cursor.peek();
        final TypeRef returns = this.types.parseReturnType();
        final String name = this.cursor.expectIdentifier();
        final List<IDecl.Parameter> parameters = this.types.parseParameters();
        final Token bodyStart = this.cursor.peek();
        final IExpr.Lambda body;
        if (this.cursor.match(TokenKind.ARROW)) {
            final IExpr value = this.expressions.parseExpression();
            this.cursor.expect(TokenKind.SEMICOLON);
            body = new IExpr.Lambda(parameters, value, null, bodyStart.line(), bodyStart.column());
        } else {
            body = new IExpr.Lambda(parameters, null, this.parseBlock(), bodyStart.line(), bodyStart.column());
        }
        return new IStmt.LocalFunction(returns, name, body, start.line(), start.column());
    }
}
