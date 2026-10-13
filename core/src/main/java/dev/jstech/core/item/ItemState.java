/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What an item holds besides itself, as it was declared: the modes it switches between, the energy, the fluid and
 * the stacks it keeps inside. An item that keeps anything inside stacks alone, since two in one stack would share
 * one store and splitting them would make two of it.
 *
 * @param modes          its modes, in the order the mode key goes through them; empty for none
 * @param energyCapacity the most FE it holds; 0 for none
 * @param energyIn       the most FE it takes at a time
 * @param energyOut      the most FE it gives at a time
 * @param fluidCapacity  the most fluid it holds, in millibuckets; 0 for none
 * @param slots          how many stacks it holds; 0 for none
 */
public record ItemState(List<ItemMode> modes, long energyCapacity, int energyIn, int energyOut, int fluidCapacity,
                        int slots) {

    /** The most stacks an item holds: as many as the game's container contents keep. */
    public static final int MOST_SLOTS = 256;
    /** An item that holds nothing. */
    public static final ItemState NOTHING = new ItemState(List.of(), 0L, 0, 0, 0, 0);

    public ItemState {
        modes = List.copyOf(modes);
        final Set<String> ids = new HashSet<>();
        for (final ItemMode mode : modes) {
            if (!ids.add(mode.id())) {
                throw new IllegalArgumentException("two modes of one item are saved as " + mode.id());
            }
        }
        if (energyCapacity < 0 || energyIn < 0 || energyOut < 0 || fluidCapacity < 0) {
            throw new IllegalArgumentException("an item holds no less than nothing");
        }
        if (slots < 0 || slots > MOST_SLOTS) {
            throw new IllegalArgumentException("an item holds between 0 and " + MOST_SLOTS + " stacks, not " + slots);
        }
    }

    /** Whether it switches between modes. */
    public boolean hasModes() {
        return !this.modes.isEmpty();
    }

    /** Whether it holds energy. */
    public boolean holdsEnergy() {
        return this.energyCapacity > 0;
    }

    /** Whether it holds a fluid. */
    public boolean holdsFluid() {
        return this.fluidCapacity > 0;
    }

    /** Whether it holds stacks. */
    public boolean holdsItems() {
        return this.slots > 0;
    }

    /** Whether it holds nothing and switches between nothing. */
    public boolean isNothing() {
        return !hasModes() && !holdsEnergy() && !holdsFluid() && !holdsItems();
    }

    /** Whether it stacks alone: it keeps something inside. */
    public boolean stacksAlone() {
        return holdsEnergy() || holdsFluid() || holdsItems();
    }

    /** The same, switching between {@code switched}. */
    public ItemState withModes(final List<ItemMode> switched) {
        return new ItemState(switched, this.energyCapacity, this.energyIn, this.energyOut, this.fluidCapacity,
                this.slots);
    }

    /** The same, holding up to {@code capacity} FE, taking {@code in} and giving {@code out} at a time. */
    public ItemState withEnergy(final long capacity, final int in, final int out) {
        return new ItemState(this.modes, capacity, in, out, this.fluidCapacity, this.slots);
    }

    /** The same, holding up to {@code capacity} millibuckets of a fluid. */
    public ItemState withFluid(final int capacity) {
        return new ItemState(this.modes, this.energyCapacity, this.energyIn, this.energyOut, capacity, this.slots);
    }

    /** The same, holding {@code count} stacks. */
    public ItemState withSlots(final int count) {
        return new ItemState(this.modes, this.energyCapacity, this.energyIn, this.energyOut, this.fluidCapacity,
                count);
    }
}
