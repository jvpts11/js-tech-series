/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.edit;

import dev.jstech.core.task.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * A large edit of the world under way ({@link WorldEdits}): planned away from the server's thread, then set a share of
 * each tick until it is done. It says how far it has got, can be stopped, and tells who asks when it ends.
 */
public final class WorldEdit {

    private final ServerLevel level;
    private final List<Consumer<WorldEdit>> listeners = new ArrayList<>();
    private @Nullable Task<EditPlan> planning;
    private @Nullable EditPlan plan;
    private int set;
    private State state = State.PLANNING;
    /* The player it is counted for, against their limit of edits under way; none for one a mod makes. */
    private @Nullable UUID player;

    WorldEdit(final ServerLevel level) {
        this.level = level;
    }

    /** Where it happens. */
    public ServerLevel level() {
        return this.level;
    }

    /** Where it has got to. */
    public State state() {
        return this.state;
    }

    /** The player it is counted for, against their limit of edits under way; null for one made for nobody. */
    public @Nullable UUID player() {
        return this.player;
    }

    /** How many blocks it sets in all; 0 while it is still being planned. */
    public int total() {
        return this.plan == null ? 0 : this.plan.size();
    }

    /** How many it has set. */
    public int set() {
        return this.set;
    }

    /** How far it has got, 0 to 1; 1 once it is done. */
    public double progress() {
        if (this.state == State.DONE) {
            return 1;
        }
        return total() == 0 ? 0 : (double) this.set / total();
    }

    /** Whether it has ended: done, stopped or failed. */
    public boolean over() {
        return this.state == State.DONE || this.state == State.STOPPED || this.state == State.FAILED;
    }

    /** Stops it where it is: what was set stays set. On the server's thread. */
    public void stop() {
        if (over()) {
            return;
        }
        if (this.planning != null) {
            this.planning.cancel();
        }
        end(State.STOPPED);
    }

    /** Runs {@code then} on the server's thread when it ends, at once if it has. */
    public void whenOver(final Consumer<WorldEdit> then) {
        Objects.requireNonNull(then, "then");
        if (over()) {
            then.accept(this);
        } else {
            this.listeners.add(then);
        }
    }

    void planning(final Task<EditPlan> task) {
        this.planning = task;
    }

    void forPlayer(final UUID who) {
        this.player = who;
    }

    void planned(final EditPlan made) {
        this.plan = made;
        this.state = State.SETTING;
    }

    @Nullable EditPlan plan() {
        return this.plan;
    }

    void advance(final int blocks) {
        this.set += blocks;
    }

    void end(final State how) {
        this.state = how;
        final List<Consumer<WorldEdit>> waiting = List.copyOf(this.listeners);
        this.listeners.clear();
        waiting.forEach(then -> then.accept(this));
    }

    /** Where an edit has got to. */
    public enum State {
        /** Its plan is being worked out away from the server's thread. */
        PLANNING,
        /** Its blocks are being set, a share each tick. */
        SETTING,
        /** Every block is set. */
        DONE,
        /** It was stopped before the end. */
        STOPPED,
        /** Its planning failed, and nothing was set. */
        FAILED
    }
}
