/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import dev.jstech.computers.cannon.CannonError;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.lua.ast.ILuaExpr;
import dev.jstech.computers.cannon.lua.ast.ILuaStmt;
import dev.jstech.computers.cannon.lua.ast.LuaBlock;
import dev.jstech.computers.cannon.lua.ast.LuaChunk;
import dev.jstech.computers.cannon.lua.ast.LuaFunctionBody;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns Lua tokens into a syntax tree.
 *
 * <p>The grammar is the language's own, operator precedence included, and the messages are the
 * ones its reference parser gives, because a program written for another Lua is read here and its
 * author knows those words. Like that parser, this one stops at the first mistake.
 */
public final class LuaParser {

    /** Stops the parse at the first mistake, which has already been reported. */
    private static final class Stop extends RuntimeException {

        private static final long serialVersionUID = 1L;

        Stop() {
            super(null, null, false, false);
        }
    }

    private static final int UNARY_PRIORITY = 12;

    private final List<LuaToken> tokens;
    private final DiagnosticBag diagnostics;
    private final String name;
    private int position;

    public LuaParser(final List<LuaToken> tokens, final DiagnosticBag diagnostics, final String name) {
        this.tokens = tokens;
        this.diagnostics = diagnostics;
        this.name = name;
    }

    /** The chunk, or null when the source could not be read; the mistake is in the diagnostics. */
    public LuaChunk parse() {
        try {
            final LuaBlock body = this.block();
            if (!this.check(LuaTokenKind.END_OF_FILE)) {
                throw this.unexpected();
            }
            return new LuaChunk(this.name, new LuaFunctionBody(List.of(), true, body, 1, 1));
        } catch (final Stop stopped) {
            return null;
        }
    }

    // statements

    private LuaBlock block() {
        final LuaToken start = this.peek();
        final List<ILuaStmt> statements = new ArrayList<>();
        while (!this.blockEnds()) {
            if (this.check(LuaTokenKind.RETURN)) {
                statements.add(this.returnStatement());
                break;
            }
            final ILuaStmt statement = this.statement();
            if (statement != null) {
                statements.add(statement);
            }
        }
        return new LuaBlock(statements, start.line(), start.column());
    }

    private boolean blockEnds() {
        return switch (this.peek().kind()) {
            case END_OF_FILE, END, ELSE, ELSEIF, UNTIL -> true;
            default -> false;
        };
    }

    private ILuaStmt statement() {
        final LuaToken start = this.peek();
        switch (start.kind()) {
            case SEMICOLON -> {
                this.advance();
                return null;
            }
            case IF -> {
                return this.ifStatement();
            }
            case WHILE -> {
                this.advance();
                final ILuaExpr condition = this.expression();
                this.expect(LuaTokenKind.DO);
                final LuaBlock body = this.block();
                this.expectClose(LuaTokenKind.END, start);
                return new ILuaStmt.While(condition, body, start.line(), start.column());
            }
            case DO -> {
                this.advance();
                final LuaBlock body = this.block();
                this.expectClose(LuaTokenKind.END, start);
                return new ILuaStmt.Do(body, start.line(), start.column());
            }
            case FOR -> {
                return this.forStatement();
            }
            case REPEAT -> {
                this.advance();
                final LuaBlock body = this.block();
                this.expectClose(LuaTokenKind.UNTIL, start);
                final ILuaExpr condition = this.expression();
                return new ILuaStmt.Repeat(body, condition, start.line(), start.column());
            }
            case FUNCTION -> {
                return this.functionStatement();
            }
            case LOCAL -> {
                this.advance();
                if (this.match(LuaTokenKind.FUNCTION)) {
                    final String name = this.expectName();
                    return new ILuaStmt.LocalFunction(name, this.functionBody(start, false), start.line(),
                            start.column());
                }
                return this.localStatement(start);
            }
            case RETURN -> {
                return this.returnStatement();
            }
            case BREAK -> {
                this.advance();
                return new ILuaStmt.Break(start.line(), start.column());
            }
            case GOTO, DOUBLE_COLON -> {
                this.diagnostics.error(start.line(), start.column(), CannonError.LUA_NOT_SUPPORTED, "goto");
                throw new Stop();
            }
            default -> {
                return this.expressionStatement();
            }
        }
    }

    private ILuaStmt ifStatement() {
        final LuaToken start = this.advance();
        final List<ILuaStmt.Clause> clauses = new ArrayList<>();
        ILuaExpr condition = this.expression();
        this.expect(LuaTokenKind.THEN);
        LuaBlock body = this.block();
        clauses.add(new ILuaStmt.Clause(condition, body, start.line(), start.column()));
        LuaBlock otherwise = null;
        while (true) {
            final LuaToken at = this.peek();
            if (this.match(LuaTokenKind.ELSEIF)) {
                condition = this.expression();
                this.expect(LuaTokenKind.THEN);
                body = this.block();
                clauses.add(new ILuaStmt.Clause(condition, body, at.line(), at.column()));
                continue;
            }
            if (this.match(LuaTokenKind.ELSE)) {
                otherwise = this.block();
            }
            break;
        }
        this.expectClose(LuaTokenKind.END, start);
        return new ILuaStmt.If(clauses, otherwise, start.line(), start.column());
    }

    private ILuaStmt forStatement() {
        final LuaToken start = this.advance();
        final String first = this.expectName();
        if (this.match(LuaTokenKind.ASSIGN)) {
            final ILuaExpr from = this.expression();
            this.expect(LuaTokenKind.COMMA);
            final ILuaExpr limit = this.expression();
            final ILuaExpr step = this.match(LuaTokenKind.COMMA) ? this.expression() : null;
            this.expect(LuaTokenKind.DO);
            final LuaBlock body = this.block();
            this.expectClose(LuaTokenKind.END, start);
            return new ILuaStmt.NumericFor(first, from, limit, step, body, start.line(), start.column());
        }
        final List<String> names = new ArrayList<>();
        names.add(first);
        while (this.match(LuaTokenKind.COMMA)) {
            names.add(this.expectName());
        }
        this.expect(LuaTokenKind.IN);
        final List<ILuaExpr> values = this.expressionList();
        this.expect(LuaTokenKind.DO);
        final LuaBlock body = this.block();
        this.expectClose(LuaTokenKind.END, start);
        return new ILuaStmt.GenericFor(names, values, body, start.line(), start.column());
    }

    private ILuaStmt functionStatement() {
        final LuaToken start = this.advance();
        final List<String> path = new ArrayList<>();
        path.add(this.expectName());
        String method = null;
        while (this.check(LuaTokenKind.DOT) || this.check(LuaTokenKind.COLON)) {
            final boolean colon = this.advance().is(LuaTokenKind.COLON);
            final String part = this.expectName();
            if (colon) {
                method = part;
                break;
            }
            path.add(part);
        }
        return new ILuaStmt.FunctionDecl(path, method, this.functionBody(start, method != null),
                start.line(), start.column());
    }

    private ILuaStmt localStatement(final LuaToken start) {
        final List<String> names = new ArrayList<>();
        names.add(this.expectName());
        while (this.match(LuaTokenKind.COMMA)) {
            names.add(this.expectName());
        }
        final List<ILuaExpr> values = this.match(LuaTokenKind.ASSIGN) ? this.expressionList() : List.of();
        return new ILuaStmt.Local(names, values, start.line(), start.column());
    }

    private ILuaStmt returnStatement() {
        final LuaToken start = this.advance();
        final List<ILuaExpr> values = this.blockEnds() || this.check(LuaTokenKind.SEMICOLON)
                ? List.of() : this.expressionList();
        this.match(LuaTokenKind.SEMICOLON);
        if (!this.blockEnds()) {
            throw this.unexpected();
        }
        return new ILuaStmt.Return(values, start.line(), start.column());
    }

    private ILuaStmt expressionStatement() {
        final LuaToken start = this.peek();
        final ILuaExpr first = this.suffixedExpression();
        if (this.check(LuaTokenKind.ASSIGN) || this.check(LuaTokenKind.COMMA)) {
            final List<ILuaExpr> targets = new ArrayList<>();
            targets.add(this.assignable(first));
            while (this.match(LuaTokenKind.COMMA)) {
                targets.add(this.assignable(this.suffixedExpression()));
            }
            this.expect(LuaTokenKind.ASSIGN);
            final List<ILuaExpr> values = this.expressionList();
            return new ILuaStmt.Assign(targets, values, start.line(), start.column());
        }
        if (!(first instanceof ILuaExpr.Call) && !(first instanceof ILuaExpr.MethodCall)) {
            this.diagnostics.error(this.peek().line(), this.peek().column(), CannonError.LUA_ASSIGNMENT_TARGET,
                    this.peek().describe());
            throw new Stop();
        }
        return new ILuaStmt.CallStmt(first, start.line(), start.column());
    }

    private ILuaExpr assignable(final ILuaExpr expression) {
        if (expression instanceof ILuaExpr.Name || expression instanceof ILuaExpr.Index) {
            return expression;
        }
        this.diagnostics.error(expression.line(), expression.column(), CannonError.LUA_ASSIGNMENT_TARGET,
                this.peek().describe());
        throw new Stop();
    }

    private LuaFunctionBody functionBody(final LuaToken start, final boolean method) {
        final LuaToken at = this.peek();
        this.expect(LuaTokenKind.OPEN_PAREN);
        final List<String> parameters = new ArrayList<>();
        if (method) {
            parameters.add("self");
        }
        boolean varargs = false;
        if (!this.check(LuaTokenKind.CLOSE_PAREN)) {
            do {
                if (this.match(LuaTokenKind.ELLIPSIS)) {
                    varargs = true;
                    break;
                }
                parameters.add(this.expectName());
            } while (this.match(LuaTokenKind.COMMA));
        }
        this.expect(LuaTokenKind.CLOSE_PAREN);
        final LuaBlock body = this.block();
        this.expectClose(LuaTokenKind.END, start);
        return new LuaFunctionBody(parameters, varargs, body, at.line(), at.column());
    }

    // expressions

    private List<ILuaExpr> expressionList() {
        final List<ILuaExpr> values = new ArrayList<>();
        values.add(this.expression());
        while (this.match(LuaTokenKind.COMMA)) {
            values.add(this.expression());
        }
        return values;
    }

    private ILuaExpr expression() {
        return this.expression(0);
    }

    /* Precedence climbing with the language's table: each operator has a left and a right priority. */
    private ILuaExpr expression(final int limit) {
        final LuaToken start = this.peek();
        ILuaExpr left;
        if (this.check(LuaTokenKind.NOT) || this.check(LuaTokenKind.MINUS) || this.check(LuaTokenKind.HASH)) {
            final LuaToken operator = this.advance();
            final ILuaExpr operand = this.expression(UNARY_PRIORITY);
            left = new ILuaExpr.Unary(operator.kind(), operand, start.line(), start.column());
        } else {
            left = this.simpleExpression();
        }
        while (true) {
            final LuaToken operator = this.peek();
            final int leftPriority = leftPriority(operator.kind());
            if (leftPriority < 0 || leftPriority <= limit) {
                return left;
            }
            this.advance();
            final ILuaExpr right = this.expression(rightPriority(operator.kind()));
            if (operator.is(LuaTokenKind.AND) || operator.is(LuaTokenKind.OR)) {
                left = new ILuaExpr.Logical(operator.is(LuaTokenKind.AND), left, right, operator.line(),
                        operator.column());
            } else {
                left = new ILuaExpr.Binary(operator.kind(), left, right, operator.line(), operator.column());
            }
        }
    }

    private static int leftPriority(final LuaTokenKind kind) {
        return switch (kind) {
            case OR -> 1;
            case AND -> 2;
            case LESS, GREATER, LESS_EQUAL, GREATER_EQUAL, NOT_EQUAL, EQUAL -> 3;
            case CONCAT -> 9;
            case PLUS, MINUS -> 10;
            case STAR, SLASH, DOUBLE_SLASH, PERCENT -> 11;
            case CARET -> 14;
            default -> -1;
        };
    }

    private static int rightPriority(final LuaTokenKind kind) {
        return switch (kind) {
            case OR -> 1;
            case AND -> 2;
            case LESS, GREATER, LESS_EQUAL, GREATER_EQUAL, NOT_EQUAL, EQUAL -> 3;
            // Concatenation and powers bind to the right.
            case CONCAT -> 8;
            case PLUS, MINUS -> 10;
            case STAR, SLASH, DOUBLE_SLASH, PERCENT -> 11;
            case CARET -> 13;
            default -> -1;
        };
    }

    private ILuaExpr simpleExpression() {
        final LuaToken start = this.peek();
        switch (start.kind()) {
            case NUMBER -> {
                this.advance();
                return new ILuaExpr.Number(start.value(), start.line(), start.column());
            }
            case STRING -> {
                this.advance();
                return new ILuaExpr.Text(String.valueOf(start.value()), start.line(), start.column());
            }
            case NIL -> {
                this.advance();
                return new ILuaExpr.Nil(start.line(), start.column());
            }
            case TRUE, FALSE -> {
                this.advance();
                return new ILuaExpr.Bool(start.is(LuaTokenKind.TRUE), start.line(), start.column());
            }
            case ELLIPSIS -> {
                this.advance();
                return new ILuaExpr.Vararg(start.line(), start.column());
            }
            case OPEN_BRACE -> {
                return this.table();
            }
            case FUNCTION -> {
                this.advance();
                return new ILuaExpr.Function(this.functionBody(start, false), start.line(), start.column());
            }
            default -> {
                return this.suffixedExpression();
            }
        }
    }

    private ILuaExpr primaryExpression() {
        final LuaToken start = this.peek();
        if (start.is(LuaTokenKind.NAME)) {
            this.advance();
            return new ILuaExpr.Name(start.text(), start.line(), start.column());
        }
        if (start.is(LuaTokenKind.OPEN_PAREN)) {
            this.advance();
            final ILuaExpr inner = this.expression();
            this.expectClose(LuaTokenKind.CLOSE_PAREN, start);
            return new ILuaExpr.Paren(inner, start.line(), start.column());
        }
        throw this.unexpected();
    }

    private ILuaExpr suffixedExpression() {
        ILuaExpr expression = this.primaryExpression();
        while (true) {
            final LuaToken at = this.peek();
            switch (at.kind()) {
                case DOT -> {
                    this.advance();
                    final LuaToken name = this.peek();
                    final String field = this.expectName();
                    expression = new ILuaExpr.Index(expression,
                            new ILuaExpr.Text(field, name.line(), name.column()), at.line(), at.column());
                }
                case OPEN_BRACKET -> {
                    this.advance();
                    final ILuaExpr key = this.expression();
                    this.expectClose(LuaTokenKind.CLOSE_BRACKET, at);
                    expression = new ILuaExpr.Index(expression, key, at.line(), at.column());
                }
                case COLON -> {
                    this.advance();
                    final String method = this.expectName();
                    expression = new ILuaExpr.MethodCall(expression, method, this.callArguments(),
                            at.line(), at.column());
                }
                case OPEN_PAREN, STRING, OPEN_BRACE -> expression = new ILuaExpr.Call(expression,
                        this.callArguments(), at.line(), at.column());
                default -> {
                    return expression;
                }
            }
        }
    }

    private List<ILuaExpr> callArguments() {
        final LuaToken start = this.peek();
        if (start.is(LuaTokenKind.STRING)) {
            this.advance();
            return List.of(new ILuaExpr.Text(String.valueOf(start.value()), start.line(), start.column()));
        }
        if (start.is(LuaTokenKind.OPEN_BRACE)) {
            return List.of(this.table());
        }
        this.expect(LuaTokenKind.OPEN_PAREN);
        final List<ILuaExpr> arguments = this.check(LuaTokenKind.CLOSE_PAREN) ? new ArrayList<>()
                : this.expressionList();
        this.expectClose(LuaTokenKind.CLOSE_PAREN, start);
        return arguments;
    }

    private ILuaExpr table() {
        final LuaToken start = this.advance();
        final List<ILuaExpr.Field> fields = new ArrayList<>();
        while (!this.check(LuaTokenKind.CLOSE_BRACE)) {
            final LuaToken at = this.peek();
            if (at.is(LuaTokenKind.OPEN_BRACKET)) {
                this.advance();
                final ILuaExpr key = this.expression();
                this.expectClose(LuaTokenKind.CLOSE_BRACKET, at);
                this.expect(LuaTokenKind.ASSIGN);
                fields.add(new ILuaExpr.Field(key, this.expression(), at.line(), at.column()));
            } else if (at.is(LuaTokenKind.NAME) && this.kindAt(this.position + 1) == LuaTokenKind.ASSIGN) {
                this.advance();
                this.advance();
                fields.add(new ILuaExpr.Field(new ILuaExpr.Text(at.text(), at.line(), at.column()),
                        this.expression(), at.line(), at.column()));
            } else {
                fields.add(new ILuaExpr.Field(null, this.expression(), at.line(), at.column()));
            }
            if (!this.match(LuaTokenKind.COMMA) && !this.match(LuaTokenKind.SEMICOLON)) {
                break;
            }
        }
        this.expectClose(LuaTokenKind.CLOSE_BRACE, start);
        return new ILuaExpr.Table(fields, start.line(), start.column());
    }

    // tokens

    private LuaToken peek() {
        return this.tokens.get(Math.min(this.position, this.tokens.size() - 1));
    }

    private LuaTokenKind kindAt(final int index) {
        return this.tokens.get(Math.min(Math.max(index, 0), this.tokens.size() - 1)).kind();
    }

    private boolean check(final LuaTokenKind kind) {
        return this.peek().is(kind);
    }

    private boolean match(final LuaTokenKind kind) {
        if (this.check(kind)) {
            this.advance();
            return true;
        }
        return false;
    }

    private LuaToken advance() {
        final LuaToken token = this.peek();
        if (this.position < this.tokens.size() - 1) {
            this.position++;
        }
        return token;
    }

    private void expect(final LuaTokenKind kind) {
        if (this.match(kind)) {
            return;
        }
        final LuaToken found = this.peek();
        this.diagnostics.error(found.line(), found.column(), CannonError.LUA_EXPECTED, kind.describe(),
                found.describe());
        throw new Stop();
    }

    private void expectClose(final LuaTokenKind kind, final LuaToken opener) {
        if (this.match(kind)) {
            return;
        }
        final LuaToken found = this.peek();
        if (opener.line() == found.line()) {
            this.diagnostics.error(found.line(), found.column(), CannonError.LUA_EXPECTED, kind.describe(),
                    found.describe());
        } else {
            this.diagnostics.error(found.line(), found.column(), CannonError.LUA_EXPECTED_CLOSE, kind.describe(),
                    opener.text(), opener.line(), found.describe());
        }
        throw new Stop();
    }

    private String expectName() {
        if (this.check(LuaTokenKind.NAME)) {
            return this.advance().text();
        }
        final LuaToken found = this.peek();
        this.diagnostics.error(found.line(), found.column(), CannonError.LUA_EXPECTED, "<name>",
                found.describe());
        throw new Stop();
    }

    private Stop unexpected() {
        final LuaToken found = this.peek();
        this.diagnostics.error(found.line(), found.column(), CannonError.LUA_UNEXPECTED, found.describe());
        return new Stop();
    }
}
