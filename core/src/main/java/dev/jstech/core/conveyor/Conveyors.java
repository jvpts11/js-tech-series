/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * The conveyors of every mod, found by their blocks: each block entity type a {@link ConveyorBlock} makes takes items
 * from hoppers, pipes and machines through the game's item capability, behind and at its sides and above, with
 * nothing for its mod to register.
 */
public final class Conveyors {

    private Conveyors() {
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, Conveyors::onRegisterCapabilities);
    }

    /** The block entity type of every conveyor of every mod, each once. */
    public static Set<BlockEntityType<? extends ConveyorBlockEntity>> entityTypes() {
        final Set<BlockEntityType<? extends ConveyorBlockEntity>> out = new LinkedHashSet<>();
        for (final Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof ConveyorBlock conveyor) {
                out.add(conveyor.entityType());
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        for (final BlockEntityType<? extends ConveyorBlockEntity> type : entityTypes()) {
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, (BlockEntityType<ConveyorBlockEntity>) type,
                    ConveyorBlockEntity::inserter);
        }
    }
}
