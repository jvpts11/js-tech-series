/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.IStmt;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything checking a body has to hand, and where the checking has got to.
 *
 * <p>The fixed half is what the earlier stages built: the types the program declares, the rules those
 * types obey, and the model the answers are written into. The moving half is what is true only at this
 * point of this body: which type it belongs to, what it answers with, the names in scope, whether it is
 * inside a loop or a choice, and whether a mistake here should be spoken about at all.
 *
 * <p>Every checker holds one of these and there is one per pass, which is what lets a statement, a value,
 * a member and a call be separate things to check while still agreeing on where they are.
 */
final class BodyScope {

    private final BuiltIns builtIns;
    private final TypeRules rules;
    private final Declarations declarations;
    private final SemanticModel model;
    private final DiagnosticBag diagnostics;
    /** Whether a parameter the method fills in is given a value by every way out of it. */
    private final DefiniteAssignment assignment = new DefiniteAssignment(this::report);

    private NamedType currentType;
    private ITypeSymbol returnType = ITypeSymbol.Primitive.VOID;
    private Scope scope = new Scope(null);
    private boolean staticContext;
    private boolean inConstructor;
    private int loopDepth;
    private int switchDepth;
    private int quiet;

    BodyScope(final BuiltIns builtIns, final TypeRules rules, final Declarations declarations,
              final SemanticModel model, final DiagnosticBag diagnostics) {
        this.builtIns = builtIns;
        this.rules = rules;
        this.declarations = declarations;
        this.model = model;
        this.diagnostics = diagnostics;
    }

    /** How a member was reached, which is what decides whether static and instance line up. */
    enum Access {
        /** Through the name of a type, as in a static call. */
        TYPE,
        /** Through a value. */
        INSTANCE,
        /** By a bare name inside the type that declares it. */
        IMPLICIT
    }

    BuiltIns builtIns() {
        return this.builtIns;
    }

    TypeRules rules() {
        return this.rules;
    }

    Declarations declarations() {
        return this.declarations;
    }

    SemanticModel model() {
        return this.model;
    }

    /** Says which file whatever comes next belongs to, for the messages and for the answers alike. */
    void file(final String name) {
        this.diagnostics.setFile(name);
        this.model.setFile(name);
    }

    NamedType currentType() {
        return this.currentType;
    }

    void currentType(final NamedType type) {
        this.currentType = type;
    }

    ITypeSymbol returnType() {
        return this.returnType;
    }

    void returnType(final ITypeSymbol type) {
        this.returnType = type;
    }

    Scope scope() {
        return this.scope;
    }

    void scope(final Scope replacement) {
        this.scope = replacement;
    }

    boolean staticContext() {
        return this.staticContext;
    }

    boolean inConstructor() {
        return this.inConstructor;
    }

    int loopDepth() {
        return this.loopDepth;
    }

    void enterLoop() {
        this.loopDepth++;
    }

    void leaveLoop() {
        this.loopDepth--;
    }

    int switchDepth() {
        return this.switchDepth;
    }

    void enterChoice() {
        this.switchDepth++;
    }

    void leaveChoice() {
        this.switchDepth--;
    }

    /** Starts on a fresh body: nothing in scope, and everything about where it is set from what it is. */
    void begin(final boolean isStatic, final ITypeSymbol returns, final boolean constructor) {
        this.scope = new Scope(null);
        this.staticContext = isStatic;
        this.returnType = returns;
        this.inConstructor = constructor;
        this.loopDepth = 0;
        this.switchDepth = 0;
    }

    /*
     * Every message goes through here so a look-ahead can be taken back. Working out whether an
     * argument is a method being handed over means resolving it, and resolving it must not complain
     * about what the real pass is about to do properly.
     */
    void report(final int line, final int column, final SigmaError error, final Object... arguments) {
        if (this.quiet == 0) {
            this.diagnostics.error(line, column, error, arguments);
        }
    }

    /** Stops anything being reported until {@link #speakAgain}, for a look-ahead that may be thrown away. */
    void hush() {
        this.quiet++;
    }

    void speakAgain() {
        this.quiet--;
    }

    /*
     * An outward parameter has to be given a value on every way out of the method, because the caller
     * is promised one. This walks the body carrying whether it has been given yet, and says so at the
     * first way out that has not.
     */
    void checkOutParameters(final List<IDecl.Parameter> parameters, final IStmt.Block body, final INode at) {
        for (final IDecl.Parameter parameter : parameters) {
            if (!parameter.outward()) {
                continue;
            }
            if (body == null || !this.assignment.assignedBy(body, parameter.name(), false)) {
                this.report(at.line(), at.column(), SigmaError.OUT_NOT_ASSIGNED, parameter.name());
            }
        }
    }

    void expect(final ITypeSymbol given, final ITypeSymbol wanted, final INode at) {
        if (!this.rules.isAssignable(given, wanted)) {
            this.report(at.line(), at.column(),
                    SigmaError.CANNOT_CONVERT, given.describe(), wanted.describe());
        }
    }

    /*
     * The nearest declaration wins. A class that writes a method its interface also declares would
     * otherwise offer the same method twice and every call of it would look ambiguous.
     */
    static List<IMemberSymbol> lookup(final NamedType type, final String name) {
        final List<IMemberSymbol> found = new ArrayList<>();
        for (final IMemberSymbol member : type.allMembers()) {
            if (!member.name().equals(name) || member instanceof IMemberSymbol.ConstructorSymbol) {
                continue;
            }
            if (found.isEmpty()) {
                found.add(member);
                continue;
            }
            if (!(found.getFirst() instanceof IMemberSymbol.MethodSymbol)
                    || !(member instanceof IMemberSymbol.MethodSymbol method)) {
                continue;
            }
            boolean alreadyThere = false;
            for (final IMemberSymbol seen : found) {
                alreadyThere = alreadyThere
                        || sameSignature((IMemberSymbol.MethodSymbol) seen, method);
            }
            if (!alreadyThere) {
                found.add(member);
            }
        }
        return found;
    }

    private static boolean sameSignature(final IMemberSymbol.MethodSymbol left,
                                         final IMemberSymbol.MethodSymbol right) {
        if (left.parameters().size() != right.parameters().size()) {
            return false;
        }
        for (int i = 0; i < left.parameters().size(); i++) {
            if (!left.parameters().get(i).type().equals(right.parameters().get(i).type())) {
                return false;
            }
        }
        return true;
    }
}
