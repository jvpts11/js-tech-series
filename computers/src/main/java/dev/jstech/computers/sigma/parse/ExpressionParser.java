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
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.ast.TypeRef;
import dev.jstech.computers.sigma.lex.Lexer;
import dev.jstech.computers.sigma.lex.Token;
import dev.jstech.computers.sigma.lex.TokenKind;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reads a value: everything that can stand where the language wants something worked out.
 *
 * <p>Precedence is a table rather than nine methods that differ by one line, and the levels are in the
 * order C# puts them, loosest first. Everything else is one method per shape.
 *
 * <p>A lambda is the one place a value holds statements, so this is also the one place the grammar folds
 * back on itself. The reader of statements is made here for that reason, and handed on to whoever else
 * needs it, since there is only ever one file being read.
 */
@TextHolder
final class ExpressionParser {

    private final TokenCursor cursor;
    private final DiagnosticBag diagnostics;
    private final TypeParser types;
    private final StatementParser statements;

    /** What a hole in an interpolated string was expected to end with, where something else followed. */
    private static final TextKey END_OF_HOLE = TextKey.of("jsc.sigma.expression_parser.end_of_hole",
            "the end of the hole");

    private static final Set<TokenKind> LITERALS = EnumSet.of(
            TokenKind.INT_LITERAL, TokenKind.LONG_LITERAL, TokenKind.FLOAT_LITERAL, TokenKind.DOUBLE_LITERAL,
            TokenKind.STRING_LITERAL, TokenKind.CHAR_LITERAL, TokenKind.TRUE, TokenKind.FALSE, TokenKind.NULL);

    /*
     * One table instead of nine near-identical methods; the index is the precedence level, lowest
     * binding first, exactly as C# orders them.
     */
    private static final TokenKind[][] BINARY_LEVELS = {
        {TokenKind.OR_OR},
        {TokenKind.AND_AND},
        {TokenKind.PIPE},
        {TokenKind.CARET},
        {TokenKind.AMPERSAND},
        {TokenKind.EQUAL, TokenKind.NOT_EQUAL},
        {TokenKind.LESS, TokenKind.LESS_EQUAL, TokenKind.GREATER, TokenKind.GREATER_EQUAL},
        {TokenKind.SHIFT_LEFT, TokenKind.SHIFT_RIGHT},
        {TokenKind.PLUS, TokenKind.MINUS},
        {TokenKind.STAR, TokenKind.SLASH, TokenKind.PERCENT},
    };

    /** The level that also carries {@code is} and {@code as}, which bind like a comparison. */
    private static final int RELATIONAL_LEVEL = 6;

    ExpressionParser(final TokenCursor cursor, final DiagnosticBag diagnostics, final TypeParser types) {
        this.cursor = cursor;
        this.diagnostics = diagnostics;
        this.types = types;
        this.statements = new StatementParser(cursor, diagnostics, types, this);
    }

    /** The reader of statements that goes with this one, since a lambda's body is made of them. */
    StatementParser statements() {
        return this.statements;
    }

    IExpr parseExpression() {
        if (!this.cursor.descend()) {
            return null;
        }
        try {
            return this.parseAssignment();
        } finally {
            this.cursor.ascend();
        }
    }

    /** Reads one expression standing on its own, as a hole in an interpolated string holds one. */
    IExpr parseLoneExpression() {
        final IExpr expression = this.parseExpression();
        if (!this.cursor.atEnd()) {
            final Token extra = this.cursor.peek();
            this.diagnostics.error(extra.line(), extra.column(), SigmaError.EXPECTED_TOKEN, END_OF_HOLE,
                    extra.describe());
        }
        return expression;
    }

    List<IExpr> parseArguments() {
        final List<IExpr> arguments = new ArrayList<>();
        if (!this.cursor.expect(TokenKind.LEFT_PAREN)) {
            return arguments;
        }
        while (!this.cursor.check(TokenKind.RIGHT_PAREN) && !this.cursor.atEnd()) {
            final int before = this.cursor.at();
            final IExpr argument = this.cursor.check(TokenKind.OUT)
                    ? this.parseOutArgument() : this.parseExpression();
            if (argument != null) {
                arguments.add(argument);
            }
            if (!this.cursor.match(TokenKind.COMMA)) {
                break;
            }
            if (this.cursor.at() == before) {
                this.cursor.advance();
            }
        }
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        return arguments;
    }

    /*
     * Evaluating a value and throwing it away is always a mistake, so only the forms that do
     * something are allowed to stand alone.
     */
    static boolean isStatementExpression(final IExpr expression) {
        return switch (expression) {
            case IExpr.Call ignored -> true;
            case IExpr.Assign ignored -> true;
            case IExpr.New ignored -> true;
            case IExpr.Unary unary -> unary.operator() == Operator.INCREMENT
                    || unary.operator() == Operator.DECREMENT;
            default -> false;
        };
    }

    private IExpr parseAssignment() {
        final IExpr left = this.parseConditional();
        final Operator operator = assignmentOperator(this.cursor.peek().kind());
        if (operator == null || left == null) {
            return left;
        }
        final Token at = this.cursor.advance();
        final IExpr value = this.parseAssignment();
        if (!isAssignable(left)) {
            this.diagnostics.error(at.line(), at.column(), SigmaError.INVALID_ASSIGNMENT_TARGET);
        }
        return new IExpr.Assign(left, operator, value, left.line(), left.column());
    }

    private static Operator assignmentOperator(final TokenKind kind) {
        return switch (kind) {
            case ASSIGN -> Operator.ASSIGN;
            case PLUS_ASSIGN -> Operator.ADD;
            case MINUS_ASSIGN -> Operator.SUBTRACT;
            case STAR_ASSIGN -> Operator.MULTIPLY;
            case SLASH_ASSIGN -> Operator.DIVIDE;
            case PERCENT_ASSIGN -> Operator.REMAINDER;
            case AMPERSAND_ASSIGN -> Operator.BIT_AND;
            case PIPE_ASSIGN -> Operator.BIT_OR;
            case CARET_ASSIGN -> Operator.BIT_XOR;
            case SHIFT_LEFT_ASSIGN -> Operator.SHIFT_LEFT;
            case SHIFT_RIGHT_ASSIGN -> Operator.SHIFT_RIGHT;
            default -> null;
        };
    }

    private static boolean isAssignable(final IExpr expression) {
        return expression instanceof IExpr.Name
                || expression instanceof IExpr.Member
                || expression instanceof IExpr.Index;
    }

    private IExpr parseConditional() {
        final IExpr condition = this.parseBinary(0);
        if (!this.cursor.check(TokenKind.QUESTION)) {
            return condition;
        }
        this.cursor.advance();
        final IExpr whenTrue = this.parseAssignment();
        this.cursor.expect(TokenKind.COLON);
        final IExpr whenFalse = this.parseAssignment();
        return new IExpr.Conditional(condition, whenTrue, whenFalse,
                condition == null ? this.cursor.peek().line() : condition.line(),
                condition == null ? this.cursor.peek().column() : condition.column());
    }

    private IExpr parseBinary(final int level) {
        if (level >= BINARY_LEVELS.length) {
            return this.parseUnary();
        }
        IExpr left = this.parseBinary(level + 1);
        while (true) {
            if (level == RELATIONAL_LEVEL
                    && (this.cursor.check(TokenKind.IS) || this.cursor.check(TokenKind.AS))) {
                final Token at = this.cursor.advance();
                final TypeRef type = this.types.parseTypeRef();
                left = new IExpr.TypeTest(left, type, at.is(TokenKind.AS),
                        left == null ? at.line() : left.line(), left == null ? at.column() : left.column());
                continue;
            }
            final TokenKind kind = this.cursor.matchAny(BINARY_LEVELS[level]);
            if (kind == null) {
                return left;
            }
            final IExpr right = this.parseBinary(level + 1);
            left = new IExpr.Binary(binaryOperator(kind), left, right,
                    left == null ? this.cursor.peek().line() : left.line(),
                    left == null ? this.cursor.peek().column() : left.column());
        }
    }

    private static Operator binaryOperator(final TokenKind kind) {
        return switch (kind) {
            case OR_OR -> Operator.OR;
            case AND_AND -> Operator.AND;
            case PIPE -> Operator.BIT_OR;
            case CARET -> Operator.BIT_XOR;
            case AMPERSAND -> Operator.BIT_AND;
            case EQUAL -> Operator.EQUAL;
            case NOT_EQUAL -> Operator.NOT_EQUAL;
            case LESS -> Operator.LESS;
            case LESS_EQUAL -> Operator.LESS_EQUAL;
            case GREATER -> Operator.GREATER;
            case GREATER_EQUAL -> Operator.GREATER_EQUAL;
            case SHIFT_LEFT -> Operator.SHIFT_LEFT;
            case SHIFT_RIGHT -> Operator.SHIFT_RIGHT;
            case PLUS -> Operator.ADD;
            case MINUS -> Operator.SUBTRACT;
            case STAR -> Operator.MULTIPLY;
            case SLASH -> Operator.DIVIDE;
            default -> Operator.REMAINDER;
        };
    }

    private IExpr parseUnary() {
        final Token start = this.cursor.peek();
        switch (start.kind()) {
            case NOT:
                this.cursor.advance();
                return new IExpr.Unary(Operator.NOT, this.operand(), false, start.line(), start.column());
            case MINUS:
                this.cursor.advance();
                return new IExpr.Unary(Operator.NEGATE, this.operand(), false, start.line(), start.column());
            case PLUS:
                this.cursor.advance();
                return new IExpr.Unary(Operator.PLUS, this.operand(), false, start.line(), start.column());
            case TILDE:
                this.cursor.advance();
                return new IExpr.Unary(Operator.COMPLEMENT, this.operand(), false, start.line(), start.column());
            case PLUS_PLUS:
                this.cursor.advance();
                return new IExpr.Unary(Operator.INCREMENT, this.operand(), false, start.line(), start.column());
            case MINUS_MINUS:
                this.cursor.advance();
                return new IExpr.Unary(Operator.DECREMENT, this.operand(), false, start.line(), start.column());
            default:
                break;
        }
        if (this.isCastAhead()) {
            this.cursor.advance();
            final TypeRef type = this.types.parseTypeRef();
            this.cursor.expect(TokenKind.RIGHT_PAREN);
            return new IExpr.Cast(type, this.operand(), start.line(), start.column());
        }
        return this.parsePostfix();
    }

    /* What a unary operator or a cast applies to sits a level inside it, so a chain of them counts too. */
    private IExpr operand() {
        if (!this.cursor.descend()) {
            return null;
        }
        try {
            return this.parseUnary();
        } finally {
            this.cursor.ascend();
        }
    }

    /*
     * "(int) x" is a conversion and "(a) + b" is a sum in brackets. The rule is the one C# uses: a
     * built-in type name always converts, and any other name only converts when what follows could
     * start a value on its own.
     */
    private boolean isCastAhead() {
        if (!this.cursor.check(TokenKind.LEFT_PAREN) || this.isLambdaAhead()) {
            return false;
        }
        final int after = this.types.scanType(this.cursor.at() + 1);
        if (after < 0 || this.cursor.kindAt(after) != TokenKind.RIGHT_PAREN) {
            return false;
        }
        final TokenKind next = this.cursor.kindAt(after + 1);
        if (this.types.isBuiltIn(this.cursor.kindAhead(1))) {
            return true;
        }
        return next == TokenKind.IDENTIFIER || next == TokenKind.THIS || next == TokenKind.BASE
                || next == TokenKind.NEW || next == TokenKind.NOT || next == TokenKind.TILDE
                || next == TokenKind.LEFT_PAREN || LITERALS.contains(next);
    }

    private boolean isLambdaAhead() {
        if (!this.cursor.check(TokenKind.LEFT_PAREN)) {
            return false;
        }
        int depth = 0;
        int at = this.cursor.at();
        while (true) {
            final TokenKind kind = this.cursor.kindAt(at);
            if (kind == TokenKind.END_OF_FILE) {
                return false;
            }
            if (kind == TokenKind.LEFT_PAREN) {
                depth++;
            } else if (kind == TokenKind.RIGHT_PAREN) {
                depth--;
                if (depth == 0) {
                    return this.cursor.kindAt(at + 1) == TokenKind.ARROW;
                }
            }
            at++;
        }
    }

    private IExpr parsePostfix() {
        IExpr expression = this.parsePrimary();
        while (true) {
            if (this.cursor.check(TokenKind.DOT)) {
                final Token dot = this.cursor.advance();
                final String name = this.cursor.expectIdentifier();
                expression = new IExpr.Member(expression, name,
                        expression == null ? dot.line() : expression.line(),
                        expression == null ? dot.column() : expression.column());
            } else if (this.cursor.check(TokenKind.LEFT_PAREN)) {
                final Token at = this.cursor.peek();
                final List<IExpr> arguments = this.parseArguments();
                expression = new IExpr.Call(expression, arguments,
                        expression == null ? at.line() : expression.line(),
                        expression == null ? at.column() : expression.column());
            } else if (this.cursor.check(TokenKind.LEFT_BRACKET)) {
                final Token at = this.cursor.advance();
                final IExpr index = this.parseExpression();
                this.cursor.expect(TokenKind.RIGHT_BRACKET);
                expression = new IExpr.Index(expression, index,
                        expression == null ? at.line() : expression.line(),
                        expression == null ? at.column() : expression.column());
            } else if (this.cursor.check(TokenKind.PLUS_PLUS) || this.cursor.check(TokenKind.MINUS_MINUS)) {
                final Token at = this.cursor.advance();
                expression = new IExpr.Unary(at.is(TokenKind.PLUS_PLUS) ? Operator.INCREMENT : Operator.DECREMENT,
                        expression, true,
                        expression == null ? at.line() : expression.line(),
                        expression == null ? at.column() : expression.column());
            } else {
                return expression;
            }
        }
    }

    /*
     * "out value" hands over a place that already exists; "out int value" and "out var value" declare
     * it right there, which is where a player wants it when the call is the only reason it exists.
     */
    private IExpr parseOutArgument() {
        final Token start = this.cursor.advance();
        final int afterType = this.types.scanType(this.cursor.at());
        TypeRef type = null;
        if (afterType > this.cursor.at() && this.cursor.kindAt(afterType) == TokenKind.IDENTIFIER) {
            type = this.types.parseTypeRef();
        }
        final String name = this.cursor.expectIdentifier();
        return new IExpr.OutArgument(type, name, start.line(), start.column());
    }

    /**
     * A string with holes in it, kept as what was written.
     *
     * <p>What it amounts to is the pieces added together, and that is what it becomes, but later and elsewhere.
     * Turning it into additions here would throw away the one thing that tells a string with holes apart from
     * the additions somebody wrote by hand, and that difference is what decides whether a source is allowed in
     * the smaller language at all.
     */
    private IExpr parseInterpolated(final Token token) {
        @SuppressWarnings("unchecked")
        final List<Object> parts = (List<Object>) token.value();
        final List<Object> read = new ArrayList<>(parts.size());
        for (final Object part : parts) {
            read.add(part instanceof Lexer.Hole hole ? this.parseHole(token, hole) : part);
        }
        return new IExpr.Interpolation(read, token.line(), token.column());
    }

    /**
     * The expression in one hole, read by a lexer and parser of its own.
     *
     * <p>The code is padded with the lines and columns before it, so anything wrong inside the hole is
     * reported where it sits in the file rather than at the start of a string nobody can find.
     */
    private IExpr parseHole(final Token token, final Lexer.Hole hole) {
        if (hole.code().isBlank()) {
            this.diagnostics.error(hole.line(), hole.column(), SigmaError.EXPECTED_EXPRESSION, "'}'");
            return new IExpr.Literal(TokenKind.STRING_LITERAL, "", token.line(), token.column());
        }
        final String padded = "\n".repeat(Math.max(0, hole.line() - 1)) + " ".repeat(Math.max(0, hole.column() - 1))
                + hole.code();
        final Lexer lexer = new Lexer(new SourceFile("", padded), this.diagnostics);
        // The hole is read as deep as the string it sits in, so holes nested in holes still meet the limit.
        return new Parser(lexer.tokenize(), this.diagnostics, this.cursor.depth()).parseLoneExpression();
    }

    private IExpr parsePrimary() {
        final Token start = this.cursor.peek();
        if (start.kind() == TokenKind.INTERPOLATED_STRING) {
            this.cursor.advance();
            return this.parseInterpolated(start);
        }
        if (LITERALS.contains(start.kind())) {
            this.cursor.advance();
            return new IExpr.Literal(start.kind(), start.value(), start.line(), start.column());
        }
        switch (start.kind()) {
            case THIS:
                this.cursor.advance();
                return new IExpr.This(start.line(), start.column());
            case BASE:
                this.cursor.advance();
                return new IExpr.Base(start.line(), start.column());
            case NEW:
                return this.parseNew();
            case IDENTIFIER:
                if (this.cursor.kindAhead(1) == TokenKind.ARROW) {
                    return this.parseShorthandLambda();
                }
                this.cursor.advance();
                return new IExpr.Name(start.text(), start.line(), start.column());
            case LEFT_PAREN:
                if (this.isLambdaAhead()) {
                    return this.parseLambda();
                }
                this.cursor.advance();
                final IExpr grouped = this.parseExpression();
                this.cursor.expect(TokenKind.RIGHT_PAREN);
                return grouped;
            default:
                break;
        }
        /*
         * A built-in type name can stand where a value does, as the receiver of one of its own
         * methods: "string.Format(...)" and "int.Parse(...)" read the way a player expects.
         */
        if (this.types.isBuiltIn(start.kind()) && this.cursor.kindAhead(1) == TokenKind.DOT) {
            this.cursor.advance();
            return new IExpr.Name(start.text(), start.line(), start.column());
        }
        this.diagnostics.error(start.line(), start.column(), SigmaError.EXPECTED_EXPRESSION, start.describe());
        return null;
    }

    private IExpr parseNew() {
        final Token start = this.cursor.advance();
        final TypeRef type = this.types.parseTypeRef();
        if (this.cursor.check(TokenKind.LEFT_BRACKET)) {
            this.cursor.advance();
            final IExpr length = this.parseExpression();
            this.cursor.expect(TokenKind.RIGHT_BRACKET);
            return new IExpr.NewArray(type, length, start.line(), start.column());
        }
        final List<IExpr> arguments = this.parseArguments();
        return new IExpr.New(type, arguments, start.line(), start.column());
    }

    private IExpr parseShorthandLambda() {
        final Token name = this.cursor.advance();
        this.cursor.advance();
        final List<IDecl.Parameter> parameters = List.of(
                new IDecl.Parameter(false, null, name.text(), name.line(), name.column()));
        return this.finishLambda(parameters, name);
    }

    private IExpr parseLambda() {
        final Token start = this.cursor.peek();
        final List<IDecl.Parameter> parameters = new ArrayList<>();
        this.cursor.advance();
        while (!this.cursor.check(TokenKind.RIGHT_PAREN) && !this.cursor.atEnd()) {
            final int before = this.cursor.at();
            final Token at = this.cursor.peek();
            final boolean outward = this.cursor.match(TokenKind.OUT);
            final int afterType = this.types.scanType(this.cursor.at());
            TypeRef type = null;
            if (afterType > this.cursor.at() && this.cursor.kindAt(afterType) == TokenKind.IDENTIFIER) {
                type = this.types.parseTypeRef();
            }
            final String name = this.cursor.expectIdentifier();
            parameters.add(new IDecl.Parameter(outward, type, name, at.line(), at.column()));
            if (!this.cursor.match(TokenKind.COMMA)) {
                break;
            }
            if (this.cursor.at() == before) {
                this.cursor.advance();
            }
        }
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        this.cursor.expect(TokenKind.ARROW);
        return this.finishLambda(parameters, start);
    }

    private IExpr finishLambda(final List<IDecl.Parameter> parameters, final Token start) {
        if (this.cursor.check(TokenKind.LEFT_BRACE)) {
            final IStmt.Block block = this.statements.parseBlock();
            return new IExpr.Lambda(parameters, null, block, start.line(), start.column());
        }
        final IExpr body = this.parseExpression();
        return new IExpr.Lambda(parameters, body, null, start.line(), start.column());
    }
}
