/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.rack;

import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRackPartBlockEntity;
import dev.jstech.core.heat.IThermalBudget;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * A Server Rack's heat as J's Core reads machines' heat: the draw of the machines turned on in it, what its cabinet and
 * its Cooling Units take away ({@link RackThermals}), and the share of their speed its machines keep while it is over.
 * The cabinet keeps a budget and no temperature of its own. Read live from the rack each time it is asked.
 *
 * @param rack the rack it reads
 */
public record RackThermalBudget(ServerRackBlockEntity rack) implements IThermalBudget {

    private static final double PERCENT = 100.0;

    /** The budget of the rack {@code part} is a block of, or none while it is bound to no rack that is loaded. */
    public static @Nullable IThermalBudget ofPart(final ServerRackPartBlockEntity part) {
        final BlockPos origin = part.originPos();
        if (origin == null || part.getLevel() == null) {
            return null;
        }
        return Loaded.blockEntity(part.getLevel(), origin) instanceof ServerRackBlockEntity rack
                ? new RackThermalBudget(rack) : null;
    }

    @Override
    public double loadWatts() {
        return rack.thermalLoadWatts();
    }

    @Override
    public double budgetWatts() {
        return rack.thermalBudgetWatts();
    }

    @Override
    public double temperature() {
        return Double.NaN;
    }

    @Override
    public double performance() {
        return rack.thermalThrottlePercent() / PERCENT;
    }
}
