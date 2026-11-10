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
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
@TextHolder
final class StatementChecker {

    private final BodyScope scope;
    private final ExpressionChecker expressions;

    static final String INFERRED = "var";

    /** What a message names where there is no value or type at all, as in "cannot convert 'nothing' to 'var'". */
    static final TextKey NOTHING = TextKey.of("jsc.sigma.statement_checker.nothing", "nothing");

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
     * A method that gives something back has to do it on every way out. This knows the shapes that certainly leave;
     * anything else counts as a path that falls off the end. A goto never falls through, since where it lands is
     * held to the same rule where the label stands; a label and a comefrom are ways in, which what came before them
     * does not cover; and the region of a label that may be switched off may be passed over, to its block's end.
     */
    static boolean alwaysReturns(final IStmt statement, final Set<String> switchable) {
        return switch (statement) {
            case IStmt.Return ignored -> true;
            case IStmt.Goto ignored -> true;
            case IStmt.Throw ignored -> true;
            // A try returns when what it protects does and so does every catch; a finally adds no way out.
            case IStmt.Try guarded -> alwaysReturns(guarded.body(), switchable) && guarded.catches().stream()
                    .allMatch(one -> alwaysReturns(one.body(), switchable));
            case IStmt.Block block -> runReturns(block.statements(), switchable);
            case IStmt.Labeled labeled -> !switchable.contains(labeled.label())
                    && alwaysReturns(labeled.statement(), switchable);
            case IStmt.If branch -> branch.otherwise() != null
                    && alwaysReturns(branch.then(), switchable) && alwaysReturns(branch.otherwise(), switchable);
            case IStmt.While loop -> isAlwaysTrue(loop.condition()) && !breaksOut(loop.body());
            case IStmt.DoWhile loop -> (alwaysReturns(loop.body(), switchable) || isAlwaysTrue(loop.condition()))
                    && !breaksOut(loop.body());
            case IStmt.For loop -> (loop.condition() == null || isAlwaysTrue(loop.condition()))
                    && !breaksOut(loop.body());
            case IStmt.Switch choice -> choice.sections().stream().anyMatch(IStmt.SwitchSection::fallback)
                    && choice.sections().stream().allMatch(section -> sectionReturns(section, switchable));
            case IStmt.Lock lock -> alwaysReturns(lock.body(), switchable);
            case IStmt.Unsafe unsafe -> alwaysReturns(unsafe.body(), switchable);
            default -> false;
        };
    }

    /* A run of statements returns when one of them does after the last way into it. */
    private static boolean runReturns(final List<IStmt> statements, final Set<String> switchable) {
        boolean returned = false;
        for (final IStmt statement : statements) {
            if (statement instanceof IStmt.Labeled labeled && switchable.contains(labeled.label())) {
                return false;
            }
            if (statement instanceof IStmt.Labeled || statement instanceof IStmt.ComeFrom) {
                returned = false;
            }
            returned |= alwaysReturns(statement, switchable);
        }
        return returned;
    }

    /*
     * Whether a break in this statement leaves the loop or switch that holds it. A break inside a loop or a switch
     * nested in it leaves that one instead, and a lambda is an expression, so neither is looked into. A goto may leave
     * it too, and so may the end of a labelled statement a comefrom takes.
     */
    private static boolean breaksOut(final IStmt statement) {
        return switch (statement) {
            case IStmt.Break ignored -> true;
            case IStmt.Goto ignored -> true;
            case IStmt.Labeled ignored -> true;
            case IStmt.Block block -> block.statements().stream().anyMatch(StatementChecker::breaksOut);
            case IStmt.If branch -> breaksOut(branch.then())
                    || branch.otherwise() != null && breaksOut(branch.otherwise());
            case IStmt.Lock lock -> breaksOut(lock.body());
            case IStmt.Unsafe unsafe -> breaksOut(unsafe.body());
            case IStmt.Try guarded -> breaksOut(guarded.body())
                    || guarded.catches().stream().anyMatch(one -> breaksOut(one.body()));
            default -> false;
        };
    }

    /* A section returns when a statement of it does and no break comes before that one; one after it never runs. */
    private static boolean sectionReturns(final IStmt.SwitchSection section, final Set<String> switchable) {
        for (final IStmt statement : section.statements()) {
            if (breaksOut(statement)) {
                return false;
            }
            if (alwaysReturns(statement, switchable)) {
                return true;
            }
        }
        return false;
    }

    void checkBlock(final IStmt.Block block, final boolean newScope) {
        final Scope saved = this.scope.scope();
        if (newScope) {
            this.scope.scope(new Scope(saved));
        }
        this.declareLocalFunctions(block.statements());
        for (final IStmt statement : block.statements()) {
            this.checkStatement(statement);
        }
        this.scope.scope(saved);
    }

    /*
     * A local function may be called anywhere in the block it stands in, before it as well, as C#'s may, so the names
     * of a block's local functions are declared before any of its statements is read.
     */
    private void declareLocalFunctions(final List<IStmt> statements) {
        for (final IStmt statement : statements) {
            if (statement instanceof IStmt.LocalFunction function) {
                this.declareLocalFunction(function);
            }
        }
    }

    /* A local function's name, a variable holding the delegate of its shape, which nothing writes to. */
    private IBinding.Variable declareLocalFunction(final IStmt.LocalFunction function) {
        this.scope.declarations().refuseReceiver(function.body().parameters(), function.name());
        final ITypeSymbol returns = this.scope.declarations().resolve(function.returnType(), this.scope.currentType());
        final NamedType shape = NamedType.shapeOf(function.name(), returns, this.scope.declarations()
                .parametersOf(function.body().parameters(), this.scope.currentType()));
        final IBinding.Variable variable = new IBinding.Variable(function.name(), shape, false);
        if (!this.scope.scope().declare(variable)) {
            this.scope.report(function.line(), function.column(), SigmaError.DUPLICATE_DECLARATION, function.name());
        }
        this.scope.model().setDeclared(function, variable);
        this.scope.model().setLocalFunction(variable);
        return variable;
    }

    /* A local function's body, checked as its lambda against its own shape, which has to give back on every way out. */
    private void checkLocalFunction(final IStmt.LocalFunction function) {
        final IBinding.Variable declared = this.scope.model().declaredAt(function);
        final IBinding.Variable variable = declared != null ? declared : this.declareLocalFunction(function);
        this.expressions.localFunction(function, variable.type());
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
        // A body the player has not typed yet, read while the line is still being written, has nothing to check.
        if (statement == null) {
            return;
        }
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
                } else if (this.leavesFinally(false)) {
                    this.scope.report(stop.line(), stop.column(), SigmaError.FINALLY_EXIT);
                }
            }
            case IStmt.Continue next -> {
                if (this.scope.loopDepth() == 0) {
                    this.scope.report(next.line(), next.column(), SigmaError.CONTINUE_OUTSIDE_LOOP);
                } else if (this.leavesFinally(true)) {
                    this.scope.report(next.line(), next.column(), SigmaError.FINALLY_EXIT);
                }
            }
            case IStmt.Return give -> this.checkReturn(give);
            case IStmt.LocalDecl local -> this.checkLocal(local);
            case IStmt.ExprStmt expression -> this.expressions.check(expression.expression(), null);
            case IStmt.Dispose dispose -> this.checkDispose(dispose);
            case IStmt.Lock lock -> this.checkLock(lock);
            case IStmt.Unsafe unsafe -> this.checkUnsafe(unsafe);
            case IStmt.Labeled labeled -> this.checkStatement(labeled.statement());
            // Where a jump goes is worked out once the whole body is read: a label may come after it.
            case IStmt.Goto jump -> {
                if (jump.caseValue() != null) {
                    this.expressions.check(jump.caseValue(), null);
                }
            }
            case IStmt.ComeFrom ignored -> { }
            case IStmt.Abstain ignored -> { }
            case IStmt.Ignore place -> this.checkIgnore(place);
            case IStmt.LocalFunction function -> this.checkLocalFunction(function);
            case IStmt.Deconstruct taken -> this.checkDeconstruct(taken);
            case IStmt.Try guarded -> this.checkTry(guarded);
            case IStmt.Throw thrown -> this.checkThrow(thrown);
            case IStmt.Empty ignored -> { }
        }
    }

    /*
     * A try: what it protects; each catch, of an exception and never of one a catch before it already takes, with the
     * exception it took as a local of its own; and its finally, which nothing leaves but its own end.
     */
    private void checkTry(final IStmt.Try guarded) {
        this.scope.model().setUsesExceptions();
        this.checkBlock(guarded.body(), true);
        final NamedType root = this.scope.builtIns().exceptionType();
        final List<ITypeSymbol> taken = new ArrayList<>();
        for (final IStmt.Catch one : guarded.catches()) {
            final ITypeSymbol type = one.type() == null ? root
                    : this.scope.declarations().resolve(one.type(), this.scope.currentType());
            final boolean anException = type instanceof NamedType named && named.isOrDescendsFrom(root);
            if (!this.scope.rules().isError(type) && !anException) {
                this.scope.report(one.line(), one.column(), SigmaError.CATCH_TYPE, type.describe());
            } else if (taken.stream().anyMatch(before -> this.scope.rules().isAssignable(type, before))) {
                this.scope.report(one.line(), one.column(), SigmaError.CATCH_UNREACHABLE, type.describe());
            }
            taken.add(type);
            this.scope.model().setCaught(one, type);
            final Scope saved = this.scope.scope();
            this.scope.scope(new Scope(saved));
            if (one.name() != null) {
                final IBinding.Variable variable = new IBinding.Variable(one.name(), type, false);
                if (!this.scope.scope().declare(variable)) {
                    this.scope.report(one.line(), one.column(), SigmaError.DUPLICATE_DECLARATION, one.name());
                }
                this.scope.model().setDeclared(one, variable);
            }
            this.scope.enterCatch(one);
            this.checkBlock(one.body(), false);
            this.scope.leaveHandler();
            this.scope.scope(saved);
        }
        if (guarded.cleanup() != null) {
            this.scope.enterFinally();
            this.checkBlock(guarded.cleanup(), true);
            this.scope.leaveHandler();
        }
    }

    /* A throw: of an exception; or, written bare, of the one the catch it stands in took, thrown on as it was. */
    private void checkThrow(final IStmt.Throw thrown) {
        this.scope.model().setUsesExceptions();
        if (thrown.value() == null) {
            final IStmt.Catch around = this.scope.innermostCatch();
            if (around == null) {
                this.scope.report(thrown.line(), thrown.column(), SigmaError.RETHROW_OUTSIDE);
            } else {
                this.scope.model().setRethrows(thrown, around);
            }
            return;
        }
        final ITypeSymbol type = this.expressions.check(thrown.value(), null);
        if (!this.scope.rules().isError(type) && !this.scope.rules().isAssignable(type,
                this.scope.builtIns().exceptionType())) {
            this.scope.report(thrown.line(), thrown.column(), SigmaError.THROW_TYPE, type.describe());
        }
    }

    /* Whether a break or a continue would leave the finally it stands in, which nothing but its end leaves. */
    private boolean leavesFinally(final boolean continuing) {
        final BodyScope.Finally around = this.scope.innermostFinally();
        return around != null && (continuing ? this.scope.loopDepth() == around.loops()
                : this.scope.loopDepth() + this.scope.switchDepth() == around.loops() + around.choices());
    }

    /* "var (count, name) = Find();": a new local for each place of the tuple, of that place's type. */
    private void checkDeconstruct(final IStmt.Deconstruct taken) {
        final ITypeSymbol value = this.expressions.check(taken.value(), null);
        final List<ITypeSymbol> places = value instanceof ITypeSymbol.TupleType tuple ? tuple.places() : null;
        final boolean fits = places != null && places.size() == taken.names().size();
        if (!fits && !this.scope.rules().isError(value)) {
            this.scope.report(taken.line(), taken.column(), SigmaError.DECONSTRUCT_SHAPE, value.describe(),
                    taken.names().size());
        }
        for (int i = 0; i < taken.names().size(); i++) {
            final IExpr.Name name = taken.names().get(i);
            final ITypeSymbol type = fits ? places.get(i) : ITypeSymbol.Special.ERROR;
            final IBinding.Variable variable = new IBinding.Variable(name.identifier(), type, false);
            if (!this.scope.scope().declare(variable)) {
                this.scope.report(name.line(), name.column(), SigmaError.DUPLICATE_DECLARATION, name.identifier());
            }
            this.scope.model().setDeclared(name, variable);
            this.scope.model().setBinding(name, variable);
            this.scope.model().setType(name, type);
        }
    }

    /* What ignore and remember work on: a local of the running call, a field of an object, or a static field. */
    private void checkIgnore(final IStmt.Ignore ignore) {
        final ITypeSymbol type = this.expressions.check(ignore.place(), null);
        if (this.scope.rules().isError(type)) {
            return;
        }
        final IBinding binding = this.scope.model().bindingOf(ignore.place());
        final boolean place = binding instanceof IBinding.Variable variable
                && !this.scope.constants().isLocal(variable) && !this.scope.model().isLocalFunction(variable)
                || binding instanceof IBinding.Member member
                && member.member() instanceof IMemberSymbol.FieldSymbol field && !field.isConstant();
        if (!place) {
            this.scope.report(ignore.line(), ignore.column(), SigmaError.NOT_A_PLACE_TO_IGNORE);
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
            this.declareLocalFunctions(section.statements());
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
            case Float real when !Float.isFinite(real) -> "number:" + real;
            case Double real when !Double.isFinite(real) -> "number:" + real;
            case Number number -> "number:" + new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
            default -> value.getClass().getSimpleName() + ":" + value;
        };
    }

    private void checkReturn(final IStmt.Return give) {
        if (this.scope.innermostFinally() != null) {
            this.scope.report(give.line(), give.column(), SigmaError.FINALLY_EXIT);
        }
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
        if (local.constant()) {
            this.workOutConst(local, variable, declared);
        } else {
            this.scope.model().setAddressable(variable);
        }
    }

    /*
     * A const local holds a value known when compiling, worked out here from what it was given, and every read of it
     * further down is written as that value.
     */
    private void workOutConst(final IStmt.LocalDecl local, final IBinding.Variable variable,
                              final ITypeSymbol declared) {
        if (local.initializer() == null) {
            this.scope.report(local.line(), local.column(), SigmaError.CONST_NEEDS_VALUE, local.name());
            return;
        }
        final Constants.Known found = this.scope.constants().fold(local.initializer());
        final Constants.Known held = found == null ? null : Constants.converted(found, declared);
        if (held == null) {
            if (!this.scope.rules().isError(this.scope.model().typeOf(local.initializer()))
                    && !this.scope.rules().isError(declared)) {
                this.scope.report(local.initializer().line(), local.initializer().column(),
                        SigmaError.CONST_NOT_KNOWN, local.name());
            }
            return;
        }
        if (held.value() != null && !Constants.holdsValues(declared, this.scope.builtIns())) {
            this.scope.report(local.initializer().line(), local.initializer().column(), SigmaError.CONST_TYPE,
                    declared.describe());
            return;
        }
        this.scope.constants().local(variable, held);
    }

    /*
     * "var" takes the type of what it is given, which means it has to be given something, and
     * something with a type of its own: null and a call that gives nothing back have neither.
     */
    private ITypeSymbol inferred(final IStmt.LocalDecl local) {
        if (local.initializer() == null) {
            this.scope.report(local.line(), local.column(),
                    SigmaError.CANNOT_CONVERT, NOTHING, INFERRED);
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

    /* An unsafe block: its statements may hold pointers, as the whole body of an unsafe method may. */
    private void checkUnsafe(final IStmt.Unsafe unsafe) {
        final boolean was = this.scope.unsafe();
        this.scope.unsafe(true);
        this.checkBlock(unsafe.body(), true);
        this.scope.unsafe(was);
    }
}
