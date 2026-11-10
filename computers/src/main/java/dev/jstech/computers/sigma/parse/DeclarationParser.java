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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
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

    /** Each word that can stand before a declaration, with the modifier it stands for. */
    private static final Map<TokenKind, IDecl.Modifier> MODIFIERS = new EnumMap<>(Map.of(
            TokenKind.PUBLIC, IDecl.Modifier.PUBLIC,
            TokenKind.PRIVATE, IDecl.Modifier.PRIVATE,
            TokenKind.PROTECTED, IDecl.Modifier.PROTECTED,
            TokenKind.INTERNAL, IDecl.Modifier.INTERNAL,
            TokenKind.STATIC, IDecl.Modifier.STATIC,
            TokenKind.READONLY, IDecl.Modifier.READONLY,
            TokenKind.VIRTUAL, IDecl.Modifier.VIRTUAL,
            TokenKind.OVERRIDE, IDecl.Modifier.OVERRIDE,
            TokenKind.ABSTRACT, IDecl.Modifier.ABSTRACT,
            TokenKind.CONST, IDecl.Modifier.CONST));

    /** The modifiers that came in a later version, which are words only where a modifier stands. */
    private static final Map<String, IDecl.Modifier> CONTEXTUAL_MODIFIERS = Map.of("sealed", IDecl.Modifier.SEALED,
            "unsafe", IDecl.Modifier.UNSAFE);

    /** The word an operator's method is declared with, a word only where an operator and a bracket follow it. */
    private static final String OPERATOR = "operator";
    /** Each operator a type may give a meaning to, by how it is written, as the operator taking two values. */
    private static final Map<TokenKind, Operator> BINARY_OPERATORS = new EnumMap<>(Map.ofEntries(
            Map.entry(TokenKind.PLUS, Operator.ADD), Map.entry(TokenKind.MINUS, Operator.SUBTRACT),
            Map.entry(TokenKind.STAR, Operator.MULTIPLY), Map.entry(TokenKind.SLASH, Operator.DIVIDE),
            Map.entry(TokenKind.PERCENT, Operator.REMAINDER), Map.entry(TokenKind.AMPERSAND, Operator.BIT_AND),
            Map.entry(TokenKind.PIPE, Operator.BIT_OR), Map.entry(TokenKind.CARET, Operator.BIT_XOR),
            Map.entry(TokenKind.SHIFT_LEFT, Operator.SHIFT_LEFT),
            Map.entry(TokenKind.SHIFT_RIGHT, Operator.SHIFT_RIGHT),
            Map.entry(TokenKind.EQUAL, Operator.EQUAL), Map.entry(TokenKind.NOT_EQUAL, Operator.NOT_EQUAL),
            Map.entry(TokenKind.LESS, Operator.LESS), Map.entry(TokenKind.LESS_EQUAL, Operator.LESS_EQUAL),
            Map.entry(TokenKind.GREATER, Operator.GREATER),
            Map.entry(TokenKind.GREATER_EQUAL, Operator.GREATER_EQUAL)));
    /** The same, for the operators taking one value. */
    private static final Map<TokenKind, Operator> UNARY_OPERATORS = new EnumMap<>(Map.of(
            TokenKind.NOT, Operator.NOT, TokenKind.MINUS, Operator.NEGATE, TokenKind.PLUS, Operator.PLUS,
            TokenKind.TILDE, Operator.COMPLEMENT, TokenKind.PLUS_PLUS, Operator.INCREMENT,
            TokenKind.MINUS_MINUS, Operator.DECREMENT));
    /** The words that say who may reach a member, which an indexer's accessor may give itself apart. */
    private static final Set<IDecl.Modifier> ACCESS = EnumSet.of(IDecl.Modifier.PUBLIC, IDecl.Modifier.PRIVATE,
            IDecl.Modifier.PROTECTED, IDecl.Modifier.INTERNAL);

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
            if (TYPE_DECLARATION_STARTS.contains(kind) || MODIFIERS.containsKey(kind)) {
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

    /**
     * The members between a class's braces, the opening one already taken; takes the closing one.
     *
     * <p>A member can be a class of its own, whose body is read by this again, so a body counts a level down like a
     * block does: a file of thousands of classes inside each other is told it nests too deeply.
     */
    private void parseClassBody(final String name, final List<IDecl.IMemberDecl> members) {
        if (!this.cursor.descend()) {
            return;
        }
        try {
            while (!this.cursor.check(TokenKind.RIGHT_BRACE) && !this.cursor.atEnd()) {
                final int before = this.cursor.at();
                final List<IDecl.IMemberDecl> read = this.parseMembers(name);
                if (!read.isEmpty()) {
                    members.addAll(read);
                } else {
                    this.skipToMember();
                }
                if (this.cursor.at() == before) {
                    this.cursor.advance();
                }
            }
            this.cursor.expect(TokenKind.RIGHT_BRACE);
        } finally {
            this.cursor.ascend();
        }
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
        final List<String> componentTypes = components.stream().map(c -> c.type().describe()).toList();
        final boolean hasConstructor = written.stream().anyMatch(member -> member instanceof IDecl.ConstructorDecl c
                && typesOf(c.parameters()).equals(componentTypes));
        if (!hasConstructor) {
            final List<IDecl.Parameter> taken = new ArrayList<>();
            for (final IDecl.Parameter component : components) {
                taken.add(new IDecl.Parameter(false, component.type(), component.name(),
                        component.line(), component.column()));
            }
            made.add(new IDecl.ConstructorDecl(EnumSet.of(IDecl.Modifier.PUBLIC), name, taken, null,
                    new IStmt.Block(stores, line, column), line, column));
        }
        if (!writesMethod(written, "ToString", List.of())) {
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
        if (!writesMethod(written, "Equals", List.of(TypeRef.named(name, line, column).describe()))) {
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

    /** The written form of each parameter's type, in order, which is what tells two overloads apart. */
    private static List<String> typesOf(final List<IDecl.Parameter> parameters) {
        return parameters.stream().map(parameter -> parameter.type().describe()).toList();
    }

    /** Whether the body writes that very method; an overload with other parameters leaves the made one alone. */
    private static boolean writesMethod(final List<IDecl.IMemberDecl> members, final String name,
                                        final List<String> parameterTypes) {
        for (final IDecl.IMemberDecl member : members) {
            if (member instanceof IDecl.MethodDecl method && method.name().equals(name)
                    && typesOf(method.parameters()).equals(parameterTypes)) {
                return true;
            }
        }
        return false;
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
    /* One member, or the two methods an indexer is written as; nothing when what is there is no member. */
    private List<IDecl.IMemberDecl> parseMembers(final String className) {
        final Set<IDecl.Modifier> modifiers = this.parseModifiers(className);
        final Token start = this.cursor.peek();
        final int at = this.cursor.at();
        final int afterType = this.types.scanType(at);
        if (afterType > at && this.cursor.kindAt(afterType) == TokenKind.THIS
                && this.cursor.kindAt(afterType + 1) == TokenKind.LEFT_BRACKET) {
            return this.parseIndexer(modifiers, this.types.parseTypeRef(), start);
        }
        final IDecl.IMemberDecl member = this.parseMember(className, modifiers, start);
        return member == null ? List.of() : List.of(member);
    }

    private IDecl.IMemberDecl parseMember(final String className, final Set<IDecl.Modifier> modifiers,
                                          final Token start) {
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

        if (!this.cursor.check(TokenKind.VOID) && !this.types.isTypeStart(start.kind())
                && this.types.scanType(this.cursor.at()) < 0) {
            this.diagnostics.error(start.line(), start.column(), SigmaError.EXPECTED_MEMBER, start.describe());
            return null;
        }
        final TypeRef type = this.types.parseReturnType();
        if (this.operatorAhead()) {
            return this.parseOperator(modifiers, type, start);
        }
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
        // A property written with an arrow: "string Name => "Lantern OS";", read only.
        if (this.cursor.match(TokenKind.ARROW)) {
            final IExpr value = this.expressions.parseExpression();
            this.cursor.expect(TokenKind.SEMICOLON);
            final IDecl.Accessor getter = new IDecl.Accessor(Set.of(), start.line(), start.column());
            return new IDecl.PropertyDecl(modifiers, type, name, getter, null, value, start.line(), start.column());
        }

        IExpr initializer = null;
        if (this.cursor.match(TokenKind.ASSIGN)) {
            initializer = this.expressions.parseExpression();
        }
        this.cursor.expect(TokenKind.SEMICOLON);
        return new IDecl.FieldDecl(modifiers, type, name, initializer, start.line(), start.column());
    }

    /* "operator" followed by an operator and its bracket: the word is a word only there. */
    private boolean operatorAhead() {
        final Token word = this.cursor.peek();
        final TokenKind sign = this.cursor.kindAhead(1);
        return word.is(TokenKind.IDENTIFIER) && OPERATOR.equals(word.text())
                && (BINARY_OPERATORS.containsKey(sign) || UNARY_OPERATORS.containsKey(sign))
                && this.cursor.kindAhead(2) == TokenKind.LEFT_PAREN;
    }

    /*
     * "public static Point operator +(Point a, Point b) => ...;": a static method named as C# names the operator, which
     * the operator written between values of the type calls. The words it was written with say it is one.
     */
    private IDecl.IMemberDecl parseOperator(final Set<IDecl.Modifier> modifiers, final TypeRef type,
                                            final Token start) {
        this.cursor.advance();
        final Token sign = this.cursor.advance();
        final List<IDecl.Parameter> parameters = this.parseParameters();
        final Operator operator = parameters.size() == 1 ? UNARY_OPERATORS.get(sign.kind())
                : BINARY_OPERATORS.get(sign.kind());
        // One that takes the wrong number of values keeps its sign in its name, for the message that refuses it.
        final String name = operator == null ? OPERATOR + " " + sign.text() : operator.methodName();
        final IStmt.Block body;
        if (this.cursor.match(TokenKind.ARROW)) {
            final IExpr value = this.expressions.parseExpression();
            this.cursor.expect(TokenKind.SEMICOLON);
            body = returning(value, start);
        } else {
            body = this.cursor.check(TokenKind.LEFT_BRACE) ? this.statements.parseBlock() : null;
            if (body == null) {
                this.cursor.expect(TokenKind.LEFT_BRACE);
            }
        }
        return new IDecl.MethodDecl(withWord(modifiers, IDecl.Modifier.OPERATOR), type, name, parameters, body,
                start.line(), start.column());
    }

    /*
     * "public string this[int at] { get { ... } set { ... } }", or "=> value;" for one that is only read: the two
     * methods it comes down to, as C#'s does, one reading and one writing that is handed the value as value.
     */
    private List<IDecl.IMemberDecl> parseIndexer(final Set<IDecl.Modifier> modifiers, final TypeRef type,
                                                 final Token start) {
        this.cursor.advance();
        final List<IDecl.Parameter> keys = this.types.parseParameters(TokenKind.LEFT_BRACKET, TokenKind.RIGHT_BRACKET);
        final Set<IDecl.Modifier> words = withWord(modifiers, IDecl.Modifier.INDEXER);
        final List<IDecl.IMemberDecl> made = new ArrayList<>();
        if (this.cursor.match(TokenKind.ARROW)) {
            final IExpr value = this.expressions.parseExpression();
            this.cursor.expect(TokenKind.SEMICOLON);
            made.add(new IDecl.MethodDecl(words, type, IDecl.INDEXER_GET, keys, returning(value, start),
                    start.line(), start.column()));
            return made;
        }
        if (this.cursor.expect(TokenKind.LEFT_BRACE)) {
            while (!this.cursor.check(TokenKind.RIGHT_BRACE) && !this.cursor.atEnd()) {
                final int before = this.cursor.at();
                final Set<IDecl.Modifier> own = this.parseModifiers();
                final Token word = this.cursor.peek();
                final boolean reads = word.is(TokenKind.IDENTIFIER) && "get".equals(word.text());
                if (!reads && !(word.is(TokenKind.IDENTIFIER) && "set".equals(word.text()))) {
                    this.diagnostics.error(word.line(), word.column(), SigmaError.EXPECTED_MEMBER, word.describe());
                    break;
                }
                this.cursor.advance();
                made.add(this.accessor(reads, accessible(words, own), type, keys, word));
                if (this.cursor.at() == before) {
                    this.cursor.advance();
                }
            }
            this.cursor.expect(TokenKind.RIGHT_BRACE);
        }
        // One with neither half is refused as a reading method with no body, where it was written.
        if (made.isEmpty()) {
            made.add(new IDecl.MethodDecl(words, type, IDecl.INDEXER_GET, keys, null, start.line(), start.column()));
        }
        return made;
    }

    /*
     * One half of an indexer: "get { ... }", "get => value;", "set { ... }" or "set => what it does;". Each half has
     * keys of its own, since every later stage keeps what it learns of a parameter by the node it was written as.
     */
    private IDecl.MethodDecl accessor(final boolean reads, final Set<IDecl.Modifier> words, final TypeRef type,
                                      final List<IDecl.Parameter> written, final Token word) {
        final List<IDecl.Parameter> keys = new ArrayList<>(written.size());
        for (final IDecl.Parameter key : written) {
            keys.add(new IDecl.Parameter(key.outward(), key.type(), key.name(), key.many(), key.receiver(), key.line(),
                    key.column()));
        }
        final IStmt.Block body;
        if (this.cursor.match(TokenKind.ARROW)) {
            final IExpr value = this.expressions.parseExpression();
            this.cursor.expect(TokenKind.SEMICOLON);
            body = reads ? returning(value, word) : new IStmt.Block(
                    List.of(new IStmt.ExprStmt(value, word.line(), word.column())), word.line(), word.column());
        } else {
            body = this.statements.parseBlock();
        }
        if (reads) {
            return new IDecl.MethodDecl(words, type, IDecl.INDEXER_GET, keys, body, word.line(), word.column());
        }
        final List<IDecl.Parameter> taken = new ArrayList<>(keys);
        taken.add(new IDecl.Parameter(false, type, "value", word.line(), word.column()));
        return new IDecl.MethodDecl(words, TypeRef.named("void", word.line(), word.column()), IDecl.INDEXER_SET,
                taken, body, word.line(), word.column());
    }

    /* A body that gives back that value and does nothing else, which is what an arrow writes. */
    private static IStmt.Block returning(final IExpr value, final Token at) {
        return new IStmt.Block(List.of(new IStmt.Return(value, at.line(), at.column())), at.line(), at.column());
    }

    /* The words with one more. */
    private static Set<IDecl.Modifier> withWord(final Set<IDecl.Modifier> words, final IDecl.Modifier word) {
        final Set<IDecl.Modifier> more = EnumSet.noneOf(IDecl.Modifier.class);
        more.addAll(words);
        more.add(word);
        return more;
    }

    /* An accessor's words: the indexer's, with the accessor's own access in place of the indexer's when it has one. */
    private static Set<IDecl.Modifier> accessible(final Set<IDecl.Modifier> words, final Set<IDecl.Modifier> own) {
        final Set<IDecl.Modifier> made = EnumSet.noneOf(IDecl.Modifier.class);
        made.addAll(words);
        if (own.stream().anyMatch(ACCESS::contains)) {
            made.removeAll(ACCESS);
        }
        made.addAll(own);
        return made;
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
        return this.parseModifiers(null);
    }

    /** The modifiers before a declaration, inside the class called {@code className} when it is in one. */
    private Set<IDecl.Modifier> parseModifiers(final String className) {
        final Set<IDecl.Modifier> modifiers = EnumSet.noneOf(IDecl.Modifier.class);
        while (MODIFIERS.containsKey(this.cursor.peek().kind()) || this.contextualModifierAhead(className)) {
            final Token word = this.cursor.advance();
            final IDecl.Modifier modifier = word.is(TokenKind.IDENTIFIER) ? CONTEXTUAL_MODIFIERS.get(word.text())
                    : MODIFIERS.get(word.kind());
            if (!modifiers.add(modifier)) {
                this.diagnostics.error(word.line(), word.column(),
                        SigmaError.DUPLICATE_MODIFIER, modifier.text());
            }
        }
        return modifiers;
    }

    private List<IDecl.Parameter> parseParameters() {
        return this.types.parseParameters();
    }

    /*
     * Whether the word here is one of the modifiers that are words only where a modifier stands, which a program
     * written before them may have used as a name: another modifier or a declaration follows it, a type and a name do,
     * or the class's own name and a bracket, as a constructor's.
     */
    private boolean contextualModifierAhead(final String className) {
        final Token word = this.cursor.peek();
        if (!word.is(TokenKind.IDENTIFIER) || !CONTEXTUAL_MODIFIERS.containsKey(word.text())) {
            return false;
        }
        final Token next = this.cursor.tokenAt(this.cursor.at() + 1);
        if (MODIFIERS.containsKey(next.kind()) || TYPE_DECLARATION_STARTS.contains(next.kind())
                || next.is(TokenKind.VOID) || next.is(TokenKind.EVENT)
                || next.is(TokenKind.IDENTIFIER) && CONTEXTUAL_MODIFIERS.containsKey(next.text())) {
            return true;
        }
        if (className != null && next.is(TokenKind.IDENTIFIER) && next.text().equals(className)
                && this.cursor.kindAt(this.cursor.at() + 2) == TokenKind.LEFT_PAREN) {
            return true;
        }
        final int after = this.types.scanType(this.cursor.at() + 1);
        return after > 0 && this.cursor.kindAt(after) == TokenKind.IDENTIFIER;
    }
}
