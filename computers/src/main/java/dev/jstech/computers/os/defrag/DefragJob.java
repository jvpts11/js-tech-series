/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.defrag;

import java.util.UUID;

/**
 * A pass of the Defrag over one disk, under way (computers/docs/design/storage.md): it tries every corrupted item once,
 * spread over the time a pass takes on that kind of disk, and keeps count of what came back and what was lost.
 *
 * <p>Pure: it says how much of the work is due as time goes by and keeps the tally, and the machine does the work.
 *
 * @see DefragRunner
 */
public final class DefragJob {

    private final UUID volume;
    private final int disk;
    private final int ticksTotal;
    private final int chance;
    private final int work;
    private final long found;
    private final int badBefore;
    private int ticksDone;
    private int tried;
    private long recovered;
    private long lost;
    private boolean paused;

    /**
     * A pass about to start.
     *
     * @param volume     the volume of the disk it repairs, which is how the disk is found again every tick
     * @param disk       the disk's place among the machine's disks, the system disk first, which names it
     * @param ticksTotal how long the pass takes
     * @param chance     the chance, in percent, that a corrupted item comes back
     * @param work       how many corrupted kinds and system files it has to try
     * @param found      how many corrupted things that comes to, items by the item and files by the file
     * @param badBefore  the bad sectors before the pass, in permille of the disk
     */
    public DefragJob(final UUID volume, final int disk, final int ticksTotal, final int chance, final int work,
                     final long found, final int badBefore) {
        this(volume, disk, ticksTotal, chance, work, found, badBefore, 0, 0, 0L, 0L);
    }

    /** A pass as it was saved, part of the way through. */
    public DefragJob(final UUID volume, final int disk, final int ticksTotal, final int chance, final int work,
                     final long found, final int badBefore, final int ticksDone, final int tried, final long recovered,
                     final long lost) {
        this.volume = volume;
        this.disk = Math.max(0, disk);
        this.ticksTotal = Math.max(1, ticksTotal);
        this.chance = Math.max(0, Math.min(100, chance));
        this.work = Math.max(0, work);
        this.found = Math.max(0L, found);
        this.badBefore = Math.max(0, badBefore);
        this.ticksDone = Math.max(0, Math.min(this.ticksTotal, ticksDone));
        this.tried = Math.max(0, Math.min(this.work, tried));
        this.recovered = Math.max(0L, recovered);
        this.lost = Math.max(0L, lost);
    }

    /**
     * Moves the pass a tick on; returns how many more corrupted kinds or files are due to be tried by now. A paused
     * pass stands where it is.
     */
    public int advance() {
        if (this.paused) {
            return 0;
        }
        if (this.ticksDone < this.ticksTotal) {
            this.ticksDone++;
        }
        final int target = (int) ((long) this.work * this.ticksDone / this.ticksTotal);
        return Math.max(0, target - this.tried);
    }

    /** Counts one corrupted kind or file tried, {@code back} of it given back and {@code gone} of it lost. */
    public void tried(final long back, final long gone) {
        this.tried = Math.min(this.work, this.tried + 1);
        this.recovered += Math.max(0L, back);
        this.lost += Math.max(0L, gone);
    }

    /** Whether the pass has run its time. */
    public boolean done() {
        return this.ticksDone >= this.ticksTotal;
    }

    /** Whether the pass is paused, standing where it is until it is resumed. */
    public boolean paused() {
        return this.paused;
    }

    public void paused(final boolean on) {
        this.paused = on;
    }

    /** How far through the pass is, from 0 to 100. */
    public int percent() {
        return (int) ((long) this.ticksDone * 100L / this.ticksTotal);
    }

    public UUID volume() {
        return this.volume;
    }

    public int disk() {
        return this.disk;
    }

    public int ticksTotal() {
        return this.ticksTotal;
    }

    public int ticksDone() {
        return this.ticksDone;
    }

    public int chance() {
        return this.chance;
    }

    public int work() {
        return this.work;
    }

    public int tried() {
        return this.tried;
    }

    public long found() {
        return this.found;
    }

    public long recovered() {
        return this.recovered;
    }

    public long lost() {
        return this.lost;
    }

    public int badBefore() {
        return this.badBefore;
    }
}
