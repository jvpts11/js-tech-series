/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

/**
 * A machine on its way up or down: the self-test, the boot manager, the system coming up, and the closing down
 * before a restart.
 *
 * <p>The phases a monitor has to find the machine in whenever it is opened, which is why each one also says how far
 * along it is. A host that runs no phases of its own answers that it is past all of them, which is what a machine
 * reached through something other than its own power amounts to: whoever asks is told there is nothing to watch
 * rather than being shown a self-test that will never end.
 */
public interface IBootingMachine {

    /** Whether the next session must run POST before handing over to the boot manager. */
    boolean needsPost();

    void setNeedsPost(boolean value);

    /**
     * Starts the machine over, letting whatever is running say goodbye first.
     *
     * <p>Different from asking for a self-test outright: a running system closes its programs and shows what
     * it shows while it does, and the self-test begins when it has finished. A host with nothing to show, or
     * nothing running, starts over at once, which is what the plain form does.
     */
    default void restart() {
        setNeedsPost(true);
    }

    /**
     * Whether the machine is closing its system down on its way to starting over.
     *
     * <p>A phase of its own, and a monitor opened during it has to find the machine in it: without this, a
     * player who looked away mid-restart came back to the desktop of a system that was being closed.
     */
    default boolean goingDown() {
        return false;
    }

    /** How long that closing-down takes in all, so a screen joining it knows how far along it is. */
    default int downTotal() {
        return 0;
    }

    /** The ticks it still has to run, so a monitor opened part way through joins it where it is. */
    default int downRemaining() {
        return 0;
    }

    /**
     * Whether this machine is standing at the end of a self-test that found nothing to boot.
     *
     * <p>A machine that answers yes is waiting for a key at its own failure, and a monitor opened on it shows
     * that rather than its setup. A host with no self-test to stand at answers no.
     */
    default boolean haltedAtPost() {
        return false;
    }

    /** A key was pressed at that failure: the machine stops standing there. */
    default void resumeFromHalt() {
    }

    /** The ticks the self-test still has to run, for a monitor opened while it is under way. */
    default int postRemaining() {
        return 0;
    }

    /** Whether the machine is stopped at its boot manager, waiting to be told what to start. */
    default boolean atBootMenu() {
        return false;
    }

    /** The ticks left before the menu boots its first entry by itself, or zero once a key has stopped it. */
    default int menuRemaining() {
        return 0;
    }

    /** A key was pressed at the menu: the machine waits there for a choice. */
    default void holdBootMenu() {
    }

    /** Leaves the menu and brings the chosen system up. */
    default void leaveBootMenu() {
    }

    /**
     * Leaves the menu to start the machine over from its self-test, which is what a boot manager's own Reboot
     * is for. No system is up yet to say goodbye, so nothing is shown closing: the machine simply starts again.
     */
    default void restartFromBootMenu() {
    }

    /** Whether the system is coming up on this machine right now. */
    default boolean booting() {
        return false;
    }

    /** The ticks the system still needs, for a monitor opened while it comes up. */
    default int bootRemaining() {
        return 0;
    }

    /** How long the coming-up under way takes in all, for the bar on the screen watching it. */
    default int bootTotal() {
        return 0;
    }

    /** What this machine's system shows while it comes up, which is nothing at all for a machine with none. */
    default BootSequence bootSequence() {
        return BootSequence.NONE;
    }

    /**
     * What the system this machine boots remembers about being greeted, and whether its welcome comes back.
     *
     * <p>A host that does not keep it answers that nobody has met its system, which is what a machine with no
     * disk of its own means anyway.
     */
    default SystemWelcome systemWelcome() {
        return SystemWelcome.UNSEEN;
    }

    /** Writes the greeting back onto the disk the system is on; a host that cannot keep it does nothing. */
    default void setSystemWelcome(final SystemWelcome welcome) {
    }

    /** Whether a drive this machine reaches holds something it could boot instead of one of its own disks. */
    default boolean hasBootableMedium() {
        return false;
    }
}
