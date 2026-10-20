/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3d;

/**
 * Holds bodies to each other by rows, as joints hold them, by sequential impulses as the contacts are held
 * ({@link ContactSolver}). A row is one way two bodies may not move apart: along a line through two points, or
 * turning about an axis. Each says how fast the bodies are to go along it, a motor's speed or none; how hard it may
 * push, one way only for a rope or a limit, so much and no more for a motor; and how soft it is, for a spring, which
 * then holds as a spring of its stiffness and damping does, steady however stiff. All the rows in turn, again and
 * again, until they agree; each starts from the push it gave the step before, so a chain of joints holds still.
 *
 * <p>Holding speeds alone, a joint drifts: a pendulum swinging fast goes round a little wider each step. So once the
 * bodies have moved, the rows are made again where they are, and the bodies moved, not sped, back together by what
 * each row drifted ({@link #project}), as the best physics engines do; nothing is added to how fast they go.
 */
public final class ConstraintRows {

    private final List<Row> live = new ArrayList<>();
    private final List<Row> spare = new ArrayList<>();
    /* The push each row gave last step, by its key, for it to start from this step. */
    private Map<Long, Double> before = new HashMap<>();
    private Map<Long, Double> after = new HashMap<>();

    /* How much of last step's push a row starts from. */
    private static final double WARM = 0.9;
    /* Below this two bodies give nothing against a row, and it is let be. */
    private static final double TINY = 1.0e-12;
    /* How much of what a row drifted is taken back each round of drawing together, and what is let be. */
    private static final double PROJECT = 1.0;
    private static final double SLOP = 0.001;
    /* The most a row is drawn back in a step, in metres or radians, so a joint torn apart comes back gently. */
    private static final double MOST_PROJECT = 0.25;

    /**
     * A new row between {@code a} and {@code b}, known from step to step by {@code key}: it holds nothing until its
     * ways are set, pushes either way without bound, and is hard.
     */
    public Row add(final RigidBody a, final RigidBody b, final long key) {
        final Row row = spare.isEmpty() ? new Row() : spare.remove(spare.size() - 1);
        row.reset(a, b, key);
        live.add(row);
        return row;
    }

    /** The rows of this step. */
    public List<Row> rows() {
        return live;
    }

    /** Whether there are no rows this step. */
    public boolean isEmpty() {
        return live.isEmpty();
    }

    /** Readies the rows for a step: what each gives against, and each started from its push of the step before. */
    public void begin() {
        weigh();
        for (final Row row : live) {
            final Double last = before.get(row.key);
            if (last != null && row.mass > 0) {
                row.impulse = clamp(last * WARM, row.lower, row.upper);
                apply(row, row.impulse);
            }
        }
    }

    /** Works every row {@code times} times, there and back by turns, so none is always worked first. */
    public void iterate(final int times) {
        for (int i = 0; i < times; i++) {
            round(i);
        }
    }

    /** Works every row once, the {@code index}th time: there on an even time, back on an odd one. */
    public void round(final int index) {
        if (index % 2 == 0) {
            for (final Row row : live) {
                work(row);
            }
        } else {
            for (int j = live.size() - 1; j >= 0; j--) {
                work(live.get(j));
            }
        }
    }

    /** Keeps what each row gave, for the next step to start from. */
    public void end() {
        after.clear();
        for (final Row row : live) {
            after.put(row.key, row.impulse);
        }
        final Map<Long, Double> swapped = before;
        before = after;
        after = swapped;
    }

    /**
     * Draws the bodies back together where they are, {@code times} rounds, the rows having been made again where the
     * bodies now stand: each row that holds a place moves its bodies by as much as undoes most of what it drifted,
     * within its bounds, a rope only drawn in and a limit only pushed back. The pushes of the step are not touched.
     */
    public void project(final int times) {
        weigh();
        for (final Row row : live) {
            row.a.startShift();
            row.b.startShift();
            row.moved = 0;
        }
        for (int i = 0; i < times; i++) {
            for (final Row row : live) {
                if (!row.holdsPlace || row.mass <= 0) {
                    continue;
                }
                final double off = row.error + row.a.shiftedAlongRow(row.linearA, row.angularA)
                        + row.b.shiftedAlongRow(row.linearB, row.angularB);
                final double drift = Math.abs(off) <= SLOP ? 0 : off - Math.signum(off) * SLOP;
                final double wanted = -Math.max(-MOST_PROJECT, Math.min(MOST_PROJECT, PROJECT * drift)) * row.mass;
                final double next = clamp(row.moved + wanted, row.lower, row.upper);
                final double size = next - row.moved;
                row.moved = next;
                if (size != 0) {
                    row.a.shiftAlongRow(row.linearA, row.angularA, size);
                    row.b.shiftAlongRow(row.linearB, row.angularB, size);
                }
            }
        }
    }

    /** Forgets this step's rows, keeping what they gave. */
    public void clear() {
        spare.addAll(live);
        live.clear();
    }

    /** Forgets everything, last step's pushes too. */
    public void reset() {
        clear();
        before.clear();
        after.clear();
    }

    /* What each row gives against, as the bodies are readied now. */
    private void weigh() {
        for (final Row row : live) {
            final double give = row.a.giveAlongRow(row.linearA, row.angularA)
                    + row.b.giveAlongRow(row.linearB, row.angularB) + row.softness;
            row.mass = give > TINY ? 1.0 / give : 0;
        }
    }

    /* One working of a row: as much push as brings the bodies to its speed along it, within its bounds. */
    private void work(final Row row) {
        if (row.mass <= 0) {
            return;
        }
        final double along = row.a.alongRow(row.linearA, row.angularA) + row.b.alongRow(row.linearB, row.angularB);
        final double wanted = -(along + row.bias + row.softness * row.impulse) * row.mass;
        final double next = clamp(row.impulse + wanted, row.lower, row.upper);
        apply(row, next - row.impulse);
        row.impulse = next;
    }

    private static void apply(final Row row, final double size) {
        if (size == 0) {
            return;
        }
        row.a.pushAlongRow(row.linearA, row.angularA, size);
        row.b.pushAlongRow(row.linearB, row.angularB, size);
    }

    private static double clamp(final double value, final double lower, final double upper) {
        return Math.max(lower, Math.min(upper, value));
    }

    /**
     * One row: how fast its two bodies go along it is the speed of {@code a}'s centre along {@link #linearA()} and
     * its spin along {@link #angularA()}, and the same of {@code b}'s along its own; the row pushes them until that
     * speed is its {@link #target(double) target}, and, holding a place, draws back what they drifted along it.
     */
    public static final class Row {

        private final Vector3d linearA = new Vector3d();
        private final Vector3d angularA = new Vector3d();
        private final Vector3d linearB = new Vector3d();
        private final Vector3d angularB = new Vector3d();
        private RigidBody a;
        private RigidBody b;
        private long key;
        private double bias;
        private double lower;
        private double upper;
        private double softness;
        private double impulse;
        private double mass;
        private double error;
        private boolean holdsPlace;
        private double moved;

        /** The way the row goes for its first body's centre. */
        public Vector3d linearA() {
            return linearA;
        }

        /** The way the row turns its first body. */
        public Vector3d angularA() {
            return angularA;
        }

        /** The way the row goes for its second body's centre. */
        public Vector3d linearB() {
            return linearB;
        }

        /** The way the row turns its second body. */
        public Vector3d angularB() {
            return angularB;
        }

        /** Sets how fast its bodies are to go along it, in metres or radians a second: a motor's speed, or none. */
        public Row target(final double speed) {
            this.bias = -speed;
            return this;
        }

        /** Bounds its push, in newton seconds a step: {@code least} to {@code most}. */
        public Row bounds(final double least, final double most) {
            this.lower = least;
            this.upper = most;
            return this;
        }

        /** Makes it soft, as a spring: {@code softness} the speed a newton second of its push lets through. */
        public Row softness(final double softness) {
            this.softness = Math.max(0, softness);
            return this;
        }

        /**
         * Makes it hold a place: {@code off} is how far its bodies stand along it from where they are to be, in
         * metres or radians, drawn back once they have moved.
         */
        public Row holds(final double off) {
            this.error = off;
            this.holdsPlace = true;
            return this;
        }

        /** The push it gave this step, in newton seconds. */
        public double impulse() {
            return impulse;
        }

        /** Its name from step to step. */
        public long key() {
            return key;
        }

        private void reset(final RigidBody first, final RigidBody second, final long name) {
            this.a = first;
            this.b = second;
            this.key = name;
            this.bias = 0;
            this.lower = Double.NEGATIVE_INFINITY;
            this.upper = Double.POSITIVE_INFINITY;
            this.softness = 0;
            this.impulse = 0;
            this.mass = 0;
            this.error = 0;
            this.holdsPlace = false;
            this.moved = 0;
            linearA.zero();
            angularA.zero();
            linearB.zero();
            angularB.zero();
        }
    }
}
