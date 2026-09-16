/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.media.MediaFormat;

/**
 * A system being put on a disk: which one, where it is going, what it is being read from and how much of it is
 * left to copy.
 *
 * <p>The machine holds this, not the screen in front of it. Putting a system on a disk takes as long as the
 * system is big and the medium is slow, and it goes on whether or not anybody is watching: closing the monitor
 * halfway through no longer throws the work away, and the job is saved with the machine, so an install that was
 * under way when the world went away carries on where it was.
 *
 * <p>Pure logic, like {@link SetupJob} next to it: it counts ticks and answers questions about them. Who ticks
 * it, who writes the system when it ends and who draws it are the machine's business.
 */
public final class OsInstallJob {

    /**
     * A copy that is being read from no drive at all.
     *
     * <p>Not {@code -1}: a drive's position is packed into a long, and any position west or north of the world's
     * origin packs into a negative one, so "less than zero" would have meant "no drive" for half the world.
     */
    public static final long NO_READER = Long.MIN_VALUE;

    private final String osId;
    private final int targetSlot;
    private final long readerPos;
    private final int ticksTotal;
    private int ticksLeft;

    /**
     * @param osId       the system being installed, as the registry names it
     * @param targetSlot the disk slot it is going on, or -1 for the machine's default disk
     * @param readerPos  the drive the medium sits in, so the machine can tell when it is taken out
     * @param ticksTotal how long the whole copy takes
     * @param ticksLeft  how much of that is still to go
     */
    public OsInstallJob(final String osId, final int targetSlot, final long readerPos, final int ticksTotal,
                        final int ticksLeft) {
        this.osId = osId;
        this.targetSlot = targetSlot;
        this.readerPos = readerPos;
        this.ticksTotal = Math.max(1, ticksTotal);
        this.ticksLeft = Math.max(0, Math.min(ticksLeft, this.ticksTotal));
    }

    /**
     * A copy about to start.
     *
     * <p>The time is the one a program's setup takes for the same bytes off the same medium, since it is the
     * same work: a system is only a large program in this. That keeps one rule for how long putting something
     * on a disk takes, ceiling and all.
     *
     * @param footprintMb what the system takes on the disk
     * @param format      the medium it is read from
     * @param eraFactor   how much faster than the earliest machines this one unpacks and writes
     */
    public static OsInstallJob beginning(final String osId, final int targetSlot, final long readerPos,
                                         final int footprintMb, final MediaFormat format, final int eraFactor) {
        final int ticks = SetupTiming.ticks(footprintMb, format, false, eraFactor);
        return new OsInstallJob(osId, targetSlot, readerPos, ticks, ticks);
    }

    /** The system being installed, as the registry names it. */
    public String osId() {
        return this.osId;
    }

    /** The disk it is going on, or -1 for the machine's default. */
    public int targetSlot() {
        return this.targetSlot;
    }

    /** The drive the medium is in; a copy stops if what it is reading leaves. */
    public long readerPos() {
        return this.readerPos;
    }

    /** Whether this copy is being read from a drive, and so can lose what it is reading. */
    public boolean hasReader() {
        return this.readerPos != NO_READER;
    }

    public int ticksTotal() {
        return this.ticksTotal;
    }

    public int ticksLeft() {
        return this.ticksLeft;
    }

    /** How far along it is, in thousandths, for a bar to draw. */
    public int permille() {
        return (int) (1000L * (this.ticksTotal - this.ticksLeft) / this.ticksTotal);
    }

    /** Whether the last tick has gone and the system is ready to be written. */
    public boolean finished() {
        return this.ticksLeft <= 0;
    }

    /** One tick of copying; true on the tick it finishes. */
    public boolean tick() {
        if (this.ticksLeft > 0) {
            this.ticksLeft--;
        }
        return this.ticksLeft == 0;
    }
}
