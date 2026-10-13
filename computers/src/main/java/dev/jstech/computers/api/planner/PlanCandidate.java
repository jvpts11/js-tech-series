/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.api.planner;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * One of the plans a planner weighs for a craft: what it makes, its steps, the hints the statement asked for, and
 * its cost, which is the time the planner reckons it takes, in ticks. The cheapest plan not set aside is the one the
 * network runs.
 *
 * <p>A rule or a hint another mod brings may add to the cost or take from it, saying why, or set the plan aside
 * altogether, saying why; the planner shows every reason beside the plan.
 */
@ApiStatus.Experimental
public final class PlanCandidate {

    private final ResourceLocation result;
    private final long quantity;
    private final List<PlanStep> steps;
    private final Set<String> hints;
    private final List<Component> notes = new ArrayList<>();
    private long cost;
    @Nullable
    private Component setAside;

    /**
     * @param result   what the craft makes, by the item's id
     * @param quantity how many
     * @param steps    the plan's steps, the ones that make what the others need first
     * @param cost     what the planner reckons it takes, in ticks
     * @param hints    the words of the hints the statement asked for, in capitals
     */
    public PlanCandidate(final ResourceLocation result, final long quantity, final List<PlanStep> steps,
                         final long cost, final Set<String> hints) {
        this.result = result;
        this.quantity = quantity;
        this.steps = List.copyOf(steps);
        this.cost = Math.max(0L, cost);
        this.hints = Set.copyOf(hints);
    }

    /** What the craft makes. */
    public ResourceLocation result() {
        return result;
    }

    /** How many of it. */
    public long quantity() {
        return quantity;
    }

    /** The plan's steps. */
    public List<PlanStep> steps() {
        return steps;
    }

    /** The words of the hints the statement asked for, in capitals. */
    public Set<String> hints() {
        return hints;
    }

    /** What the plan costs now, in ticks. */
    public long cost() {
        return cost;
    }

    /** Adds {@code ticks} to the cost (or takes them off, when negative, never below nothing), saying why. */
    public void addCost(final long ticks, final Component why) {
        Objects.requireNonNull(why, "why");
        // Saturates instead of wrapping, so an extreme penalty makes the plan the dearest and never the cheapest.
        cost = ticks > 0 && cost > Long.MAX_VALUE - ticks ? Long.MAX_VALUE : Math.max(0L, cost + ticks);
        notes.add(why);
    }

    /** Sets the plan aside, saying why: the network will not run it whatever it costs. */
    public void setAside(final Component why) {
        Objects.requireNonNull(why, "why");
        if (setAside == null) {
            setAside = why;
        }
    }

    /** Whether the plan was set aside. */
    public boolean isSetAside() {
        return setAside != null;
    }

    /** Why the plan was set aside, or null when it was not. */
    @Nullable
    public Component whySetAside() {
        return setAside;
    }

    /** Why the cost changed, in the order the changes were made. */
    public List<Component> notes() {
        return List.copyOf(notes);
    }
}
