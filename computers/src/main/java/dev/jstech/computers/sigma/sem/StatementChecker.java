/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.ast.TypeRef;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Checks what a method does, statement by statement.
 *
 * <p>Most of it is about where things are allowed to be: a name may not be declared while an outer one
 * of the same name is still visible, a break needs something to break out of, a return has to match what
 * the method answers with. Those are questions about the place a statement sits in, which is why they are
 * here and not with the values.
 *
 * <p>A block opens a scope of its own and gives it back afterwards, and so do the shapes that declare
 * something of their own for their body to see. Getting that wrong is invisible until two names collide,
 * so it is written the same way every time: keep what was there, replace it, put it back.
 */
final class StatementChecker {

    private final BodyScope scope;
    private final ExpressionChecker expressions;

    static final String INFERRED = "var";

    StatementChecker(final BodyScope scope, final ExpressionChecker expressions) {
        this.scope = scope;
        this.expressions = expressions;
    }

    /** Whether a type was written as {@code var}, which means it takes the type of what it is given. */
    static boolean isInferred(final TypeRef reference) {
        return reference != null && INFERRED.equals(reference.name())
                && reference.arrayRank() == 0 && reference.arguments().isEmpty();
    }

    /*
     * A method that gives something back has to do it on every way out. This knows the shapes that
     * certainly leave; anything else counts as a path that falls off the end.
     */
    static boolean alwaysReturns(final IStmt statement) {
        return switch (statement) {
            case IStmt.Return ignored -> true;
            case IStmt.Block block -> block.statements().stream().anyMatch(StatementChecker::alwaysReturns);
            case IStmt.If branch -> branch.otherwise() != null
                    && alwaysReturns(branch.then()) && alwaysReturns(branch.otherwise());
            case IStmt.While loop -> isAlwaysTrue(loop.condition());
            case IStmt.DoWhile loop -> alwaysReturns(loop.body()) || isAlwaysTrue(loop.condition());
            case IStmt.For loop -> loop.condition() == null || isAlwaysTrue(loop.condition());
            case IStmt.Switch choice -> choice.sections().stream().anyMatch(IStmt.SwitchSection::fallback)
                    && choice.sections().stream().allMatch(section ->
                            section.statements().stream().anyMatch(StatementChecker::alwaysReturns));
            case IStmt.Lock lock -> alwaysReturns(lock.body());
            default -> false;
        };
    }

    void checkBlock(final IStmt.Block block, final boolean newScope) {
        final Scope saved = this.scope.scope();
        if (newScope) {
            this.scope.scope(new Scope(saved));
        }
        for (final IStmt statement : block.statements()) {
            this.checkStatement(statement);
        }
        this.scope.scope(saved);
    }

    /** Whatever an expression is being used as a condition has to be a yes or a no. */
    void condition(final IExpr expression) {
        final ITypeSymbol type = this.expressions.check(expression, ITypeSymbol.Primitive.BOOL);
        if (!this.scope.rules().isError(type) && type != ITypeSymbol.Primitive.BOOL) {
            this.scope.report(expression.line(), expression.column(),
                    SigmaError.CONDITION_MUST_BE_BOOL, type.describe());
        }
    }

    private static boolean isAlwaysTrue(final IExpr condition) {
        return condition instanceof IExpr.Literal literal && Boolean.TRUE.equals(literal.value());
    }

    private void checkStatement(final IStmt statement) {
        switch (statement) {
            case IStmt.Block block -> this.checkBlock(block, true);
            case IStmt.If branch -> {
                this.condition(branch.condition());
                this.checkStatement(branch.then());
                if (branch.otherwise() != null) {
                    this.checkStatement(branch.otherwise());
                }
            }
            case IStmt.While loop -> {
                this.condition(loop.condition());
                this.scope.enterLoop();
                this.checkStatement(loop.body());
                this.scope.leaveLoop();
            }
            case IStmt.DoWhile loop -> {
                this.scope.enterLoop();
                this.checkStatement(loop.body());
                this.scope.leaveLoop();
                this.condition(loop.condition());
            }
            case IStmt.For loop -> this.checkFor(loop);
            case IStmt.ForEach loop -> this.checkForEach(loop);
            case IStmt.Switch choice -> this.checkSwitch(choice);
            case IStmt.Break stop -> {
                if (this.scope.loopDepth() == 0 && this.scope.switchDepth() == 0) {
                    this.scope.report(stop.line(), stop.column(), SigmaError.BREAK_OUTSIDE_LOOP);
                }
            }
            case IStmt.Continue next -> {
                if (this.scope.loopDepth() == 0) {
                    this.scope.report(next.line(), next.column(), SigmaError.CONTINUE_OUTSIDE_LOOP);
                }
            }
            case IStmt.Return give -> this.checkReturn(give);
            case IStmt.LocalDecl local -> this.checkLocal(local);
            case IStmt.ExprStmt expression -> this.expressions.check(expression.expression(), null);
            case IStmt.Dispose dispose -> this.checkDispose(dispose);
            case IStmt.Lock lock -> this.checkLock(lock);
            case IStmt.Empty ignored -> { }
        }
    }

    private void checkFor(final IStmt.For loop) {
        final Scope saved = this.scope.scope();
        this.scope.scope(new Scope(saved));
        for (final IStmt initializer : loop.initializers()) {
            this.checkStatement(initializer);
        }
        if (loop.condition() != null) {
            this.condition(loop.condition());
        }
        for (final IExpr update : loop.updates()) {
            this.expressions.check(update, null);
        }
        this.scope.enterLoop();
        this.checkStatement(loop.body());
        this.scope.leaveLoop();
        this.scope.scope(saved);
    }

    private void checkForEach(final IStmt.ForEach loop) {
        final ITypeSymbol source = this.expressions.check(loop.source(), null);
        final ITypeSymbol element = this.scope.rules().elementOf(source);
        if (element == null) {
            this.scope.report(loop.line(), loop.column(), SigmaError.NOT_A_COLLECTION, source.describe());
        }
        final ITypeSymbol found = element == null ? ITypeSymbol.Special.ERROR : element;
        final ITypeSymbol declared = isInferred(loop.type()) ? found
                : this.scope.declarations().resolve(loop.type(), this.scope.currentType());
        if (!this.scope.rules().isAssignable(found, declared)) {
            this.scope.report(loop.line(), loop.column(),
                    SigmaError.CANNOT_CONVERT, found.describe(), declared.describe());
        }
        final Scope saved = this.scope.scope();
        this.scope.scope(new Scope(saved));
        final IBinding.Variable variable = new IBinding.Variable(loop.name(), declared, false);
        if (!this.scope.scope().declare(variable)) {
            this.scope.report(loop.line(), loop.column(), SigmaError.DUPLICATE_DECLARATION, loop.name());
        }
        this.scope.model().setDeclared(loop, variable);
        this.scope.enterLoop();
        this.checkStatement(loop.body());
        this.scope.leaveLoop();
        this.scope.scope(saved);
    }

    private void checkSwitch(final IStmt.Switch choice) {
        final ITypeSymbol value = this.expressions.check(choice.value(), null);
        final Set<String> seen = new HashSet<>();
        this.scope.enterChoice();
        for (final IStmt.SwitchSection section : choice.sections()) {
            for (final IExpr label : section.labels()) {
                final ITypeSymbol labelType = this.expressions.check(label, value);
                if (!this.scope.rules().isAssignable(labelType, value)) {
                    this.scope.report(label.line(), label.column(),
                            SigmaError.CANNOT_CONVERT, labelType.describe(), value.describe());
                } else if (label instanceof IExpr.Literal literal && !seen.add(labelKey(literal.value()))) {
                    this.scope.report(label.line(), label.column(), SigmaError.DUPLICATE_SWITCH_LABEL);
                }
            }
            final Scope saved = this.scope.scope();
            this.scope.scope(new Scope(saved));
            for (final IStmt statement : section.statements()) {
                this.checkStatement(statement);
            }
            this.scope.scope(saved);
        }
        this.scope.leaveChoice();
    }

    /*
     * Two labels are the same label when they pick the same value. A character in a numeric switch picks its code,
     * and 1 and 1.0 pick the same number, so numbers and characters are compared by value, not by how they were
     * written; anything else is compared as written, kept apart by its kind.
     */
    private static String labelKey(final Object value) {
        return switch (value) {
            case null -> "null";
            case Character character -> "number:" + (int) character;
            case Number number -> "number:" + new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
            default -> value.getClass().getSimpleName() + ":" + value;
        };
    }

    private void checkReturn(final IStmt.Return give) {
        if (give.value() == null) {
            if (this.scope.returnType() != ITypeSymbol.Primitive.VOID) {
                this.scope.report(give.line(), give.column(),
                        SigmaError.MISSING_RETURN_VALUE, this.scope.returnType().describe());
            }
            return;
        }
        final ITypeSymbol value = this.expressions.check(give.value(), this.scope.returnType());
        if (this.scope.returnType() == ITypeSymbol.Primitive.VOID) {
            this.scope.report(give.line(), give.column(), SigmaError.UNEXPECTED_RETURN_VALUE);
            return;
        }
        this.scope.expect(value, this.scope.returnType(), give.value());
    }

    private void checkLocal(final IStmt.LocalDecl local) {
        final ITypeSymbol declared = isInferred(local.type()) ? this.inferred(local) : this.written(local);
        final IBinding.Variable variable = new IBinding.Variable(local.name(), declared, false);
        if (!this.scope.scope().declare(variable)) {
            this.scope.report(local.line(), local.column(),
                    SigmaError.DUPLICATE_DECLARATION, local.name());
        }
        this.scope.model().setDeclared(local, variable);
    }

    /*
     * "var" takes the type of what it is given, which means it has to be given something, and
     * something with a type of its own: null and a call that gives nothing back have neither.
     */
    private ITypeSymbol inferred(final IStmt.LocalDecl local) {
        if (local.initializer() == null) {
            this.scope.report(local.line(), local.column(),
                    SigmaError.CANNOT_CONVERT, "nothing", INFERRED);
            return ITypeSymbol.Special.ERROR;
        }
        final ITypeSymbol found = this.expressions.check(local.initializer(), null);
        if (found == ITypeSymbol.Special.NULL || found == ITypeSymbol.Primitive.VOID) {
            this.scope.report(local.line(), local.column(),
                    SigmaError.CANNOT_CONVERT, found.describe(), INFERRED);
            return ITypeSymbol.Special.ERROR;
        }
        return found;
    }

    private ITypeSymbol written(final IStmt.LocalDecl local) {
        final ITypeSymbol declared = this.scope.declarations().resolve(local.type(), this.scope.currentType());
        if (local.initializer() != null) {
            this.scope.expect(this.expressions.check(local.initializer(), declared), declared,
                    local.initializer());
        }
        return declared;
    }

    private void checkDispose(final IStmt.Dispose dispose) {
        final ITypeSymbol target = this.expressions.check(dispose.target(), null);
        if (!this.scope.rules().isError(target) && !this.scope.rules().isReference(target)) {
            this.scope.report(dispose.line(), dispose.column(),
                    SigmaError.CANNOT_DISPOSE, target.describe());
        }
    }

    /* Only something on the heap has a lock to take: a number is a value, and two copies of it are two things. */
    private void checkLock(final IStmt.Lock lock) {
        final ITypeSymbol target = this.expressions.check(lock.target(), null);
        if (!this.scope.rules().isError(target) && !this.scope.rules().isReference(target)) {
            this.scope.report(lock.line(), lock.column(), SigmaError.CANNOT_LOCK, target.describe());
        }
        this.checkStatement(lock.body());
    }
}
