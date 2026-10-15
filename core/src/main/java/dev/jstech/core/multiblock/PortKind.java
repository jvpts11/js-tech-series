/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.material.MatterState;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * What a part of a multiblock opens onto the world: a place items, fluid, energy or one of the other states of matter
 * go in or come out of the machine the structure forms. A pipe, a cable or a hopper beside that part reaches the
 * machine through it, and only through the parts the structure's pattern marks.
 */
public enum PortKind implements IStableName {
    ITEM_INPUT("item_input", Resource.ITEMS, null, true),
    ITEM_OUTPUT("item_output", Resource.ITEMS, null, false),
    FLUID_INPUT("fluid_input", Resource.FLUID, null, true),
    FLUID_OUTPUT("fluid_output", Resource.FLUID, null, false),
    ENERGY_INPUT("energy_input", Resource.ENERGY, null, true),
    ENERGY_OUTPUT("energy_output", Resource.ENERGY, null, false),
    GAS_INPUT("gas_input", Resource.MATTER, MatterState.GAS, true),
    GAS_OUTPUT("gas_output", Resource.MATTER, MatterState.GAS, false),
    SUPERCRITICAL_INPUT("supercritical_input", Resource.MATTER, MatterState.SUPERCRITICAL, true),
    SUPERCRITICAL_OUTPUT("supercritical_output", Resource.MATTER, MatterState.SUPERCRITICAL, false),
    SLURRY_INPUT("slurry_input", Resource.MATTER, MatterState.SLURRY, true),
    SLURRY_OUTPUT("slurry_output", Resource.MATTER, MatterState.SLURRY, false),
    PLASMA_INPUT("plasma_input", Resource.MATTER, MatterState.PLASMA, true),
    PLASMA_OUTPUT("plasma_output", Resource.MATTER, MatterState.PLASMA, false),
    EXOTIC_INPUT("exotic_input", Resource.MATTER, MatterState.EXOTIC, true),
    EXOTIC_OUTPUT("exotic_output", Resource.MATTER, MatterState.EXOTIC, false);

    private final String serializedName;
    private final Resource resource;
    private final @Nullable MatterState state;
    private final boolean input;

    PortKind(final String serializedName, final Resource resource, @Nullable final MatterState state,
             final boolean input) {
        this.serializedName = serializedName;
        this.resource = resource;
        this.state = state;
        this.input = input;
    }

    /** What the kinds of port move: items, fluid, energy, or one of the other states of matter. */
    public enum Resource {
        ITEMS, FLUID, ENERGY, MATTER
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** What goes through the port. */
    public Resource resource() {
        return resource;
    }

    /** The state of matter a port of matter moves; none for items, fluid and energy. */
    public Optional<MatterState> state() {
        return Optional.ofNullable(state);
    }

    /** Whether things go into the machine through it, rather than out. */
    public boolean input() {
        return input;
    }
}
