/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

/**
 * Where a machine is on its way up: owing a self-test, in the middle of one, stopped at a boot manager, or
 * bringing its system up.
 *
 * <p>A machine is only ever in one of these, which is why they are one object and not four. It holds the
 * clocks and answers questions about them and nothing else: who runs them, what decides how long each takes
 * and who is shown them are the machine's business.
 *
 * <p>Deliberately transient, all of it. A machine that stayed on across a reload is already up and does not
 * test itself again, exactly like a real one that was never switched off. The one bit worth keeping, whether
 * the next look at the machine owes a self-test, is saved by whoever owns these phases.
 */
public final class BootPhases {

    /*
     * The power-on self-test runs once per power-up (and once per requested restart), and then the machine
     * boots. When it ends is worked out on the first tick after the power goes on, because how long it takes
     * depends on what is seated in the machine and these phases know nothing of that; zero means not yet.
     */
    private boolean needsPost;
    private long postEndsAt;

    /*
     * Stopped at the boot manager's menu. Its clock is separate because a key stops it and the machine then
     * waits for a choice with no end in sight, which is a thing a deadline cannot say.
     */
    private boolean atMenu;
    private long menuEndsAt;

    /* The system coming up, after the self-test and before the desktop or the prompt. */
    private boolean booting;
    private long bootEndsAt;
    /** How long this coming-up takes in all, so a bar drawn half way through knows how far along it is. */
    private int bootTicksTotal;

    /** Whether the machine owes a power-on self-test or is in the middle of one. */
    public boolean needsPost() {
        return this.needsPost;
    }

    /**
     * Says whether the machine owes a self-test, which is also what a restart amounts to.
     *
     * <p>A restart takes the machine back to the beginning, so whatever it was standing at or bringing up is
     * not any more. What that does to the session, which is not these phases' business, belongs to the caller.
     */
    public void setNeedsPost(final boolean value) {
        this.needsPost = value;
        this.postEndsAt = 0L;
        if (value) {
            endMenu();
            endBoot();
        }
    }

    /** Whether the machine has yet to work out how long its self-test will take. */
    public boolean postUntimed() {
        return this.needsPost && this.postEndsAt == 0L;
    }

    /** Says when the self-test this machine is in will end. */
    public void timePost(final long endsAt) {
        this.postEndsAt = endsAt;
    }

    /** Whether the self-test has run its course and it is time to boot. */
    public boolean postDone(final long now) {
        return this.needsPost && this.postEndsAt != 0L && now >= this.postEndsAt;
    }

    /**
     * The ticks the self-test still has to run, so a monitor opened halfway through shows the rest of it
     * rather than starting over. A machine that has not worked its length out yet answers nothing.
     */
    public int postRemaining(final long now) {
        return this.needsPost && this.postEndsAt != 0L ? (int) Math.max(0L, this.postEndsAt - now) : 0;
    }

    /** Whether the machine is stopped at its boot menu. */
    public boolean atMenu() {
        return this.atMenu;
    }

    /** Stops the machine at its boot menu, with that long before it goes on by itself. */
    public void beginMenu(final long now, final int ticks) {
        this.atMenu = true;
        this.menuEndsAt = ticks > 0 ? now + ticks : 0L;
    }

    /** The ticks left on the menu's clock, or zero once a key has stopped it. */
    public int menuRemaining(final long now) {
        return this.atMenu && this.menuEndsAt != 0L ? (int) Math.max(0L, this.menuEndsAt - now) : 0;
    }

    /** A key was pressed: the machine waits at the menu for as long as it takes. */
    public void holdMenu() {
        this.menuEndsAt = 0L;
    }

    /** Whether the menu's clock has run out and the machine should go on by itself. */
    public boolean menuDone(final long now) {
        return this.atMenu && this.menuEndsAt != 0L && now >= this.menuEndsAt;
    }

    /** Leaves the menu, whichever way it was left. */
    public void endMenu() {
        this.atMenu = false;
        this.menuEndsAt = 0L;
    }

    /** Whether the system is coming up right now. */
    public boolean booting() {
        return this.booting;
    }

    /** Whether the machine has yet to work out how long its system takes to come up. */
    public boolean bootUntimed() {
        return this.booting && this.bootEndsAt == 0L;
    }

    /** Starts the system coming up; how long it takes is worked out on the next tick. */
    public void beginBoot() {
        this.booting = true;
        this.bootEndsAt = 0L;
    }

    /** Says how long this coming-up takes, and so when it ends. */
    public void timeBoot(final long now, final int ticks) {
        this.bootTicksTotal = Math.max(1, ticks);
        this.bootEndsAt = now + this.bootTicksTotal;
    }

    /** How long the coming-up under way takes in all. */
    public int bootTotal() {
        return this.booting ? this.bootTicksTotal : 0;
    }

    /** Whether the system has finished coming up and it is time to hand over to it. */
    public boolean bootDone(final long now) {
        return this.booting && this.bootEndsAt != 0L && now >= this.bootEndsAt;
    }

    /** The ticks the system still needs, so a monitor opened part way through joins it where it is. */
    public int bootRemaining(final long now) {
        return this.booting && this.bootEndsAt != 0L ? (int) Math.max(0L, this.bootEndsAt - now) : 0;
    }

    /** The system is up, or the machine is going down: either way it is no longer coming up. */
    public void endBoot() {
        this.booting = false;
        this.bootEndsAt = 0L;
        this.bootTicksTotal = 0;
    }

    /**
     * What the power going one way or the other does to the phases: whatever was under way is not any more,
     * and a machine coming on owes a self-test before anything else.
     */
    public void powered(final boolean on) {
        endMenu();
        endBoot();
        if (on) {
            this.needsPost = true;
            this.postEndsAt = 0L;
        }
    }
}
