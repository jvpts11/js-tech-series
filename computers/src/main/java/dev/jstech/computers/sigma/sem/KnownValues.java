/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.lower.Lowerer;
import dev.jstech.computers.vm.system.SystemApi;

/**
 * Whether a value is one the compiler knows: written out in the source, or named by the library as one that never
 * changes, and anything made of those.
 *
 * <p>A property written with an arrow, {@code string Name => "Lantern OS";}, holds such a value. It is worked out once,
 * when its object is made, and since nothing it is made of can change, reading it once is reading it every time, as
 * C# would read it. A system's declarations are written this way, so the build reads them without running anything.
 */
final class KnownValues {

    private KnownValues() {
    }

    /** Whether {@code value}, already checked into {@code model}, is one the compiler knows. */
    static boolean known(final IExpr value, final SemanticModel model) {
        return switch (value) {
            case IExpr.Literal ignored -> true;
            case IExpr.Unary unary -> !unary.postfix() && unary.operator() != Operator.INCREMENT
                    && unary.operator() != Operator.DECREMENT && known(unary.operand(), model);
            case IExpr.Binary binary -> known(binary.left(), model) && known(binary.right(), model);
            case IExpr.Conditional conditional -> known(conditional.condition(), model)
                    && known(conditional.whenTrue(), model) && known(conditional.whenFalse(), model);
            case IExpr.Interpolation written -> allKnown(written, model);
            case IExpr.Member member -> libraryKnows(model.bindingOf(member) instanceof IBinding.Member read
                    ? read.member() : null) && knownTarget(member.target(), model);
            case IExpr.Call call -> libraryKnows(model.callOf(call)) && call.callee() instanceof IExpr.Member callee
                    && knownTarget(callee.target(), model) && call.arguments().stream().allMatch(a -> known(a, model));
            default -> false;
        };
    }

    /* What a known member is read through: the library's type by its name, or a known value. */
    private static boolean knownTarget(final IExpr target, final SemanticModel model) {
        return model.bindingOf(target) instanceof IBinding.TypeName || known(target, model);
    }

    /* Whether that member is one the library says never changes. */
    private static boolean libraryKnows(final IMemberSymbol member) {
        return member != null && member.owner().isBuiltIn() && SystemApi.isKnown(member.owner().name(), member.name());
    }

    private static boolean allKnown(final IExpr.Interpolation written, final SemanticModel model) {
        for (final IExpr hole : Lowerer.holesOf(written)) {
            if (!known(hole, model)) {
                return false;
            }
        }
        return true;
    }
}
