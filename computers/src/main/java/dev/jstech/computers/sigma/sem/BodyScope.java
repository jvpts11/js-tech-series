/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.IStmt;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    /** The values of the program's consts, written into every place that reads one. */
    private final Constants constants;

    private NamedType currentType;
    private ITypeSymbol returnType = ITypeSymbol.Primitive.VOID;
    private Scope scope = new Scope(null);
    private boolean staticContext;
    private boolean inConstructor;
    private int loopDepth;
    private int switchDepth;
    /** Whether the body or block being checked is unsafe, where pointers may be held. */
    private boolean unsafe;
    private int quiet;
    /**
     * The catches and the finally blocks the statement being checked stands in, innermost last: a catch, or the
     * loops and choices open where a finally began, which a way out of it may not leave.
     */
    private List<Object> handlers = new ArrayList<>();

    /** The largest whole number a byte holds. */
    private static final int BYTE_MAX = 255;

    /** A finally, with how many loops and choices were open where it began. */
    record Finally(int loops, int choices) {
    }

    BodyScope(final BuiltIns builtIns, final TypeRules rules, final Declarations declarations,
              final SemanticModel model, final DiagnosticBag diagnostics) {
        this.builtIns = builtIns;
        this.rules = rules;
        this.declarations = declarations;
        this.model = model;
        this.diagnostics = diagnostics;
        this.constants = new Constants(model, declarations);
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

    Constants constants() {
        return this.constants;
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

    /** Whether pointers may be held here: in an unsafe method or block, or anywhere in Sigma, as in C. */
    boolean unsafe() {
        return this.unsafe || this.declarations.level() == LanguageLevel.SIGMA;
    }

    void unsafe(final boolean on) {
        this.unsafe = on;
        this.declarations.unsafeHere(on);
    }

    /** Starts on a fresh body: nothing in scope, and everything about where it is set from what it is. */
    void begin(final boolean isStatic, final ITypeSymbol returns, final boolean constructor) {
        this.scope = new Scope(null);
        this.staticContext = isStatic;
        this.returnType = returns;
        this.inConstructor = constructor;
        this.loopDepth = 0;
        this.switchDepth = 0;
        this.handlers = new ArrayList<>();
        this.unsafe(false);
    }

    /** Steps into a catch. */
    void enterCatch(final IStmt.Catch one) {
        this.handlers.add(one);
    }

    /** Steps into a finally. */
    void enterFinally() {
        this.handlers.add(new Finally(this.loopDepth, this.switchDepth));
    }

    /** Steps out of the catch or the finally stepped into last. */
    void leaveHandler() {
        this.handlers.removeLast();
    }

    /** The catch the statement being checked stands in, or null when the nearest is a finally or there is none. */
    IStmt.Catch innermostCatch() {
        return !this.handlers.isEmpty() && this.handlers.getLast() instanceof IStmt.Catch one ? one : null;
    }

    /** The finally the statement being checked stands in, the nearest one, or null when it stands in none. */
    Finally innermostFinally() {
        for (int i = this.handlers.size() - 1; i >= 0; i--) {
            if (this.handlers.get(i) instanceof Finally found) {
                return found;
            }
        }
        return null;
    }

    /**
     * Sets aside the catches and the finally blocks around, for a lambda's body, which stands in none of them, and
     * hands back what to put back after it.
     */
    List<Object> setHandlersAside() {
        final List<Object> around = this.handlers;
        this.handlers = new ArrayList<>();
        return around;
    }

    /**
     * Sets the loops and switches around aside for a lambda's body, which stands in none of them: a break or a
     * continue in it has nothing of the method's to leave. Gives back what was set aside, for {@link #putJumpsBack}.
     */
    int[] setJumpsAside() {
        final int[] around = {this.loopDepth, this.switchDepth};
        this.loopDepth = 0;
        this.switchDepth = 0;
        return around;
    }

    /** Puts back what {@link #setJumpsAside} set aside. */
    void putJumpsBack(final int[] around) {
        this.loopDepth = around[0];
        this.switchDepth = around[1];
    }

    /** Puts back what {@link #setHandlersAside} set aside. */
    void putHandlersBack(final List<Object> around) {
        this.handlers = around;
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
    /** Says which labels of the body being checked start a region that may be switched off. */
    void regions(final Set<String> switchable) {
        this.assignment.regions(switchable);
    }

    /** The labels of the body being checked that start a region that may be switched off. */
    Set<String> regions() {
        return this.assignment.regions();
    }

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

    /** The same for a lambda whose body is one expression: it gives the value when the expression writes it. */
    void checkOutParameters(final List<IDecl.Parameter> parameters, final IExpr body, final INode at) {
        for (final IDecl.Parameter parameter : parameters) {
            if (parameter.outward() && !DefiniteAssignment.writesTo(body, parameter.name())) {
                this.report(at.line(), at.column(), SigmaError.OUT_NOT_ASSIGNED, parameter.name());
            }
        }
    }

    void expect(final ITypeSymbol given, final ITypeSymbol wanted, final INode at) {
        if (!this.rules.isAssignable(given, wanted) && !this.fitsByte(given, wanted, at)) {
            this.report(at.line(), at.column(),
                    SigmaError.CANNOT_CONVERT, given.describe(), wanted.describe());
        }
    }

    /* C#'s one narrowing of its own: a whole number known when compiling goes into a byte when it fits there. */
    boolean fitsByte(final ITypeSymbol given, final ITypeSymbol wanted, final INode at) {
        if (wanted != ITypeSymbol.Primitive.BYTE || !this.rules.isIntegral(given) || !(at instanceof IExpr value)) {
            return false;
        }
        final Constants.Known known = this.constants.fold(value);
        return known != null && known.value() instanceof Number number && number.longValue() >= 0
                && number.longValue() <= BYTE_MAX;
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
                        || ((IMemberSymbol.MethodSymbol) seen).sameParameters(method);
            }
            if (!alreadyThere) {
                found.add(member);
            }
        }
        return found;
    }
}
