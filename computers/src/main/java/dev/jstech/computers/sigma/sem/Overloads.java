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
    private static final int IMPOSSIBLE = -1;

    /** An argument that is already the type wanted. */
    private static final int EXACT = 2;

    /** One that will go in, but not without being widened, or a lambda still waiting for its shape. */
    private static final int WIDENED = 1;

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
            if (score < 0) {
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
     * How close one version is to the arguments, or {@link #IMPOSSIBLE} when it could not have been meant.
     *
     * <p>A type that is not known yet, which is a lambda or a method handed over without brackets, only fits a
     * parameter that takes a delegate, and counts as needing work: a version that takes it as something already
     * settled is the closer one.
     */
    private int score(final IMemberSymbol.MethodSymbol candidate, final List<ITypeSymbol> given,
                      final List<IExpr> arguments) {
        if (candidate.parameters().size() != given.size()) {
            return IMPOSSIBLE;
        }
        int total = 0;
        for (int i = 0; i < given.size(); i++) {
            final IMemberSymbol.ParameterSymbol parameter = candidate.parameters().get(i);
            final boolean outward = arguments.get(i) instanceof IExpr.OutArgument;
            if (outward != parameter.outward()) {
                return IMPOSSIBLE;
            }
            final ITypeSymbol wanted = parameter.type();
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
            } else if (!outward && this.rules.isAssignable(argument, wanted)) {
                total += WIDENED;
            } else {
                return IMPOSSIBLE;
            }
        }
        return total;
    }
}
