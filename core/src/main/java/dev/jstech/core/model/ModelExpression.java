/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

/**
 * A value in a keyframe: a number, or an expression of the Core's, which reads the Molang of Bedrock files, worked out
 * each frame from what the model shows ({@link ExpressionContext}). Read by {@link ExpressionParser}.
 */
@FunctionalInterface
public interface ModelExpression {

    /** Zero, the value of nothing. */
    ModelExpression ZERO = constant(0.0);

    /** The value now. */
    double evaluate(ExpressionContext context);

    /** Whether its value never changes, so it is worked out once. */
    default boolean constant() {
        return false;
    }

    /** A value that never changes. */
    static ModelExpression constant(final double value) {
        return new Constant(value);
    }

    /** A value that never changes. */
    record Constant(double value) implements ModelExpression {

        @Override
        public double evaluate(final ExpressionContext context) {
            return this.value;
        }

        @Override
        public boolean constant() {
            return true;
        }
    }
}
