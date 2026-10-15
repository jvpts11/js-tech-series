/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;

/**
 * What goes in and out of a machine through its faces, each set apart on its Sides tab: items, liquids, each state of
 * matter that is neither solid nor liquid, energy and heat.
 */
@TextHolder
public enum MachineResource implements IStableName {

    ITEMS("items", TextKey.of("jscore.machine.resource.items", "Items")),
    FLUIDS("fluids", TextKey.of("jscore.machine.resource.fluids", "Fluids")),
    GAS("gas", TextKey.of("jscore.machine.resource.gas", "Gas")),
    SUPERCRITICAL("supercritical", TextKey.of("jscore.machine.resource.supercritical", "Supercritical")),
    SLURRY("slurry", TextKey.of("jscore.machine.resource.slurry", "Slurry")),
    PLASMA("plasma", TextKey.of("jscore.machine.resource.plasma", "Plasma")),
    EXOTIC("exotic", TextKey.of("jscore.machine.resource.exotic", "Exotic")),
    ENERGY("energy", TextKey.of("jscore.machine.resource.energy", "Energy")),
    HEAT("heat", TextKey.of("jscore.machine.resource.heat", "Heat"));

    private final String serializedName;
    private final TextKey name;

    MachineResource(final String serializedName, final TextKey name) {
        this.serializedName = serializedName;
        this.name = name;
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** What a player calls it. */
    public TextKey text() {
        return this.name;
    }

    /** The state of matter whose tanks it is; none for items, liquids, energy and heat. */
    public Optional<MatterState> matter() {
        return switch (this) {
            case GAS -> Optional.of(MatterState.GAS);
            case SUPERCRITICAL -> Optional.of(MatterState.SUPERCRITICAL);
            case SLURRY -> Optional.of(MatterState.SLURRY);
            case PLASMA -> Optional.of(MatterState.PLASMA);
            case EXOTIC -> Optional.of(MatterState.EXOTIC);
            case ITEMS, FLUIDS, ENERGY, HEAT -> Optional.empty();
        };
    }

    /** The resource of a state of matter's tanks; none for solids and liquids, which are items and fluids. */
    public static Optional<MachineResource> of(final MatterState state) {
        return switch (state) {
            case GAS -> Optional.of(GAS);
            case SUPERCRITICAL -> Optional.of(SUPERCRITICAL);
            case SLURRY -> Optional.of(SLURRY);
            case PLASMA -> Optional.of(PLASMA);
            case EXOTIC -> Optional.of(EXOTIC);
            case SOLID, LIQUID -> Optional.empty();
        };
    }

    /** Every resource, in the order the Sides tab shows them. */
    public static List<MachineResource> round() {
        return List.of(ITEMS, FLUIDS, GAS, SUPERCRITICAL, SLURRY, PLASMA, EXOTIC, ENERGY, HEAT);
    }
}
