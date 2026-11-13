/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import java.util.List;
import java.util.Objects;

/**
 * Everything a crash page shows, worked out on the server from the machine that crashed.
 *
 * <p>A page of the Frames family is drawn from its style, its cause and its stop code, in the player's language; a
 * kernel's panic is the console's own lines, which no kernel ever printed in anything but its own words; an operating
 * system a player wrote comes down on its own page, which {@code player} describes.
 *
 * @param style      which page it is drawn on
 * @param cause      why the system went down, which turns a Frames page red when it was the hardware
 * @param stopCode   the stop code by name (MEMORY_MANAGEMENT), or the fatal exception's number on Frames 95
 * @param stopLine   the technical line under it: the STOP's numbers, or where Frames 95's exception happened
 * @param failedPart the part that was taken out or failed, which the newer Frames name on their page; empty for none
 * @param lines      what a kernel writes on the console, top to bottom; empty on a Frames page
 * @param totalTicks how long the page stands before the machine starts over
 * @param holdsOnKey whether a key on the console stops the automatic reboot, which then waits for another key
 * @param player     the page of an operating system a player wrote, or null for every other page
 */
public record CrashPage(CrashStyle style, CrashCause cause, String stopCode, String stopLine, String failedPart,
                        List<String> lines, int totalTicks, boolean holdsOnKey, PlayerPanic player) {

    /** The most lines a console page carries: more than the glass shows, so the end is never what gets cut. */
    public static final int MOST_LINES = 40;

    private static final int TICKS_PER_SECOND = 20;

    public CrashPage {
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(cause, "cause");
        Objects.requireNonNull(stopCode, "stopCode");
        Objects.requireNonNull(stopLine, "stopLine");
        Objects.requireNonNull(failedPart, "failedPart");
        lines = List.copyOf(lines.size() > MOST_LINES ? lines.subList(lines.size() - MOST_LINES, lines.size())
                : lines);
        if (totalTicks <= 0) {
            throw new IllegalArgumentException("a crash page stands for some time; got " + totalTicks);
        }
    }

    /** A page of one of the mod's own systems, which carries no player's panic. */
    public CrashPage(final CrashStyle style, final CrashCause cause, final String stopCode, final String stopLine,
                     final String failedPart, final List<String> lines, final int totalTicks,
                     final boolean holdsOnKey) {
        this(style, cause, stopCode, stopLine, failedPart, lines, totalTicks, holdsOnKey, null);
    }

    /** The page an operating system a player wrote came down on, in its own colours. */
    public static CrashPage of(final PlayerPanic player) {
        // In a long: seconds past what an int of ticks holds would go round to almost none, so they stand the longest.
        return new CrashPage(CrashStyle.PLAYER, CrashCause.INIT_KILLED, "", "", "", List.of(),
                player.standsUntilRestarted() ? 1 : (int) Math.min(Integer.MAX_VALUE,
                        Math.max(1L, (long) player.rebootSeconds() * TICKS_PER_SECOND)), false, player);
    }

    /** Whether the page stands until the player restarts the machine. */
    public boolean standsUntilRestarted() {
        return this.style.standsUntilRestarted() || this.player != null && this.player.standsUntilRestarted();
    }

    /** Whether this page is the red one a fault of the hardware puts up on a Frames system. */
    public boolean red() {
        return this.style.frames() && this.cause.hardware();
    }
}
