/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.id.IStableName;

/**
 * What a part of a multiblock opens onto the world: a place items, fluid or energy go in or come out of the machine
 * the structure forms. A pipe, a cable or a hopper beside that part reaches the machine through it, and only through
 * the parts the structure's pattern marks.
 */
public enum PortKind implements IStableName {
    ITEM_INPUT("item_input", Resource.ITEMS, true),
    ITEM_OUTPUT("item_output", Resource.ITEMS, false),
    FLUID_INPUT("fluid_input", Resource.FLUID, true),
    FLUID_OUTPUT("fluid_output", Resource.FLUID, false),
    ENERGY_INPUT("energy_input", Resource.ENERGY, true),
    ENERGY_OUTPUT("energy_output", Resource.ENERGY, false);

    private final String serializedName;
    private final Resource resource;
    private final boolean input;

    PortKind(final String serializedName, final Resource resource, final boolean input) {
        this.serializedName = serializedName;
        this.resource = resource;
        this.input = input;
    }

    /** What the three kinds of port move. */
    public enum Resource {
        ITEMS, FLUID, ENERGY
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** What goes through the port. */
    public Resource resource() {
        return resource;
    }

    /** Whether things go into the machine through it, rather than out. */
    public boolean input() {
        return input;
    }
}
