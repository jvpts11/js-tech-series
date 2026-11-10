/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.ast.IExpr;
import java.util.ArrayList;
import java.util.List;

/**
 * Choosing between the versions of a method that share a name.
 *
 * <p>Two questions, and neither of them is about the call: whether a version could have been meant at all, and
 * which of the ones that could is the closest. Both are answered from the types alone, so nothing here reads a
 * body or reports anything. Whoever asked has the argument that is wrong and the place it was written, which is
 * what a message needs and this does not have.
 *
 * <p>Closeness is counted rather than ranked, because a version can be closer in one argument and further in
 * another and the count is what settles it: a type that matches exactly is worth more than one that has to be
 * widened on the way in, and a version that needs no widening anywhere beats one that needs it once.
 */
final class Overloads {

    /** A version that could not have been meant at all, whatever the others score. */
    private static final int IMPOSSIBLE = Integer.MIN_VALUE;

    /** An argument that is already the type wanted. */
    private static final int EXACT = 2;

    /** One that will go in, but not without being widened, or a lambda still waiting for its shape. */
    private static final int WIDENED = 1;
    /** The largest whole number a byte holds. */
    private static final int BYTE_MAX = 255;

    private final TypeRules rules;

    Overloads(final TypeRules rules) {
        this.rules = rules;
    }

    /**
     * The versions that fit the arguments best, or nothing when none of them fits at all.
     *
     * <p>More than one coming back is the call being ambiguous: they are equally close and there is no saying
     * which was meant.
     */
    List<IMemberSymbol.MethodSymbol> best(final List<IMemberSymbol.MethodSymbol> candidates,
                                          final List<ITypeSymbol> given, final List<IExpr> arguments) {
        final List<IMemberSymbol.MethodSymbol> fitting = new ArrayList<>();
        int best = IMPOSSIBLE;
        for (final IMemberSymbol.MethodSymbol candidate : candidates) {
            final int score = this.score(candidate, given, arguments);
            if (score == IMPOSSIBLE) {
                continue;
            }
            if (score > best) {
                best = score;
                fitting.clear();
            }
            if (score == best) {
                fitting.add(candidate);
            }
        }
        return fitting;
    }

    /**
     * Whether the version takes these arguments with its params array handed over a value at a time: it takes
     * params, and it does not take them as they are written, which is always tried first.
     */
    boolean expanded(final IMemberSymbol.MethodSymbol candidate, final List<ITypeSymbol> given,
                     final List<IExpr> arguments) {
        return candidate.takesMany() && this.scoreAs(candidate, given, arguments, false) == IMPOSSIBLE;
    }

    /**
     * How close one version is to the arguments, or {@link #IMPOSSIBLE} when it could not have been meant.
     *
     * <p>A version that takes params is taken as written when the arguments fit that way, and otherwise with the array
     * handed over a value at a time. Taken that way it counts one less than a version that fits as written with the
     * same closeness in every argument, as C# prefers the version that needs no array made for it; the scores are
     * doubled so that this never outweighs one argument fitting better.
     */
    private int score(final IMemberSymbol.MethodSymbol candidate, final List<ITypeSymbol> given,
                      final List<IExpr> arguments) {
        final int written = this.scoreAs(candidate, given, arguments, false);
        if (written != IMPOSSIBLE) {
            return written * 2;
        }
        final int expanded = candidate.takesMany() ? this.scoreAs(candidate, given, arguments, true) : IMPOSSIBLE;
        return expanded == IMPOSSIBLE ? IMPOSSIBLE : expanded * 2 - 1;
    }

    /**
     * The closeness of one form of a version: as written, or with the params array handed over a value at a time.
     *
     * <p>A type that is not known yet, which is a lambda or a method handed over without brackets, only fits a
     * parameter that takes a delegate, and counts as needing work: a version that takes it as something already
     * settled is the closer one.
     */
    private int scoreAs(final IMemberSymbol.MethodSymbol candidate, final List<ITypeSymbol> given,
                        final List<IExpr> arguments, final boolean expanded) {
        final int fixed = expanded ? candidate.parameters().size() - 1 : candidate.parameters().size();
        if (expanded ? given.size() < fixed : given.size() != fixed) {
            return IMPOSSIBLE;
        }
        int total = 0;
        for (int i = 0; i < given.size(); i++) {
            final IMemberSymbol.ParameterSymbol parameter = candidate.parameters().get(Math.min(i, fixed));
            final boolean outward = arguments.get(i) instanceof IExpr.OutArgument;
            if (outward != parameter.outward()) {
                return IMPOSSIBLE;
            }
            final ITypeSymbol wanted = i < fixed ? parameter.type() : elementOf(parameter);
            final ITypeSymbol argument = given.get(i);
            if (argument == null) {
                if (outward) {
                    total += EXACT;
                    continue;
                }
                final NamedType named = this.rules.named(wanted);
                if (named == null || named.kind() != NamedType.Kind.DELEGATE) {
                    return IMPOSSIBLE;
                }
                total += WIDENED;
            } else if (argument.equals(wanted)) {
                total += EXACT;
            } else if (!outward && (this.rules.isAssignable(argument, wanted) || fitsByte(arguments.get(i), wanted))) {
                total += WIDENED;
            } else {
                return IMPOSSIBLE;
            }
        }
        return total;
    }

    /** Whether a whole number written out goes into a byte that is wanted, as C# lets one that fits. */
    static boolean fitsByte(final IExpr argument, final ITypeSymbol wanted) {
        return wanted == ITypeSymbol.Primitive.BYTE && argument instanceof IExpr.Literal literal
                && literal.value() instanceof Integer number && number >= 0 && number <= BYTE_MAX;
    }

    /** What a params parameter takes one at a time: the element of its array. */
    static ITypeSymbol elementOf(final IMemberSymbol.ParameterSymbol parameter) {
        return parameter.type() instanceof ITypeSymbol.ArrayType array ? array.element() : parameter.type();
    }
}
