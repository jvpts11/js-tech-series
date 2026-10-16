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
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * What a part of a multiblock opens onto the world: a place items, fluid, energy or one of the other states of matter
 * go in or come out of the machine the structure forms. A pipe, a cable or a hopper beside that part reaches the
 * machine through it, and only through the parts the structure's pattern marks.
 */
@TextHolder
public enum PortKind implements IStableName {
    ITEM_INPUT("item_input", Resource.ITEMS, null, true,
            TextKey.of("jscore.multiblock.port.item_input", "Items in")),
    ITEM_OUTPUT("item_output", Resource.ITEMS, null, false,
            TextKey.of("jscore.multiblock.port.item_output", "Items out")),
    FLUID_INPUT("fluid_input", Resource.FLUID, null, true,
            TextKey.of("jscore.multiblock.port.fluid_input", "Liquid in")),
    FLUID_OUTPUT("fluid_output", Resource.FLUID, null, false,
            TextKey.of("jscore.multiblock.port.fluid_output", "Liquid out")),
    ENERGY_INPUT("energy_input", Resource.ENERGY, null, true,
            TextKey.of("jscore.multiblock.port.energy_input", "Energy in")),
    ENERGY_OUTPUT("energy_output", Resource.ENERGY, null, false,
            TextKey.of("jscore.multiblock.port.energy_output", "Energy out")),
    GAS_INPUT("gas_input", Resource.MATTER, MatterState.GAS, true,
            TextKey.of("jscore.multiblock.port.gas_input", "Gas in")),
    GAS_OUTPUT("gas_output", Resource.MATTER, MatterState.GAS, false,
            TextKey.of("jscore.multiblock.port.gas_output", "Gas out")),
    SUPERCRITICAL_INPUT("supercritical_input", Resource.MATTER, MatterState.SUPERCRITICAL, true,
            TextKey.of("jscore.multiblock.port.supercritical_input", "Supercritical fluid in")),
    SUPERCRITICAL_OUTPUT("supercritical_output", Resource.MATTER, MatterState.SUPERCRITICAL, false,
            TextKey.of("jscore.multiblock.port.supercritical_output", "Supercritical fluid out")),
    SLURRY_INPUT("slurry_input", Resource.MATTER, MatterState.SLURRY, true,
            TextKey.of("jscore.multiblock.port.slurry_input", "Slurry in")),
    SLURRY_OUTPUT("slurry_output", Resource.MATTER, MatterState.SLURRY, false,
            TextKey.of("jscore.multiblock.port.slurry_output", "Slurry out")),
    PLASMA_INPUT("plasma_input", Resource.MATTER, MatterState.PLASMA, true,
            TextKey.of("jscore.multiblock.port.plasma_input", "Plasma in")),
    PLASMA_OUTPUT("plasma_output", Resource.MATTER, MatterState.PLASMA, false,
            TextKey.of("jscore.multiblock.port.plasma_output", "Plasma out")),
    EXOTIC_INPUT("exotic_input", Resource.MATTER, MatterState.EXOTIC, true,
            TextKey.of("jscore.multiblock.port.exotic_input", "Exotic matter in")),
    EXOTIC_OUTPUT("exotic_output", Resource.MATTER, MatterState.EXOTIC, false,
            TextKey.of("jscore.multiblock.port.exotic_output", "Exotic matter out")),
    /** Heat, which flows by itself from hot to cold, both ways through the one port. */
    HEAT("heat", Resource.HEAT, null, true, TextKey.of("jscore.multiblock.port.heat", "Heat"));

    private final String serializedName;
    private final Resource resource;
    private final @Nullable MatterState state;
    private final boolean input;
    private final TextKey text;

    PortKind(final String serializedName, final Resource resource, @Nullable final MatterState state,
             final boolean input, final TextKey text) {
        this.serializedName = serializedName;
        this.resource = resource;
        this.state = state;
        this.input = input;
        this.text = text;
    }

    /** What the kinds of port move: items, fluid, energy, one of the other states of matter, or heat. */
    public enum Resource {
        ITEMS, FLUID, ENERGY, MATTER, HEAT
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** What a player calls it. */
    public TextKey text() {
        return this.text;
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
