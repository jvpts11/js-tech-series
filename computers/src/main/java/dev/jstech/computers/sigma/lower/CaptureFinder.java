/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lower;

import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.SemanticModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Works out what a method's lambdas keep hold of.
 *
 * <p>A lambda outlives the call it was written in, so a variable it uses cannot stay in that call's slot. The
 * variables any lambda of a method reaches for have to live somewhere of their own, shared by the method and
 * all of its lambdas, because two lambdas of the same method that use the same variable must see each other's
 * writes.
 *
 * <p>This is a question about the tree, which is why it is answered here rather than while anything is being
 * written down. The walk also records how deep inside loops each variable was declared, because that is what
 * says whether a variable outlives the lambda that keeps it: one declared inside a loop is a different
 * variable on every turn, and a single shared place could not be all of them.
 */
final class CaptureFinder {

    private final SemanticModel model;
    private final DiagnosticBag diagnostics;

    CaptureFinder(final SemanticModel model, final DiagnosticBag diagnostics) {
        this.model = model;
        this.diagnostics = diagnostics;
    }

    /** What a method's lambdas keep, or nothing when it has no lambda that keeps anything. */
    Captures forMethod(final List<IDecl.Parameter> parameters, final IStmt.Block body, final INode at) {
        final Found method = new Found();
        for (final IDecl.Parameter parameter : parameters) {
            keep(method.declared, this.model.declaredAt(parameter), 0);
        }
        this.walk(body, 0, method);
        if (method.lambdas.isEmpty()) {
            return null;
        }
        final Map<IBinding.Variable, String> fields = new LinkedHashMap<>();
        boolean holdsThis = false;
        for (final IExpr.Lambda lambda : method.lambdas) {
            final Found inside = this.inside(lambda);
            holdsThis = holdsThis || inside.thisToo;
            for (final IBinding.Variable variable : inside.used) {
                final Integer depth = method.declared.get(variable);
                if (depth == null) {
                    this.cannotYet(at, "a lambda inside another one that keeps its variable");
                    return null;
                }
                if (depth > 0) {
                    this.cannotYet(at, "a lambda that keeps a variable a loop declares");
                    return null;
                }
                fields.putIfAbsent(variable, variable.name());
            }
        }
        if (fields.isEmpty()) {
            return null;
        }
        return new Captures(fields, holdsThis);
    }

    private static void keep(final Map<IBinding.Variable, Integer> into, final IBinding.Variable variable,
                             final int depth) {
        if (variable != null) {
            into.putIfAbsent(variable, depth);
        }
    }

    private void cannotYet(final INode at, final String what) {
        this.diagnostics.error(at.line(), at.column(), SigmaError.NOT_YET_BUILT, what);
    }

    /** What a lambda uses from outside itself, and whether it reaches the object it was written in. */
    private Found inside(final IExpr.Lambda lambda) {
        final Found found = new Found();
        for (final IDecl.Parameter parameter : lambda.parameters()) {
            keep(found.declared, this.model.declaredAt(parameter), 0);
        }
        this.walk(lambda.block(), 0, found);
        this.walk(lambda.body(), 0, found);
        found.used.removeAll(found.declared.keySet());
        return found;
    }

    /*
     * One walk serves both questions: how deep inside loops each variable was declared, and what each
     * lambda reaches for. The depth is what says whether a variable outlives the lambda that keeps it.
     */
    private void walk(final IStmt statement, final int depth, final Found found) {
        switch (statement) {
            case null -> { }
            case IStmt.Block block -> block.statements().forEach(inner -> this.walk(inner, depth, found));
            case IStmt.LocalDecl local -> {
                keep(found.declared, this.model.declaredAt(local), depth);
                this.walk(local.initializer(), depth, found);
            }
            case IStmt.ExprStmt expression -> this.walk(expression.expression(), depth, found);
            case IStmt.If branch -> {
                this.walk(branch.condition(), depth, found);
                this.walk(branch.then(), depth, found);
                this.walk(branch.otherwise(), depth, found);
            }
            case IStmt.While loop -> {
                this.walk(loop.condition(), depth, found);
                this.walk(loop.body(), depth + 1, found);
            }
            case IStmt.DoWhile loop -> {
                this.walk(loop.body(), depth + 1, found);
                this.walk(loop.condition(), depth, found);
            }
            case IStmt.For loop -> {
                loop.initializers().forEach(inner -> this.walk(inner, depth + 1, found));
                this.walk(loop.condition(), depth + 1, found);
                loop.updates().forEach(update -> this.walk(update, depth + 1, found));
                this.walk(loop.body(), depth + 1, found);
            }
            case IStmt.ForEach loop -> {
                keep(found.declared, this.model.declaredAt(loop), depth + 1);
                this.walk(loop.source(), depth, found);
                this.walk(loop.body(), depth + 1, found);
            }
            case IStmt.Switch choice -> {
                this.walk(choice.value(), depth, found);
                for (final IStmt.SwitchSection section : choice.sections()) {
                    section.labels().forEach(label -> this.walk(label, depth, found));
                    section.statements().forEach(inner -> this.walk(inner, depth, found));
                }
            }
            case IStmt.Return give -> this.walk(give.value(), depth, found);
            case IStmt.Dispose dispose -> this.walk(dispose.target(), depth, found);
            case IStmt.Lock lock -> {
                this.walk(lock.target(), depth, found);
                this.walk(lock.body(), depth + 1, found);
            }
            default -> { }
        }
    }

    private void walk(final IExpr expression, final int depth, final Found found) {
        switch (expression) {
            case null -> { }
            case IExpr.Name name -> {
                final IBinding binding = this.model.bindingOf(name);
                if (binding instanceof IBinding.Variable variable) {
                    found.used.add(variable);
                } else if (binding instanceof IBinding.Member member && !member.member().isStatic()) {
                    found.thisToo = true;
                }
            }
            case IExpr.This ignored -> found.thisToo = true;
            case IExpr.Base ignored -> found.thisToo = true;
            case IExpr.OutArgument outward -> {
                if (outward.type() != null) {
                    keep(found.declared, this.model.declaredAt(outward), depth);
                }
                if (this.model.bindingOf(outward) instanceof IBinding.Variable variable) {
                    found.used.add(variable);
                }
            }
            case IExpr.Binary binary -> {
                this.walk(binary.left(), depth, found);
                this.walk(binary.right(), depth, found);
            }
            case IExpr.Unary unary -> this.walk(unary.operand(), depth, found);
            case IExpr.Assign assign -> {
                this.walk(assign.target(), depth, found);
                this.walk(assign.value(), depth, found);
            }
            case IExpr.Conditional conditional -> {
                this.walk(conditional.condition(), depth, found);
                this.walk(conditional.whenTrue(), depth, found);
                this.walk(conditional.whenFalse(), depth, found);
            }
            case IExpr.Call call -> {
                this.walk(call.callee(), depth, found);
                call.arguments().forEach(argument -> this.walk(argument, depth, found));
            }
            case IExpr.Member member -> this.walk(member.target(), depth, found);
            case IExpr.Index index -> {
                this.walk(index.target(), depth, found);
                this.walk(index.index(), depth, found);
            }
            case IExpr.New created -> created.arguments()
                    .forEach(argument -> this.walk(argument, depth, found));
            case IExpr.NewArray created -> this.walk(created.length(), depth, found);
            case IExpr.Cast cast -> this.walk(cast.value(), depth, found);
            case IExpr.TypeTest test -> this.walk(test.value(), depth, found);
            case IExpr.Lambda inner -> {
                found.lambdas.add(inner);
                inner.parameters().forEach(parameter ->
                        keep(found.declared, this.model.declaredAt(parameter), depth));
                this.walk(inner.block(), depth, found);
                this.walk(inner.body(), depth, found);
            }
            default -> { }
        }
    }

    /** What one walk of a method found. */
    private static final class Found {
        private final List<IExpr.Lambda> lambdas = new ArrayList<>();
        private final Map<IBinding.Variable, Integer> declared = new IdentityHashMap<>();
        private final Set<IBinding.Variable> used = Collections.newSetFromMap(new IdentityHashMap<>());
        private boolean thisToo;
    }
}
