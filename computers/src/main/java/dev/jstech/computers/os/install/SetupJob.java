/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.install;

import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * One program being set up on one machine: what it is, where it comes from, how it was asked for,
 * and how far along it is.
 *
 * <p>Pure logic: the job counts ticks and answers questions about them; who ticks it and who shows
 * it are the machine's and the windows' business. It is saved with the machine, so a setup half way
 * through when the world went away carries on where it was.
 */
@TextHolder
public final class SetupJob {

    private final String programId;
    private final String name;
    private final String house;
    private final int sizeMb;
    private final Text source;
    private final boolean removing;
    private final int ticksTotal;
    private int ticksLeft;
    /**
     * The package manager that was asked for it, whose way of drawing a bar and closing lines the prompt
     * copies; {@link PackageManagerKind#NONE} when it was a disc's setup program or the plain install verb.
     */
    private final PackageManagerKind manager;
    /** The package's short name, the word the prompt and the managers use for it. */
    private final String packageName;
    /** Whether a prompt has drawn the bar once; not saved, a prompt can draw it again from scratch. */
    private boolean barDrawn;

    /** What the window says is happening, in the order a setup went through it. */
    private static final TextKey REMOVING_FILES = TextKey.of("jsc.install.setup_job.removing_files", "Removing files");
    private static final TextKey CLEANING_UP = TextKey.of("jsc.install.setup_job.cleaning_up", "Cleaning up");
    private static final TextKey PREPARING = TextKey.of("jsc.install.setup_job.preparing", "Preparing to install");
    private static final TextKey COPYING_FILES = TextKey.of("jsc.install.setup_job.copying_files", "Copying files");
    private static final TextKey REGISTERING = TextKey.of("jsc.install.setup_job.registering", "Registering %s");
    private static final TextKey FINISHING = TextKey.of("jsc.install.setup_job.finishing", "Finishing");

    /**
     * @param programId   the program, as the registry names it
     * @param name        what the window calls it
     * @param house       who publishes it
     * @param sizeMb      what it takes on the disk
     * @param source      where it comes from, worded for the window: "DVD", "the Mirror"
     * @param removing    taking it off rather than putting it on
     * @param ticksTotal  how long the whole thing takes
     * @param ticksLeft   how much of that is still to go
     * @param manager     the package manager that was asked for it, which decides what the prompt prints
     * @param packageName the package's short name
     */
    public SetupJob(final String programId, final String name, final String house, final int sizeMb,
                    final Text source, final boolean removing, final int ticksTotal, final int ticksLeft,
                    final PackageManagerKind manager, final String packageName) {
        this.programId = programId;
        this.name = name;
        this.house = house;
        this.sizeMb = sizeMb;
        this.source = source;
        this.removing = removing;
        this.ticksTotal = Math.max(1, ticksTotal);
        this.ticksLeft = Math.max(0, Math.min(ticksLeft, this.ticksTotal));
        this.manager = manager == null ? PackageManagerKind.NONE : manager;
        this.packageName = packageName == null || packageName.isEmpty() ? pathOf(programId) : packageName;
    }

    /** A job part way through, asked for with a setup program. */
    public SetupJob(final String programId, final String name, final String house, final int sizeMb,
                    final Text source, final boolean removing, final int ticksTotal, final int ticksLeft) {
        this(programId, name, house, sizeMb, source, removing, ticksTotal, ticksLeft, PackageManagerKind.NONE, "");
    }

    /** A job at its start, asked for with a setup program. */
    public SetupJob(final String programId, final String name, final String house, final int sizeMb,
                    final Text source, final boolean removing, final int ticksTotal) {
        this(programId, name, house, sizeMb, source, removing, ticksTotal, ticksTotal, PackageManagerKind.NONE, "");
    }

    /** A job at its start, asked for from that package manager. */
    public SetupJob(final String programId, final String name, final String house, final int sizeMb,
                    final Text source, final boolean removing, final int ticksTotal,
                    final PackageManagerKind manager, final String packageName) {
        this(programId, name, house, sizeMb, source, removing, ticksTotal, ticksTotal, manager, packageName);
    }

    private static String pathOf(final String id) {
        final int colon = id == null ? -1 : id.indexOf(':');
        return id == null ? "" : colon >= 0 ? id.substring(colon + 1) : id;
    }

    public String programId() {
        return this.programId;
    }

    public String name() {
        return this.name;
    }

    public String house() {
        return this.house;
    }

    public int sizeMb() {
        return this.sizeMb;
    }

    /** Where it comes from, worded for the window and read in the player's language. */
    public Text source() {
        return this.source;
    }

    public boolean removing() {
        return this.removing;
    }

    public int ticksTotal() {
        return this.ticksTotal;
    }

    public int ticksLeft() {
        return this.ticksLeft;
    }

    /** The package manager that was asked for it, or {@link PackageManagerKind#NONE} for a setup program. */
    public PackageManagerKind manager() {
        return this.manager;
    }

    /** The package's short name, the word the prompt and the managers use for it. */
    public String packageName() {
        return this.packageName;
    }

    /** How far along, in thousandths, which is what a bar and a percentage are drawn from. */
    public int permille() {
        return (int) (1000L * (this.ticksTotal - this.ticksLeft) / this.ticksTotal);
    }

    /** The ticks gone by since it began. */
    public int ticksDone() {
        return this.ticksTotal - this.ticksLeft;
    }

    /** Whether the last tick has gone. */
    public boolean finished() {
        return this.ticksLeft <= 0;
    }

    /** One tick of copying; true when this one was the last. */
    public boolean tick() {
        if (this.ticksLeft > 0) {
            this.ticksLeft--;
        }
        return this.ticksLeft == 0;
    }

    /**
     * Says the bar is about to be drawn at a prompt, and whether this is the first time.
     *
     * <p>The first drawing goes on a line of its own; every one after it goes over that line, the way a
     * bar at a real terminal grows in place rather than filling the screen with copies of itself.
     */
    public boolean drawBar() {
        final boolean first = !this.barDrawn;
        this.barDrawn = true;
        return first;
    }

    /** The seconds still to go, rounded up, for a window that says "about 20 seconds left". */
    public int secondsLeft() {
        return (this.ticksLeft + SetupTiming.TICKS_PER_SECOND - 1) / SetupTiming.TICKS_PER_SECOND;
    }

    /**
     * What the window says is happening right now.
     *
     * <p>A real setup names the file it is copying; this names the stages the disc's own layout has,
     * in the order a setup went through them, so the line changes as the bar moves.
     */
    public Text phase() {
        if (this.removing) {
            return (this.permille() < 500 ? REMOVING_FILES : CLEANING_UP).text();
        }
        final int p = this.permille();
        if (p < 100) {
            return PREPARING.text();
        }
        if (p < 850) {
            return COPYING_FILES.text();
        }
        if (p < 1000) {
            return REGISTERING.with(this.name);
        }
        return FINISHING.text();
    }
}
