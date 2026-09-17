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
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.ast.TypeRef;
import dev.jstech.computers.sigma.lex.Token;
import dev.jstech.computers.sigma.lex.TokenKind;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reads what a program declares: its types, and inside each of them its members.
 *
 * <p>Modifiers come first wherever something is declared, so they are read before anything knows what
 * is being declared; after them a single word usually says which kind it is. A member is the exception,
 * since a field, a property and a method all begin with a type and a name and only differ in what comes
 * after them.
 *
 * <p>Getting back on track after a mistake belongs here as well, because the places worth getting back
 * to are the start of a declaration and the start of a member.
 */
final class DeclarationParser {

    private final TokenCursor cursor;
    private final DiagnosticBag diagnostics;
    private final TypeParser types;
    private final StatementParser statements;
    private final ExpressionParser expressions;

    private static final Set<TokenKind> MODIFIERS = EnumSet.of(
            TokenKind.PUBLIC, TokenKind.PRIVATE, TokenKind.PROTECTED, TokenKind.STATIC, TokenKind.READONLY,
            TokenKind.VIRTUAL, TokenKind.OVERRIDE, TokenKind.ABSTRACT);

    private static final Set<TokenKind> TYPE_DECLARATION_STARTS = EnumSet.of(
            TokenKind.CLASS, TokenKind.STRUCT, TokenKind.RECORD, TokenKind.INTERFACE, TokenKind.ENUM,
            TokenKind.DELEGATE);

    DeclarationParser(final TokenCursor cursor, final DiagnosticBag diagnostics, final TypeParser types,
                      final StatementParser statements, final ExpressionParser expressions) {
        this.cursor = cursor;
        this.diagnostics = diagnostics;
        this.types = types;
        this.statements = statements;
        this.expressions = expressions;
    }

    IDecl.ITypeDecl parseTypeDeclaration() {
        return this.parseTypeDeclaration(this.parseModifiers());
    }

    /*
     * After a mistake, run to the next place a declaration can plausibly start, so the rest of the
     * file is still read and the player sees every other mistake in one go.
     */
    void skipToTypeDeclaration() {
        while (!this.cursor.atEnd()) {
            final TokenKind kind = this.cursor.peek().kind();
            if (TYPE_DECLARATION_STARTS.contains(kind) || MODIFIERS.contains(kind)) {
                return;
            }
            this.cursor.advance();
        }
    }

    private void skipToMember() {
        int depth = 0;
        while (!this.cursor.atEnd()) {
            final TokenKind kind = this.cursor.peek().kind();
            if (kind == TokenKind.LEFT_BRACE) {
                depth++;
            } else if (kind == TokenKind.RIGHT_BRACE) {
                if (depth == 0) {
                    return;
                }
                depth--;
            } else if (kind == TokenKind.SEMICOLON && depth == 0) {
                this.cursor.advance();
                return;
            }
            this.cursor.advance();
        }
    }

    /** A type declaration whose modifiers have been read already, at the top of a file or inside a type. */
    private IDecl.ITypeDecl parseTypeDeclaration(final Set<IDecl.Modifier> modifiers) {
        final Token start = this.cursor.peek();
        if (this.cursor.match(TokenKind.CLASS)) {
            return this.parseClass(modifiers, start, IDecl.ClassDecl.Flavour.CLASS);
        }
        if (this.cursor.match(TokenKind.STRUCT)) {
            return this.parseClass(modifiers, start, IDecl.ClassDecl.Flavour.STRUCT);
        }
        if (this.cursor.match(TokenKind.RECORD)) {
            return this.parseRecord(modifiers, start);
        }
        if (this.cursor.match(TokenKind.INTERFACE)) {
            return this.parseInterface(modifiers, start);
        }
        if (this.cursor.match(TokenKind.ENUM)) {
            return this.parseEnum(modifiers, start);
        }
        if (this.cursor.match(TokenKind.DELEGATE)) {
            return this.parseDelegate(modifiers, start);
        }
        this.diagnostics.error(start.line(), start.column(),
                SigmaError.EXPECTED_TYPE_DECLARATION, start.describe());
        return null;
    }

    private IDecl.ITypeDecl parseClass(final Set<IDecl.Modifier> modifiers, final Token start,
                                       final IDecl.ClassDecl.Flavour flavour) {
        final String name = this.cursor.expectIdentifier();
        final List<TypeRef> bases = this.parseBaseList();
        final List<IDecl.IMemberDecl> members = new ArrayList<>();
        if (this.cursor.expect(TokenKind.LEFT_BRACE)) {
            this.parseClassBody(name, members);
        }
        return new IDecl.ClassDecl(modifiers, name, bases, members, flavour, start.line(), start.column());
    }

    /** The members between a class's braces, the opening one already taken; takes the closing one. */
    private void parseClassBody(final String name, final List<IDecl.IMemberDecl> members) {
        while (!this.cursor.check(TokenKind.RIGHT_BRACE) && !this.cursor.atEnd()) {
            final int before = this.cursor.at();
            final IDecl.IMemberDecl member = this.parseMember(name);
            if (member != null) {
                members.add(member);
            } else {
                this.skipToMember();
            }
            if (this.cursor.at() == before) {
                this.cursor.advance();
            }
        }
        this.cursor.expect(TokenKind.RIGHT_BRACE);
    }

    /**
     * A record: {@code record Point(int X, int Y);}, or the same with a body of further members.
     *
     * <p>The components are written out here as what they mean, so that nothing after the parser has
     * to know a record from a class: each is a public readonly field, all of them are taken by one
     * constructor, ToString prints them by name, and Equals compares them one by one. A member the
     * body writes under one of those names is kept instead of the one that would have been made.
     */
    private IDecl.ITypeDecl parseRecord(final Set<IDecl.Modifier> modifiers, final Token start) {
        final String name = this.cursor.expectIdentifier();
        final List<IDecl.Parameter> components = this.cursor.check(TokenKind.LEFT_PAREN)
                ? this.parseParameters() : List.of();
        final List<TypeRef> bases = this.parseBaseList();
        final List<IDecl.IMemberDecl> written = new ArrayList<>();
        if (this.cursor.match(TokenKind.LEFT_BRACE)) {
            this.parseClassBody(name, written);
        } else {
            this.cursor.expect(TokenKind.SEMICOLON);
        }
        final List<IDecl.IMemberDecl> members = new ArrayList<>(recordMembers(name, components, written, start));
        members.addAll(written);
        return new IDecl.ClassDecl(modifiers, name, bases, members, IDecl.ClassDecl.Flavour.RECORD,
                start.line(), start.column());
    }

    private static List<IDecl.IMemberDecl> recordMembers(final String name, final List<IDecl.Parameter> components,
                                                         final List<IDecl.IMemberDecl> written, final Token start) {
        final int line = start.line();
        final int column = start.column();
        final List<IDecl.IMemberDecl> made = new ArrayList<>();
        final List<IStmt> stores = new ArrayList<>();
        for (final IDecl.Parameter component : components) {
            if (!declares(written, IDecl.FieldDecl.class, component.name())) {
                made.add(new IDecl.FieldDecl(EnumSet.of(IDecl.Modifier.PUBLIC, IDecl.Modifier.READONLY),
                        component.type(), component.name(), null, component.line(), component.column()));
            }
            stores.add(new IStmt.ExprStmt(new IExpr.Assign(
                    new IExpr.Member(new IExpr.This(line, column), component.name(), line, column),
                    Operator.ASSIGN, new IExpr.Name(component.name(), line, column), line, column), line, column));
        }
        final boolean hasConstructor = written.stream().anyMatch(member -> member instanceof IDecl.ConstructorDecl c
                && c.parameters().size() == components.size());
        if (!hasConstructor) {
            final List<IDecl.Parameter> taken = new ArrayList<>();
            for (final IDecl.Parameter component : components) {
                taken.add(new IDecl.Parameter(false, component.type(), component.name(),
                        component.line(), component.column()));
            }
            made.add(new IDecl.ConstructorDecl(EnumSet.of(IDecl.Modifier.PUBLIC), name, taken, null,
                    new IStmt.Block(stores, line, column), line, column));
        }
        if (!declares(written, IDecl.MethodDecl.class, "ToString")) {
            // Name { X = 1, Y = 2 }, or Name { } with nothing to show.
            IExpr text = new IExpr.Literal(TokenKind.STRING_LITERAL, name + " {", line, column);
            for (int i = 0; i < components.size(); i++) {
                final IDecl.Parameter component = components.get(i);
                text = plus(text, new IExpr.Literal(TokenKind.STRING_LITERAL,
                        (i > 0 ? ", " : " ") + component.name() + " = ", line, column), line, column);
                text = plus(text, new IExpr.Name(component.name(), line, column), line, column);
            }
            text = plus(text, new IExpr.Literal(TokenKind.STRING_LITERAL, " }", line, column), line, column);
            made.add(new IDecl.MethodDecl(EnumSet.of(IDecl.Modifier.PUBLIC), TypeRef.named("string", line, column),
                    "ToString", List.of(), new IStmt.Block(List.of(new IStmt.Return(text, line, column)), line, column),
                    line, column));
        }
        if (!declares(written, IDecl.MethodDecl.class, "Equals")) {
            // other != null && X == other.X && Y == other.Y
            IExpr same = new IExpr.Binary(Operator.NOT_EQUAL, new IExpr.Name("other", line, column),
                    new IExpr.Literal(TokenKind.NULL, null, line, column), line, column);
            for (final IDecl.Parameter component : components) {
                same = new IExpr.Binary(Operator.AND, same, new IExpr.Binary(Operator.EQUAL,
                        new IExpr.Name(component.name(), line, column),
                        new IExpr.Member(new IExpr.Name("other", line, column), component.name(), line, column),
                        line, column), line, column);
            }
            final IDecl.Parameter other = new IDecl.Parameter(false, TypeRef.named(name, line, column),
                    "other", line, column);
            made.add(new IDecl.MethodDecl(EnumSet.of(IDecl.Modifier.PUBLIC), TypeRef.named("bool", line, column),
                    "Equals", List.of(other),
                    new IStmt.Block(List.of(new IStmt.Return(same, line, column)), line, column), line, column));
        }
        return made;
    }

    private static IExpr plus(final IExpr left, final IExpr right, final int line, final int column) {
        return new IExpr.Binary(Operator.ADD, left, right, line, column);
    }

    private static boolean declares(final List<IDecl.IMemberDecl> members, final Class<? extends IDecl> kind,
                                    final String name) {
        for (final IDecl.IMemberDecl member : members) {
            if (kind.isInstance(member) && member.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private IDecl.ITypeDecl parseInterface(final Set<IDecl.Modifier> modifiers, final Token start) {
        final String name = this.cursor.expectIdentifier();
        final List<TypeRef> bases = this.parseBaseList();
        final List<IDecl.MethodDecl> methods = new ArrayList<>();
        if (this.cursor.expect(TokenKind.LEFT_BRACE)) {
            while (!this.cursor.check(TokenKind.RIGHT_BRACE) && !this.cursor.atEnd()) {
                final int before = this.cursor.at();
                final IDecl.MethodDecl method = this.parseInterfaceMethod();
                if (method != null) {
                    methods.add(method);
                } else {
                    this.skipToMember();
                }
                if (this.cursor.at() == before) {
                    this.cursor.advance();
                }
            }
            this.cursor.expect(TokenKind.RIGHT_BRACE);
        }
        return new IDecl.InterfaceDecl(modifiers, name, bases, methods, start.line(), start.column());
    }

    private IDecl.MethodDecl parseInterfaceMethod() {
        final Set<IDecl.Modifier> modifiers = this.parseModifiers();
        final Token start = this.cursor.peek();
        final TypeRef returnType = this.types.parseReturnType();
        if (returnType == null) {
            return null;
        }
        final String name = this.cursor.expectIdentifier();
        final List<IDecl.Parameter> parameters = this.parseParameters();
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IDecl.MethodDecl(modifiers, returnType, name, parameters, null, start.line(), start.column());
    }

    private IDecl.ITypeDecl parseEnum(final Set<IDecl.Modifier> modifiers, final Token start) {
        final String name = this.cursor.expectIdentifier();
        final List<IDecl.EnumConstant> constants = new ArrayList<>();
        if (this.cursor.expect(TokenKind.LEFT_BRACE)) {
            while (!this.cursor.check(TokenKind.RIGHT_BRACE) && !this.cursor.atEnd()) {
                final int before = this.cursor.at();
                final Token constantStart = this.cursor.peek();
                final String constantName = this.cursor.expectIdentifier();
                IExpr value = null;
                if (this.cursor.match(TokenKind.ASSIGN)) {
                    value = this.expressions.parseExpression();
                }
                constants.add(new IDecl.EnumConstant(constantName, value,
                        constantStart.line(), constantStart.column()));
                if (!this.cursor.match(TokenKind.COMMA)) {
                    break;
                }
                if (this.cursor.at() == before) {
                    this.cursor.advance();
                }
            }
            this.cursor.expect(TokenKind.RIGHT_BRACE);
        }
        return new IDecl.EnumDecl(modifiers, name, constants, start.line(), start.column());
    }

    private IDecl.ITypeDecl parseDelegate(final Set<IDecl.Modifier> modifiers, final Token start) {
        final TypeRef returnType = this.types.parseReturnType();
        final String name = this.cursor.expectIdentifier();
        final List<IDecl.Parameter> parameters = this.parseParameters();
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IDecl.DelegateDecl(modifiers, returnType, name, parameters, start.line(), start.column());
    }

    private List<TypeRef> parseBaseList() {
        final List<TypeRef> bases = new ArrayList<>();
        if (this.cursor.match(TokenKind.COLON)) {
            do {
                final TypeRef base = this.types.parseTypeRef();
                if (base == null) {
                    break;
                }
                bases.add(base);
            } while (this.cursor.match(TokenKind.COMMA));
        }
        return bases;
    }

    /*
     * A member starts with its modifiers, then a shape that says what it is: the class's own name
     * before a parenthesis is a constructor, "event" is an event, and otherwise a type and a name
     * are followed by parentheses for a method, a brace for a property, or neither for a field.
     */
    private IDecl.IMemberDecl parseMember(final String className) {
        final Set<IDecl.Modifier> modifiers = this.parseModifiers();
        final Token start = this.cursor.peek();

        // A type inside a type: named through the one around it, and read exactly as one at the top is.
        if (TYPE_DECLARATION_STARTS.contains(start.kind())) {
            final IDecl.ITypeDecl nested = this.parseTypeDeclaration(modifiers);
            return nested == null ? null : new IDecl.TypeMember(nested, start.line(), start.column());
        }

        if (this.cursor.match(TokenKind.EVENT)) {
            final TypeRef type = this.types.parseTypeRef();
            if (type == null) {
                return null;
            }
            final String name = this.cursor.expectIdentifier();
            this.cursor.expect(TokenKind.SEMICOLON);
            return new IDecl.EventDecl(modifiers, type, name, start.line(), start.column());
        }

        if (this.cursor.check(TokenKind.IDENTIFIER) && start.text().equals(className)
                && this.cursor.kindAhead(1) == TokenKind.LEFT_PAREN) {
            return this.parseConstructor(modifiers, start, className);
        }

        if (!this.cursor.check(TokenKind.VOID) && !this.types.isTypeStart(start.kind())) {
            this.diagnostics.error(start.line(), start.column(), SigmaError.EXPECTED_MEMBER, start.describe());
            return null;
        }
        final TypeRef type = this.types.parseReturnType();
        final String name = this.cursor.expectIdentifier();

        if (this.cursor.check(TokenKind.LEFT_PAREN)) {
            final List<IDecl.Parameter> parameters = this.parseParameters();
            IStmt.Block body = null;
            if (this.cursor.check(TokenKind.LEFT_BRACE)) {
                body = this.statements.parseBlock();
            } else {
                this.cursor.expect(TokenKind.SEMICOLON);
            }
            return new IDecl.MethodDecl(modifiers, type, name, parameters, body, start.line(), start.column());
        }

        if (this.cursor.check(TokenKind.LEFT_BRACE)) {
            return this.parseProperty(modifiers, type, name, start);
        }

        IExpr initializer = null;
        if (this.cursor.match(TokenKind.ASSIGN)) {
            initializer = this.expressions.parseExpression();
        }
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IDecl.FieldDecl(modifiers, type, name, initializer, start.line(), start.column());
    }

    private IDecl.IMemberDecl parseConstructor(final Set<IDecl.Modifier> modifiers, final Token start,
                                              final String className) {
        this.cursor.advance();
        final List<IDecl.Parameter> parameters = this.parseParameters();
        IDecl.ConstructorCall chained = null;
        if (this.cursor.match(TokenKind.COLON)) {
            final Token chainStart = this.cursor.peek();
            final boolean base = this.cursor.check(TokenKind.BASE);
            if (base || this.cursor.check(TokenKind.THIS)) {
                this.cursor.advance();
            } else {
                this.diagnostics.error(chainStart.line(), chainStart.column(),
                        SigmaError.EXPECTED_TOKEN, TokenKind.BASE.describe(), chainStart.describe());
            }
            final List<IExpr> arguments = this.expressions.parseArguments();
            chained = new IDecl.ConstructorCall(base, arguments, chainStart.line(), chainStart.column());
        }
        final IStmt.Block body = this.cursor.check(TokenKind.LEFT_BRACE) ? this.statements.parseBlock() : null;
        if (body == null) {
            this.cursor.expect(TokenKind.SEMICOLON);
        }
        return new IDecl.ConstructorDecl(modifiers, className, parameters, chained, body,
                start.line(), start.column());
    }

    /*
     * The short form only: "{ get; private set; }". A body on an accessor is a v2 feature, so a
     * brace where the semicolon belongs is reported as the missing semicolon it is.
     */
    private IDecl.IMemberDecl parseProperty(final Set<IDecl.Modifier> modifiers, final TypeRef type,
                                            final String name, final Token start) {
        this.cursor.advance();
        IDecl.Accessor getter = null;
        IDecl.Accessor setter = null;
        while (!this.cursor.check(TokenKind.RIGHT_BRACE) && !this.cursor.atEnd()) {
            final int before = this.cursor.at();
            final Set<IDecl.Modifier> accessorModifiers = this.parseModifiers();
            final Token word = this.cursor.peek();
            if (this.cursor.check(TokenKind.IDENTIFIER) && "get".equals(word.text())) {
                this.cursor.advance();
                getter = new IDecl.Accessor(accessorModifiers, word.line(), word.column());
            } else if (this.cursor.check(TokenKind.IDENTIFIER) && "set".equals(word.text())) {
                this.cursor.advance();
                setter = new IDecl.Accessor(accessorModifiers, word.line(), word.column());
            } else {
                this.diagnostics.error(word.line(), word.column(), SigmaError.EXPECTED_MEMBER, word.describe());
                break;
            }
            this.cursor.expect(TokenKind.SEMICOLON);
            if (this.cursor.at() == before) {
                this.cursor.advance();
            }
        }
        this.cursor.expect(TokenKind.RIGHT_BRACE);
        return new IDecl.PropertyDecl(modifiers, type, name, getter, setter, start.line(), start.column());
    }

    private Set<IDecl.Modifier> parseModifiers() {
        final Set<IDecl.Modifier> modifiers = EnumSet.noneOf(IDecl.Modifier.class);
        while (MODIFIERS.contains(this.cursor.peek().kind())) {
            final Token word = this.cursor.advance();
            final IDecl.Modifier modifier = switch (word.kind()) {
                case PUBLIC -> IDecl.Modifier.PUBLIC;
                case PRIVATE -> IDecl.Modifier.PRIVATE;
                case PROTECTED -> IDecl.Modifier.PROTECTED;
                case STATIC -> IDecl.Modifier.STATIC;
                case VIRTUAL -> IDecl.Modifier.VIRTUAL;
                case OVERRIDE -> IDecl.Modifier.OVERRIDE;
                case ABSTRACT -> IDecl.Modifier.ABSTRACT;
                default -> IDecl.Modifier.READONLY;
            };
            if (!modifiers.add(modifier)) {
                this.diagnostics.error(word.line(), word.column(),
                        SigmaError.DUPLICATE_MODIFIER, modifier.text());
            }
        }
        return modifiers;
    }

    private List<IDecl.Parameter> parseParameters() {
        final List<IDecl.Parameter> parameters = new ArrayList<>();
        if (!this.cursor.expect(TokenKind.LEFT_PAREN)) {
            return parameters;
        }
        while (!this.cursor.check(TokenKind.RIGHT_PAREN) && !this.cursor.atEnd()) {
            final int before = this.cursor.at();
            final Token start = this.cursor.peek();
            final boolean outward = this.cursor.match(TokenKind.OUT);
            final TypeRef type = this.types.parseTypeRef();
            if (type == null) {
                break;
            }
            final String name = this.cursor.expectIdentifier();
            parameters.add(new IDecl.Parameter(outward, type, name, start.line(), start.column()));
            if (!this.cursor.match(TokenKind.COMMA)) {
                break;
            }
            if (this.cursor.at() == before) {
                this.cursor.advance();
            }
        }
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        return parameters;
    }
}
