/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.JsCore;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import org.jetbrains.annotations.Nullable;

/**
 * The capabilities a block and an item offer their energy of a mod's own by: the port of the block on a face, the
 * store of an item (a battery). A block registers its port as any capability, from its mod's capability registration.
 *
 * <pre>{@code
 * event.registerBlockEntity(EnergyPorts.PORT, MY_MACHINE.get(), (machine, side) -> machine.energy());
 * }</pre>
 */
public final class EnergyPorts {

    /** The port of a block, on a face. */
    public static final BlockCapability<IEnergyPort, @Nullable Direction> PORT = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "energy_port"), IEnergyPort.class);
    /** The store of an item. */
    public static final ItemCapability<IEnergyPort, @Nullable Void> ITEM = ItemCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "energy_port"), IEnergyPort.class);

    private EnergyPorts() {
    }

    /** The port of the block at {@code pos} on {@code side}, of any energy, or none. */
    public static Optional<IEnergyPort> at(final Level level, final BlockPos pos, @Nullable final Direction side) {
        return Optional.ofNullable(level.getCapability(PORT, pos, side));
    }
}
