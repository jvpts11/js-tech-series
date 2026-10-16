/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/**
 * What an expression in a keyframe reads as it is worked out: the queries of what the model shows (how far through its
 * animation it is, how long it has lived, what the machine it draws is doing) and the variables an expression sets
 * and reads. A query not given reads 0, as Molang's do.
 */
public final class ExpressionContext {

    private final Map<String, ToDoubleFunction<ExpressionContext>> queries = new HashMap<>();
    private final Map<String, Double> variables = new HashMap<>();
    private double animTime;
    private double lifeTime;

    /** The time into the animation being worked out, in seconds: {@code query.anim_time}. */
    public double animTime() {
        return this.animTime;
    }

    /** Sets the time into the animation being worked out. */
    public ExpressionContext animTime(final double seconds) {
        this.animTime = seconds;
        return this;
    }

    /** How long what the model draws has been in view, in seconds: {@code query.life_time}. */
    public double lifeTime() {
        return this.lifeTime;
    }

    /** Sets how long what the model draws has been in view. */
    public ExpressionContext lifeTime(final double seconds) {
        this.lifeTime = seconds;
        return this;
    }

    /** Gives the query {@code name} ({@code query.name}, or {@code q.name}) a value worked out as it is read. */
    public ExpressionContext query(final String name, final ToDoubleFunction<ExpressionContext> value) {
        this.queries.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
        return this;
    }

    /** Gives the query {@code name} a fixed value. */
    public ExpressionContext query(final String name, final double value) {
        return query(name, context -> value);
    }

    /** The value of the query {@code name}: the animation's time and life time, or what was given; 0 otherwise. */
    public double readQuery(final String name) {
        return switch (name) {
            case "anim_time" -> this.animTime;
            case "life_time" -> this.lifeTime;
            default -> {
                final ToDoubleFunction<ExpressionContext> value = this.queries.get(name);
                yield value == null ? 0.0 : value.applyAsDouble(this);
            }
        };
    }

    /** The value of the variable {@code name}, 0 until one is set. */
    public double variable(final String name) {
        return this.variables.getOrDefault(name, 0.0);
    }

    /** Whether the variable {@code name} was ever set. */
    public boolean hasVariable(final String name) {
        return this.variables.containsKey(name);
    }

    /** Sets the variable {@code name}. */
    public void setVariable(final String name, final double value) {
        this.variables.put(name, value);
    }
}
