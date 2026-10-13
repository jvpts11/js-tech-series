/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BatchTest {

    @Test
    void commit_movesEveryStepWhenAllFit() {
        final Store a = new Store(10);
        final Store b = new Store(10);

        final Batch.Outcome outcome = new Batch().add(a.put(4)).add(b.put(6)).commit();

        assertEquals(Batch.Outcome.DONE, outcome);
        assertTrue(outcome.done());
        assertEquals(4, a.held);
        assertEquals(6, b.held);
    }

    @Test
    void commit_movesNothingWhenAStepWouldComeShort() {
        final Store a = new Store(10);
        final Store b = new Store(3);

        final Batch.Outcome outcome = new Batch().add(a.put(4)).add(b.put(6)).commit();

        assertEquals(Batch.Outcome.REFUSED, outcome);
        assertEquals(0, a.held, "the first step never moved");
        assertEquals(0, b.held);
    }

    @Test
    void commit_undoesTheStepsDoneWhenOneComesShortAsItMoves() {
        final List<String> undone = new ArrayList<>();
        final Store a = new Store(10, "a", undone);
        final Store b = new Store(10, "b", undone);
        final Store liar = new Store(10, "liar", undone);
        liar.takesOnlyHalf = true;

        final Batch.Outcome outcome = new Batch().add(a.put(2)).add(b.put(3)).add(liar.put(4)).commit();

        assertEquals(Batch.Outcome.UNDONE, outcome);
        assertEquals(0, a.held);
        assertEquals(0, b.held);
        assertEquals(0, liar.held, "even the part the liar took went back");
        assertEquals(List.of("liar", "b", "a"), undone, "undone last first");
    }

    @Test
    void commit_isStuckWhenAStepCannotGiveBack() {
        final Store a = new Store(10);
        final Store liar = new Store(10);
        a.keeps = true;
        liar.takesOnlyHalf = true;

        assertEquals(Batch.Outcome.STUCK, new Batch().add(a.put(2)).add(liar.put(4)).commit());
    }

    @Test
    void commit_twoStepsIntoOneStoreAreFoundOutAsTheyMove() {
        final Store a = new Store(5);

        // Each fits alone, but not both: the second comes short as it moves, and the first is undone.
        final Batch.Outcome outcome = new Batch().add(a.put(3)).add(a.put(3)).commit();

        assertEquals(Batch.Outcome.UNDONE, outcome);
        assertEquals(0, a.held);
    }

    @Test
    void commit_undoesTheStepsDoneWhenAStepThrows() {
        final Store a = new Store(10);
        final Store broken = new Store(10);
        broken.throwsOnExecute = true;

        assertThrows(IllegalStateException.class, () -> new Batch().add(a.put(2)).add(broken.put(3)).commit());

        assertEquals(0, a.held, "the step done before the failure was given back");
    }

    @Test
    void fits_movesNothing() {
        final Store a = new Store(10);
        final Batch batch = new Batch().add(a.put(4));

        assertTrue(batch.fits());
        assertEquals(0, a.held);
        assertFalse(new Batch().add(a.put(40)).fits());
    }

    @Test
    void commit_ofAnEmptyBatchIsDone() {
        assertEquals(Batch.Outcome.DONE, new Batch().commit());
        assertEquals(0, new Batch().size());
    }

    /** A store of so much room, which may take only half of what it says it takes, or keep what it took. */
    private static final class Store {
        private final int room;
        private final String name;
        private final List<String> undone;
        int held;
        boolean takesOnlyHalf;
        boolean keeps;
        boolean throwsOnExecute;

        Store(final int room) {
            this(room, "", new ArrayList<>());
        }

        Store(final int room, final String name, final List<String> undone) {
            this.room = room;
            this.name = name;
            this.undone = undone;
        }

        Batch.IStep put(final int amount) {
            return new Batch.IStep() {
                private long moved;

                @Override
                public long wants() {
                    return amount;
                }

                @Override
                public long simulate() {
                    return Math.min(amount, room - held);
                }

                @Override
                public long execute() {
                    if (throwsOnExecute) {
                        throw new IllegalStateException("a faulty handler");
                    }
                    final int would = Math.min(amount, room - held);
                    this.moved = takesOnlyHalf ? would / 2 : would;
                    held += (int) this.moved;
                    return this.moved;
                }

                @Override
                public boolean undo() {
                    undone.add(name);
                    if (keeps) {
                        return false;
                    }
                    held -= (int) this.moved;
                    return true;
                }
            };
        }
    }
}
