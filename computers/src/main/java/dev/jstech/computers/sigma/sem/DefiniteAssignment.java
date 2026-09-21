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
import dev.jstech.computers.sigma.ast.Operator;

/**
 * Whether a parameter the method fills in has been given a value by every way out of the method.
 *
 * <p>A parameter written with {@code out} is the method's promise to the caller, and a promise kept on one path
 * and not another is not kept. So this follows the ways control can leave rather than the order the lines were
 * written in: both halves of a branch have to give the value for the branch to have given it, a loop that may
 * not run at all cannot be counted on for anything, and every {@code return} is a way out and is checked where
 * it stands.
 *
 * <p>It asks one question about statements and one about expressions, and neither is about types, so nothing
 * here resolves anything. What it does need is the way the checker reports, rather than the diagnostics
 * directly: a message raised while the checker is looking ahead has to be taken back, and that is the thing
 * the reporter it is given knows and this does not.
 */
final class DefiniteAssignment {

    private final IReporter reporter;

    DefiniteAssignment(final IReporter reporter) {
        this.reporter = reporter;
    }

    /** How the checker says something, so a look-ahead can still be taken back. */
    interface IReporter {
        void report(int line, int column, SigmaError error, Object... arguments);
    }

    /** Whether {@code name} is certainly given a value by the time control leaves {@code statement}. */
    boolean assignedBy(final IStmt statement, final String name, final boolean assigned) {
        return switch (statement) {
            case IStmt.Block block -> {
                boolean now = assigned;
                for (final IStmt inner : block.statements()) {
                    now = this.assignedBy(inner, name, now);
                }
                yield now;
            }
            case IStmt.ExprStmt expression -> assigned || writesTo(expression.expression(), name);
            case IStmt.LocalDecl local -> assigned || writesTo(local.initializer(), name);
            case IStmt.Return give -> {
                if (!assigned && !writesTo(give.value(), name)) {
                    this.reporter.report(give.line(), give.column(), SigmaError.OUT_NOT_ASSIGNED, name);
                }
                yield true;
            }
            case IStmt.If branch -> {
                final boolean then = this.assignedBy(branch.then(), name, assigned);
                final boolean otherwise = branch.otherwise() == null
                        ? assigned : this.assignedBy(branch.otherwise(), name, assigned);
                yield then && otherwise;
            }
            case IStmt.DoWhile loop -> this.assignedBy(loop.body(), name, assigned);
            case IStmt.Lock lock -> this.assignedBy(lock.body(), name, assigned);
            case IStmt.While loop -> this.aside(loop.body(), name, assigned);
            case IStmt.For loop -> this.aside(loop.body(), name, assigned);
            case IStmt.ForEach loop -> this.aside(loop.body(), name, assigned);
            case IStmt.Switch choice -> {
                for (final IStmt.SwitchSection section : choice.sections()) {
                    boolean now = assigned;
                    for (final IStmt inner : section.statements()) {
                        now = this.assignedBy(inner, name, now);
                    }
                }
                yield assigned;
            }
            default -> assigned;
        };
    }

    /*
     * A body that may not run at all cannot be counted on to have given the value, but a way out
     * inside it still has to be checked.
     */
    private boolean aside(final IStmt body, final String name, final boolean assigned) {
        this.assignedBy(body, name, assigned);
        return assigned;
    }

    /*
     * Whether evaluating this expression gives the name a value: an assignment to it, or handing it
     * to a method as the place to fill in. A lambda's body does not count, because it runs later.
     */
    private static boolean writesTo(final IExpr expression, final String name) {
        return switch (expression) {
            case null -> false;
            case IExpr.Assign assign -> (assign.operator() == Operator.ASSIGN
                    && assign.target() instanceof IExpr.Name target && target.identifier().equals(name))
                    || writesTo(assign.target(), name) || writesTo(assign.value(), name);
            case IExpr.OutArgument outward -> outward.name().equals(name);
            case IExpr.Binary binary -> writesTo(binary.left(), name) || writesTo(binary.right(), name);
            case IExpr.Unary unary -> writesTo(unary.operand(), name);
            case IExpr.Conditional conditional -> writesTo(conditional.condition(), name);
            case IExpr.Call call -> writesTo(call.callee(), name)
                    || call.arguments().stream().anyMatch(argument -> writesTo(argument, name));
            case IExpr.Member member -> writesTo(member.target(), name);
            case IExpr.Index index -> writesTo(index.target(), name) || writesTo(index.index(), name);
            case IExpr.New created -> created.arguments().stream()
                    .anyMatch(argument -> writesTo(argument, name));
            case IExpr.NewArray created -> writesTo(created.length(), name);
            case IExpr.Cast cast -> writesTo(cast.value(), name);
            case IExpr.TypeTest test -> writesTo(test.value(), name);
            default -> false;
        };
    }
}
