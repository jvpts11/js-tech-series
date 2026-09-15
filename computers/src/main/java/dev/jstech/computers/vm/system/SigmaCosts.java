/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

/**
 * The levels a call out of a program is priced at.
 *
 * <p>A Σ# program is given a share of the tick to spend, and an instruction that only moves numbers
 * about inside it costs one. Asking the machine something costs more, because the machine has to go and
 * look: a glance at what the computer is costs a little, reading the network costs more, and asking the
 * network to actually do something costs most of all. A program that sweeps everything every tick should
 * feel it.
 *
 * <p>What each call costs is declared beside the call itself, in {@link SystemApi}, so an editor can tell
 * the player what a line will cost before they write it. The machine charges with these same levels, and
 * a test on a real computer checks that it charges what the declarations say.
 */
public final class SigmaCosts {

    /** A look at something the computer already knows about itself. */
    public static final int GLANCE = 5;
    /** A look at something the network already knows. */
    public static final int GLANCE_NETWORK = 10;
    /** Gathering what the computer holds: its disks, its programs, what it is running. */
    public static final int GATHER = 30;
    /** Reading, whether from a disk or from the network. */
    public static final int READ = 50;
    /** Writing, which the machine cannot take back. */
    public static final int WRITE = 100;
    /** Changing what a window shows, which the machine has to draw again for whoever is looking at it. */
    public static final int DRAW = 50;
    /** Asking the network to do something, which becomes work for the whole base. */
    public static final int SUBMIT = 200;
    /** Starting a thread, beside the call itself: a stack of its own is not a small thing. */
    public static final int THREAD_START = 49;
    /** Calling something on a computer of another mod, across a Gateway. */
    public static final int CALL_ACROSS = 100;
    /** What each thing handed to a call across a Gateway adds to it. */
    public static final int PER_ARGUMENT_ACROSS = 5;
    /** Saying something to a computer across a Gateway, or turning one on or off. */
    public static final int SEND_ACROSS = 20;

    private SigmaCosts() {
    }
}
