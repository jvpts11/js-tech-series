/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import net.minecraft.nbt.CompoundTag;

/**
 * Whether a computer is on, whether it comes up by itself when its build stands, and whether the next
 * look at its monitor shows the power-on self-test.
 *
 * <p>Two things are handed in because they belong to the machine and not to its power: {@code changed}
 * marks the machine to be saved, and {@code endSession} is what a cold start or a power cut does to the
 * desktop and to an installer left waiting for a restart.
 */
final class ComputerPower {

    private final Runnable changed;
    private final Runnable endSession;

    private boolean manualOn;
    private boolean autoStart;

    /*
     * The power-on self-test runs once per power-up (and once per requested reboot), then the monitor
     * boots straight into the OS. Deliberately transient: a computer that stayed on across a chunk
     * reload does not POST again, exactly like a real machine that was never switched off.
     */
    private boolean needsPost;

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

    /** Whether the next monitor use should play the power-on self-test before booting. */
    boolean needsPost() {
        return this.needsPost;
    }

    void setPowered(final boolean on) {
        this.manualOn = on;
        if (on) {
            this.needsPost = true;
        }
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
                this.needsPost = true;
                this.endSession.run();
            }
            this.manualOn = true;
        }
        this.changed.run();
    }

    void setNeedsPost(final boolean value) {
        this.needsPost = value;
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
