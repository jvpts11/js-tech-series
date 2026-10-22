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
 *
 * <p>A query worked out by code is read once a frame, however many keyframes read it: its first read in a frame is
 * kept until the next ({@link #nextFrame()}). A query given a value ({@link #value}) holds it until it is given
 * another, which is how the queries of an entity are handed over each frame without making anything new.
 */
public final class ExpressionContext {

    private final Map<String, Query> queries = new HashMap<>();
    private final Map<String, Double> variables = new HashMap<>();
    private double animTime;
    private double lifeTime;
    private long frame;

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

    /**
     * Gives the query {@code name} ({@code query.name}, or {@code q.name}) a value worked out as it is read, once a
     * frame.
     */
    public ExpressionContext query(final String name, final ToDoubleFunction<ExpressionContext> value) {
        this.queries.put(Objects.requireNonNull(name, "name"), new Query(Objects.requireNonNull(value, "value")));
        return this;
    }

    /** Gives the query {@code name} a fixed value. */
    public ExpressionContext query(final String name, final double value) {
        return value(name, value);
    }

    /**
     * Gives the query {@code name} the value {@code value} until it is given another: what code hands over each frame,
     * such as an entity's speed, with nothing made after the first time.
     */
    public ExpressionContext value(final String name, final double value) {
        final Query held = this.queries.get(Objects.requireNonNull(name, "name"));
        if (held != null && held.code == null) {
            held.value = value;
        } else {
            this.queries.put(name, Query.held(value));
        }
        return this;
    }

    /** The value of the query {@code name}: the animation's time and life time, or what was given; 0 otherwise. */
    public double readQuery(final String name) {
        return switch (name) {
            case "anim_time" -> this.animTime;
            case "life_time" -> this.lifeTime;
            default -> {
                final Query query = this.queries.get(name);
                yield query == null ? 0.0 : query.read(this);
            }
        };
    }

    /**
     * Begins another frame: a query worked out by code is worked out again the next time it is read. The animator
     * calls it before it works a pose out.
     */
    public void nextFrame() {
        this.frame++;
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

    /* A query: worked out by code once a frame, or a value held. */
    private static final class Query {

        private final ToDoubleFunction<ExpressionContext> code;
        private double value;
        private long readIn = -1;

        private Query(final ToDoubleFunction<ExpressionContext> code) {
            this.code = code;
        }

        private static Query held(final double value) {
            final Query query = new Query(null);
            query.value = value;
            return query;
        }

        private double read(final ExpressionContext context) {
            if (this.code != null && this.readIn != context.frame) {
                this.readIn = context.frame;
                this.value = this.code.applyAsDouble(context);
            }
            return this.value;
        }
    }
}
