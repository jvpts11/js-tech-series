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
    /** The word before a last parameter that takes its values one by one, a word only there. */
    private static final String PARAMS = "params";
    /** How many tuples inside each other a look ahead goes into before it takes what is there for no type. */
    private static final int MAX_SCANNED_TUPLES = 32;

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

    /* Type arguments read themselves a level down, so a type nested in type arguments counts against the limit. */
    TypeRef parseTypeRef() {
        if (!this.cursor.descend()) {
            return null;
        }
        try {
            return this.typeRef();
        } finally {
            this.cursor.ascend();
        }
    }

    private TypeRef typeRef() {
        final Token start = this.cursor.peek();
        if (start.is(TokenKind.LEFT_PAREN)) {
            return this.tupleType(start);
        }
        // void is a type only as what a pointer points at: void*.
        if (start.is(TokenKind.VOID) && this.cursor.kindAhead(1) == TokenKind.STAR) {
            this.cursor.advance();
            return this.pointedAt(TypeRef.named("void", start.line(), start.column()));
        }
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
        return this.pointedAt(new TypeRef(name.toString(), arguments, 0, start.line(), start.column()));
    }

    /* A type with the stars after it, each a pointer to what came before, and then the pairs of brackets. */
    private TypeRef pointedAt(final TypeRef base) {
        TypeRef type = base;
        while (this.cursor.check(TokenKind.STAR)) {
            this.cursor.advance();
            type = TypeRef.pointer(type, 0, base.line(), base.column());
        }
        return type.withArrayRank(this.arrayRank());
    }

    /* "(int count, string name)": places each with a type and, when it is called something, a name. */
    private TypeRef tupleType(final Token start) {
        this.cursor.advance();
        final List<TypeRef> places = new ArrayList<>();
        final List<String> names = new ArrayList<>();
        do {
            final TypeRef place = this.parseTypeRef();
            if (place == null) {
                break;
            }
            places.add(place);
            names.add(this.cursor.check(TokenKind.IDENTIFIER) ? this.cursor.advance().text() : "");
        } while (this.cursor.match(TokenKind.COMMA));
        this.cursor.expect(TokenKind.RIGHT_PAREN);
        return TypeRef.tuple(places, names, this.arrayRank(), start.line(), start.column());
    }

    /* The pairs of brackets after a type, each one an array of what came before. */
    private int arrayRank() {
        int arrayRank = 0;
        while (this.cursor.check(TokenKind.LEFT_BRACKET) && this.cursor.kindAhead(1) == TokenKind.RIGHT_BRACKET) {
            this.cursor.advance();
            this.cursor.advance();
            arrayRank++;
        }
        return arrayRank;
    }

    /**
     * A parameter list in brackets, as a method, a constructor, a delegate and a local function write it. The word
     * params is a word only where a type and a name follow it, so a parameter of a type a program called params still
     * reads as one.
     */
    List<IDecl.Parameter> parseParameters() {
        return this.parseParameters(TokenKind.LEFT_PAREN, TokenKind.RIGHT_PAREN);
    }

    /** The same, between those two marks: an indexer's are in square brackets. */
    List<IDecl.Parameter> parseParameters(final TokenKind open, final TokenKind close) {
        final List<IDecl.Parameter> parameters = new ArrayList<>();
        if (!this.cursor.expect(open)) {
            return parameters;
        }
        while (!this.cursor.check(close) && !this.cursor.atEnd()) {
            final int before = this.cursor.at();
            final Token start = this.cursor.peek();
            final boolean many = this.paramsAhead();
            if (many) {
                this.cursor.advance();
            }
            // An extension method's first parameter is the value it is called on, written this, as C# writes it.
            final boolean receiver = this.cursor.match(TokenKind.THIS);
            final boolean outward = this.cursor.match(TokenKind.OUT);
            final TypeRef type = this.parseTypeRef();
            if (type == null) {
                break;
            }
            final String name = this.cursor.expectIdentifier();
            parameters.add(new IDecl.Parameter(outward, type, name, many, receiver, start.line(), start.column()));
            if (!this.cursor.match(TokenKind.COMMA)) {
                break;
            }
            if (this.cursor.at() == before) {
                this.cursor.advance();
            }
        }
        this.cursor.expect(close);
        return parameters;
    }

    /* Whether the word params starts the parameter here: a type and a name follow it. */
    private boolean paramsAhead() {
        if (!this.cursor.check(TokenKind.IDENTIFIER) || !PARAMS.equals(this.cursor.peek().text())) {
            return false;
        }
        final int after = this.scanType(this.cursor.at() + 1);
        return after > 0 && this.cursor.kindAt(after) == TokenKind.IDENTIFIER;
    }

    /** Whether a type is written at the cursor and a name follows it: what a declaration or a typed parameter is. */
    boolean typedNameAhead() {
        final int after = this.scanType(this.cursor.at());
        // A word kept for a later version where the name goes is still a declaration, refused at that word.
        return after > this.cursor.at() && (this.cursor.kindAt(after) == TokenKind.IDENTIFIER
                || LanguageGaps.reserved(this.cursor.kindAt(after)));
    }

    /*
     * Looks past a type without reporting anything, and answers where it ends, or -1 if what is
     * there is not a type at all. Used only to tell a declaration from an expression.
     */
    int scanType(final int from) {
        return this.scanType(from, 0);
    }

    /* The same, inside that many tuples, which are looked into only so deep: a guess never runs the stack out. */
    private int scanType(final int from, final int tuples) {
        int at = from;
        if (this.cursor.kindAt(at) == TokenKind.LEFT_PAREN) {
            at = tuples < MAX_SCANNED_TUPLES ? this.scanTuple(at, tuples + 1) : -1;
            while (at > 0 && this.cursor.kindAt(at) == TokenKind.LEFT_BRACKET
                    && this.cursor.kindAt(at + 1) == TokenKind.RIGHT_BRACKET) {
                at += 2;
            }
            return at;
        }
        if (this.cursor.kindAt(at) == TokenKind.VOID && this.cursor.kindAt(at + 1) == TokenKind.STAR) {
            return this.pastStarsAndBrackets(at + 1);
        }
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
                } else if (kind == TokenKind.DOT) {
                    // A type argument may be named with its namespace or the type it is nested in: Game.Item.
                    if (this.cursor.kindAt(at + 1) != TokenKind.IDENTIFIER) {
                        return -1;
                    }
                } else if (kind != TokenKind.COMMA && kind != TokenKind.LEFT_BRACKET
                        && kind != TokenKind.RIGHT_BRACKET && kind != TokenKind.LEFT_PAREN
                        && kind != TokenKind.RIGHT_PAREN && kind != TokenKind.STAR && !this.isTypeStart(kind)) {
                    return -1;
                }
                at++;
                if (depth < 0) {
                    return -1;
                }
            }
        }
        return this.pastStarsAndBrackets(at);
    }

    /* Where a type ends past the stars of its pointers and the pairs of brackets of its arrays. */
    private int pastStarsAndBrackets(final int from) {
        int at = from;
        while (this.cursor.kindAt(at) == TokenKind.STAR) {
            at++;
        }
        while (this.cursor.kindAt(at) == TokenKind.LEFT_BRACKET
                && this.cursor.kindAt(at + 1) == TokenKind.RIGHT_BRACKET) {
            at += 2;
        }
        return at;
    }

    /* Looks past "(type name, type name)", two places or more, or answers -1 when what is there is no tuple's type. */
    private int scanTuple(final int from, final int depth) {
        int at = from + 1;
        int places = 0;
        while (true) {
            final int after = this.scanType(at, depth);
            if (after < 0) {
                return -1;
            }
            at = after;
            places++;
            if (this.cursor.kindAt(at) == TokenKind.IDENTIFIER) {
                at++;
            }
            if (this.cursor.kindAt(at) != TokenKind.COMMA) {
                return this.cursor.kindAt(at) == TokenKind.RIGHT_PAREN && places > 1 ? at + 1 : -1;
            }
            at++;
        }
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
