/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.os.boot.BootPhases;
import net.minecraft.nbt.CompoundTag;

/**
 * Whether a computer is on and whether it comes up by itself when its build stands.
 *
 * <p>Where it is on its way up is {@link BootPhases} beside this, because every machine has those phases
 * and not every machine has this switch: a machine in a rack is switched on by its bay, and it still
 * tests itself, still stops at its boot manager and still takes time to come up.
 *
 * <p>Two things are handed in because they belong to the machine and not to its power: {@code changed}
 * marks the machine to be saved, and {@code endSession} is what a cold start or a power cut does to the
 * desktop and to an installer left waiting for a restart.
 */
final class ComputerPower {

    private final Runnable changed;
    private final Runnable endSession;
    /** Where the machine is on its way up: the self-test, the boot manager and the system coming up. */
    private final BootPhases phases = new BootPhases();

    private boolean manualOn;
    private boolean autoStart;

    ComputerPower(final Runnable changed, final Runnable endSession) {
        this.changed = changed;
        this.endSession = endSession;
    }

    boolean on() {
        return this.manualOn;
    }

    boolean autoStart() {
        return this.autoStart;
    }

    /** Where the machine is on its way up, for whoever carries those phases along. */
    BootPhases phases() {
        return this.phases;
    }

    /** Whether the machine owes a power-on self-test or is in the middle of one. */
    boolean needsPost() {
        return this.phases.needsPost();
    }

    /**
     * The ticks the self-test still has to run, so a monitor opened halfway through shows the rest of it
     * rather than starting over. A machine that has not worked its length out yet answers nothing.
     */
    int postRemaining(final long now) {
        return this.phases.postRemaining(now);
    }

    /** Whether the machine is stopped at its boot menu. */
    boolean atMenu() {
        return this.phases.atMenu();
    }

    /** The ticks left on the menu's clock, or zero once a key has stopped it. */
    int menuRemaining(final long now) {
        return this.phases.menuRemaining(now);
    }

    /** A key was pressed: the machine waits at the menu for as long as it takes. */
    void holdMenu() {
        this.phases.holdMenu();
    }

    /** Leaves the menu, whichever way it was left. */
    void endMenu() {
        this.phases.endMenu();
    }

    /** Starts the system coming up; how long it takes is worked out on the next tick. */
    void beginBoot() {
        this.phases.beginBoot();
    }

    /** Whether the system is coming up right now. */
    boolean booting() {
        return this.phases.booting();
    }

    /** How long the coming-up under way takes in all. */
    int bootTotal() {
        return this.phases.bootTotal();
    }

    /** The ticks the system still needs, so a monitor opened part way through joins it where it is. */
    int bootRemaining(final long now) {
        return this.phases.bootRemaining(now);
    }

    void setPowered(final boolean on) {
        this.manualOn = on;
        this.phases.powered(on);
        // Power off or a cold start: neither leaves a desktop or an installer session behind.
        this.endSession.run();
        this.changed.run();
    }

    void toggle() {
        setPowered(!this.manualOn);
    }

    void toggleAutoStart(final boolean buildValid) {
        this.autoStart = !this.autoStart;
        if (this.autoStart && buildValid) {
            if (!this.manualOn) {
                /*
                 * Auto-start bringing a machine up from off is a cold start. It writes the POST flag
                 * directly, so it has to close the desktop itself: a machine that went dark through an
                 * invalid build never passed through setPowered, and its old windows would otherwise
                 * resurface on a session that no longer exists.
                 */
                this.phases.setNeedsPost(true);
                this.endSession.run();
            }
            this.manualOn = true;
        }
        this.changed.run();
    }

    void setNeedsPost(final boolean value) {
        this.phases.setNeedsPost(value);
        if (value) {
            // A restart closes everything, as it does on any machine, and it is what an installer waits for.
            this.endSession.run();
        }
    }

    /**
     * What a change to the hardware does to the power: a machine whose build no longer stands goes dark,
     * and one set to start by itself comes up the moment its build stands again.
     *
     * <p>The session is deliberately left alone, since a machine that went dark this way never passed
     * through a power cut; and the hardware marks the machine to be saved itself, so this does not.
     */
    void hardwareChanged(final boolean buildValid) {
        if (!buildValid) {
            this.manualOn = false;
        } else if (this.autoStart) {
            this.manualOn = true;
        }
    }

    void save(final CompoundTag tag) {
        tag.putBoolean("ManualOn", this.manualOn);
        tag.putBoolean("AutoStart", this.autoStart);
    }

    void load(final CompoundTag tag) {
        this.manualOn = tag.getBoolean("ManualOn");
        this.autoStart = tag.getBoolean("AutoStart");
    }
}
