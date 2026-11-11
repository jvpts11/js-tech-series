/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * The drive of one Tape Library: which of its tapes it holds, and whether it is winding that tape to where a backup
 * lies or reading or writing there. A tape winds before it reads or writes, so every job the Backup Service gives the
 * library starts with a wind and is done when the wind is (computers/docs/design/programs.md). Free of Minecraft, so a
 * drive is tested without a world; the library's rack keeps one for each library it seats.
 */
public final class TapeDrive {

    private State state = State.IDLE;
    private int tape = -1;
    private long ticksLeft;
    private long ticksTotal;
    private State then = State.IDLE;

    /** What a drive is doing, which the library's card and its lamp show, with the id it travels to them by. */
    public enum State implements IStableId {
        /** Holding no job. */
        IDLE(0),
        /** Winding a tape to where a backup lies, or to its end to write a new one. */
        WINDING(1),
        /** At the backup, reading it back. */
        READING(2),
        /** At the end of what is written, writing a backup. */
        WRITING(3);

        private final int id;

        private static final StableIds<State> IDS = StableIds.of(State.class);

        State(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }

        /** The state that declares {@code id}; one no state declares reads as idle. */
        public static State byId(final int id) {
            return IDS.byId(id, IDLE);
        }
    }

    /** What the drive is doing now. */
    public State state() {
        return state;
    }

    /** The slot of the tape in the drive, counted from 0 among the library's ten, or -1 when it holds none. */
    public int tape() {
        return tape;
    }

    /** Whether a job is under way, so another has to wait for it. */
    public boolean busy() {
        return state != State.IDLE;
    }

    /** The ticks left of the wind, none once the tape is there. */
    public long ticksLeft() {
        return ticksLeft;
    }

    /** How far the wind has gone, in thousandths: a thousand once the tape is there. */
    public int permille() {
        return ticksTotal <= 0L ? 1000 : (int) ((ticksTotal - ticksLeft) * 1000L / ticksTotal);
    }

    /**
     * Takes a job: the tape in {@code slot} winds for {@code ticks}, then the drive reads or writes as {@code then}
     * says. Answers false, and takes nothing, while another job is under way.
     */
    public boolean start(final int slot, final long ticks, final State then) {
        if (busy() || then == State.IDLE || then == State.WINDING) {
            return false;
        }
        this.tape = slot;
        this.ticksTotal = Math.max(0L, ticks);
        this.ticksLeft = this.ticksTotal;
        this.then = then;
        this.state = this.ticksLeft > 0L ? State.WINDING : then;
        return true;
    }

    /** One tick of the wind; answers whether the tape just got there, which is when the reading or writing is done. */
    public boolean tick() {
        if (state != State.WINDING) {
            return false;
        }
        ticksLeft--;
        if (ticksLeft > 0L) {
            return false;
        }
        state = then;
        return true;
    }

    /** Whether the tape is where the job wants it, its reading or writing for the owner of the job to do. */
    public boolean there() {
        return state == State.READING || state == State.WRITING;
    }

    /** Ends the job: the drive is free for the next, its tape left in its slot. */
    public void finish() {
        state = State.IDLE;
        tape = -1;
        ticksLeft = 0L;
        ticksTotal = 0L;
        then = State.IDLE;
    }
}
