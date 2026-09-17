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
import dev.jstech.computers.sigma.ast.CompilationUnit;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.lex.Token;
import dev.jstech.computers.sigma.lex.TokenKind;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Reads tokens into a tree.
 *
 * <p>It is a plain recursive descent parser with one token of lookahead, plus a few longer scans
 * where the grammar is genuinely ambiguous: telling a declaration from an expression, a cast from a
 * parenthesised value, and a lambda from either. Those scans only look, they never report, so a
 * guess that turns out wrong costs nothing.
 *
 * <p>It never throws. A construct it cannot read is reported once and skipped to the next safe
 * point, so one missing brace does not turn into a page of noise, and every loop is guaranteed to
 * consume at least one token per pass so a file of nonsense still terminates. The tree it returns
 * is only meaningful when nothing was reported: after an error it holds whatever could still be
 * read, which is enough to keep parsing and not enough to compile.
 *
 * <p>This is the outermost layer of the grammar, which is what a file is made of: what it brings in,
 * what namespace it is in, and the types it declares. Reading those types, what they contain, what a
 * method does and what a value is are four readers of their own, all moving the same cursor.
 */
public final class Parser {

    private final TokenCursor cursor;
    private final DiagnosticBag diagnostics;
    private final DeclarationParser declarations;
    private final ExpressionParser expressions;

    public Parser(final List<Token> tokens, final DiagnosticBag diagnostics) {
        this.cursor = new TokenCursor(tokens, diagnostics);
        this.diagnostics = diagnostics;
        final TypeParser types = new TypeParser(this.cursor, diagnostics);
        this.expressions = new ExpressionParser(this.cursor, diagnostics, types);
        this.declarations = new DeclarationParser(this.cursor, diagnostics, types,
                this.expressions.statements(), this.expressions);
    }

    /**
     * Reads the whole file. The unit holds what the file brought in with {@code using} and every type
     * the parser managed to read, each with the namespace it was declared in.
     *
     * <p>A file may open with one namespace on a line of its own, and may put namespace blocks inside
     * one another; a type is in the namespace made of all of those around it. A type in none is a
     * mistake, reported once for the file.
     */
    public CompilationUnit parse(final String file) {
        final List<CompilationUnit.Using> usings = new ArrayList<>();
        final List<CompilationUnit.Declared> declared = new ArrayList<>();
        String fileNamespace = "";
        final Deque<String> blocks = new ArrayDeque<>();
        boolean askedForNamespace = false;
        while (!this.cursor.atEnd()) {
            final int before = this.cursor.at();
            if (this.cursor.check(TokenKind.USING)) {
                final Token start = this.cursor.advance();
                final String name = this.parseDottedName();
                boolean all = false;
                if (this.cursor.check(TokenKind.DOT) && this.cursor.kindAhead(1) == TokenKind.STAR) {
                    this.cursor.advance();
                    this.cursor.advance();
                    all = true;
                }
                if (!name.isEmpty()) {
                    usings.add(new CompilationUnit.Using(name, all, start.line(), start.column()));
                }
                this.cursor.expect(TokenKind.SEMICOLON);
                if (!declared.isEmpty() || !fileNamespace.isEmpty() || !blocks.isEmpty()) {
                    this.diagnostics.error(start.line(), start.column(), SigmaError.USING_TOO_LATE);
                }
            } else if (this.cursor.check(TokenKind.NAMESPACE)) {
                final Token start = this.cursor.advance();
                final String name = this.parseDottedName();
                if (this.cursor.match(TokenKind.LEFT_BRACE)) {
                    blocks.addLast(name);
                } else {
                    this.cursor.expect(TokenKind.SEMICOLON);
                    if (!fileNamespace.isEmpty() || !blocks.isEmpty() || !declared.isEmpty()) {
                        this.diagnostics.error(start.line(), start.column(), SigmaError.ONE_NAMESPACE);
                    } else {
                        fileNamespace = name;
                    }
                }
            } else if (!blocks.isEmpty() && this.cursor.check(TokenKind.RIGHT_BRACE)) {
                this.cursor.advance();
                blocks.removeLast();
            } else {
                final Token at = this.cursor.peek();
                final IDecl.ITypeDecl type = this.declarations.parseTypeDeclaration();
                if (type != null) {
                    final String namespace = joined(fileNamespace, blocks);
                    if (namespace.isEmpty() && !askedForNamespace) {
                        this.diagnostics.error(at.line(), at.column(), SigmaError.NAMESPACE_REQUIRED);
                        askedForNamespace = true;
                    }
                    declared.add(new CompilationUnit.Declared(namespace, type));
                } else {
                    this.declarations.skipToTypeDeclaration();
                }
            }
            if (this.cursor.at() == before) {
                this.cursor.advance();
            }
        }
        if (!blocks.isEmpty()) {
            final Token end = this.cursor.peek();
            this.diagnostics.error(end.line(), end.column(), SigmaError.EXPECTED_TOKEN, "}", end.describe());
        }
        return new CompilationUnit(file, usings, declared);
    }

    /** Reads one expression standing on its own, as a hole in an interpolated string holds one. */
    IExpr parseLoneExpression() {
        return this.expressions.parseLoneExpression();
    }

    /** The namespace a type is in: the file's, then every block open around it, joined with dots. */
    private static String joined(final String fileNamespace, final Deque<String> blocks) {
        final StringBuilder out = new StringBuilder(fileNamespace);
        for (final String block : blocks) {
            if (block.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append('.');
            }
            out.append(block);
        }
        return out.toString();
    }

    /** A name with dots in it, the way a namespace is written; empty, with a complaint, when none is there. */
    private String parseDottedName() {
        final StringBuilder name = new StringBuilder();
        final Token first = this.cursor.peek();
        if (first.kind() != TokenKind.IDENTIFIER) {
            this.diagnostics.error(first.line(), first.column(), SigmaError.EXPECTED_TOKEN, "a name", first.describe());
            return "";
        }
        name.append(this.cursor.advance().text());
        while (this.cursor.check(TokenKind.DOT) && this.cursor.kindAhead(1) == TokenKind.IDENTIFIER) {
            this.cursor.advance();
            name.append('.').append(this.cursor.advance().text());
        }
        return name.toString();
    }
}
