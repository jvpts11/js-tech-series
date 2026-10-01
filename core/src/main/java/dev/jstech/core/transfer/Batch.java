/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.transfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Moves that go through together or not at all: a crafting step that takes three ingredients from three chests, a
 * bus that empties a tank into another, a machine that pays energy for an item. Each step is asked first whether it
 * would move all it wants; when any would come short, nothing moves. Then each moves in turn; when one comes short
 * after all (a store that said yes and then took less), the steps already done are undone, last first, so the batch
 * ends as it began.
 *
 * <p>Each step is asked alone, so two steps into one store may each say they fit when together they do not: the
 * commit then finds it out and undoes them. A step that cannot be fully undone (a store that will not give back what
 * it took) leaves the batch {@linkplain Outcome#STUCK stuck}, which the caller should report.
 *
 * <pre>{@code
 * Outcome outcome = new Batch()
 *         .add(HandlerSteps.extract(chest, new ItemStack(Items.IRON_INGOT), 3))
 *         .add(HandlerSteps.insert(machine, new ItemStack(Items.IRON_INGOT, 3)))
 *         .commit();
 * }</pre>
 */
public final class Batch {

    private final List<IStep> steps = new ArrayList<>();

    /** Adds a step, to run after those added before it. */
    public Batch add(final IStep step) {
        this.steps.add(Objects.requireNonNull(step, "step"));
        return this;
    }

    /** How many steps the batch holds. */
    public int size() {
        return this.steps.size();
    }

    /** Whether every step says it would move all it wants; nothing moves. */
    public boolean fits() {
        for (final IStep step : this.steps) {
            if (step.simulate() < step.wants()) {
                return false;
            }
        }
        return true;
    }

    /** Moves every step, or none: what came of it. */
    public Outcome commit() {
        if (!fits()) {
            return Outcome.REFUSED;
        }
        final List<IStep> done = new ArrayList<>(this.steps.size());
        for (final IStep step : this.steps) {
            final long moved = step.execute();
            done.add(step);
            if (moved < step.wants()) {
                return undo(done);
            }
        }
        return Outcome.DONE;
    }

    /* The steps done are undone, last first: undone when every one gave back all it moved, stuck otherwise. */
    private static Outcome undo(final List<IStep> done) {
        boolean whole = true;
        for (int i = done.size() - 1; i >= 0; i--) {
            whole &= done.get(i).undo();
        }
        return whole ? Outcome.UNDONE : Outcome.STUCK;
    }

    /** What came of a batch. */
    public enum Outcome {
        /** Every step moved all it wanted. */
        DONE,
        /** A step would have come short, so nothing moved. */
        REFUSED,
        /** A step came short as it moved, and every step was undone. */
        UNDONE,
        /** A step came short as it moved, and a step could not be wholly undone. */
        STUCK;

        /** Whether the batch went through. */
        public boolean done() {
            return this == DONE;
        }
    }

    /**
     * One move of a batch. A step remembers what it did when it executes, so it can undo exactly that.
     */
    public interface IStep {

        /** How much it wants to move. */
        long wants();

        /** How much it would move now, moving nothing. */
        long simulate();

        /** Moves, and says how much it moved. */
        long execute();

        /** Undoes what the last {@link #execute()} moved; false when it could not be wholly undone. */
        boolean undo();
    }
}
