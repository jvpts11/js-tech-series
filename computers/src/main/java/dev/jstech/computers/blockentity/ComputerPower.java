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

    /*
     * When the self-test ends, in world time. The machine works this out for itself on the first tick after
     * the power goes on, because how long it takes depends on what is seated in it and the power knows nothing
     * of that; zero means it has not been worked out yet. Transient with the flag above, for the same reason.
     */
    private long postEndsAt;

    /*
     * The system coming up, after the self-test and before the desktop or the prompt. Held here beside the
     * self-test because they are the two halves of one thing, a machine on its way up, and a machine is only ever
     * in one of them. Transient for the same reason: a computer that stayed on across a reload is already up.
     */
    private boolean booting;
    private long bootEndsAt;
    /** How long this coming-up takes in all, so a bar drawn half way through knows how far along it is. */
    private int bootTicksTotal;

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

    /** Whether the machine owes a power-on self-test or is in the middle of one. */
    boolean needsPost() {
        return this.needsPost;
    }

    /** Whether the machine has yet to work out how long its self-test will take. */
    boolean postUntimed() {
        return this.needsPost && this.postEndsAt == 0L;
    }

    /** Says when the self-test this machine is in will end. */
    void timePost(final long endsAt) {
        this.postEndsAt = endsAt;
    }

    /** Whether the self-test has run its course and it is time to boot. */
    boolean postDone(final long now) {
        return this.needsPost && this.postEndsAt != 0L && now >= this.postEndsAt;
    }

    /**
     * The ticks the self-test still has to run, so a monitor opened halfway through shows the rest of it
     * rather than starting over. A machine that has not worked its length out yet answers nothing.
     */
    int postRemaining(final long now) {
        return this.needsPost && this.postEndsAt != 0L ? (int) Math.max(0L, this.postEndsAt - now) : 0;
    }

    /** Whether the system is coming up right now. */
    boolean booting() {
        return this.booting;
    }

    /** Whether the machine has yet to work out how long its system takes to come up. */
    boolean bootUntimed() {
        return this.booting && this.bootEndsAt == 0L;
    }

    /** Starts the system coming up; how long it takes is worked out on the next tick. */
    void beginBoot() {
        this.booting = true;
        this.bootEndsAt = 0L;
    }

    /** Says how long this coming-up takes, and so when it ends. */
    void timeBoot(final long now, final int ticks) {
        this.bootTicksTotal = Math.max(1, ticks);
        this.bootEndsAt = now + this.bootTicksTotal;
    }

    /** How long the coming-up under way takes in all. */
    int bootTotal() {
        return this.booting ? this.bootTicksTotal : 0;
    }

    /** Whether the system has finished coming up and it is time to hand over to it. */
    boolean bootDone(final long now) {
        return this.booting && this.bootEndsAt != 0L && now >= this.bootEndsAt;
    }

    /** The ticks the system still needs, so a monitor opened part way through joins it where it is. */
    int bootRemaining(final long now) {
        return this.booting && this.bootEndsAt != 0L ? (int) Math.max(0L, this.bootEndsAt - now) : 0;
    }

    /** The system is up, or the machine is going down: either way it is no longer coming up. */
    void endBoot() {
        this.booting = false;
        this.bootEndsAt = 0L;
        this.bootTicksTotal = 0;
    }

    void setPowered(final boolean on) {
        this.manualOn = on;
        this.booting = false;
        this.bootEndsAt = 0L;
        if (on) {
            this.needsPost = true;
            this.postEndsAt = 0L;
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
                this.postEndsAt = 0L;
                this.endSession.run();
            }
            this.manualOn = true;
        }
        this.changed.run();
    }

    void setNeedsPost(final boolean value) {
        this.needsPost = value;
        this.postEndsAt = 0L;
        if (value) {
            // A restart takes the machine back to the beginning, so whatever was coming up is not any more.
            this.booting = false;
            this.bootEndsAt = 0L;
        }
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
