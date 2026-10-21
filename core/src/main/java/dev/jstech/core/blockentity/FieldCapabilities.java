/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import dev.jstech.core.content.ModContent;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Offers every declared inventory, energy store and tank marked {@code exposed()} to pipes, cables and machines,
 * for every block entity type any mod of the series declares, so no mod registers them one by one. A block entity
 * that exposes nothing answers none, and any capability a mod registers itself still stands beside these. A block
 * entity that is an {@link IFaceGate} offers them face by face, as its gate lets through.
 */
public final class FieldCapabilities {

    private FieldCapabilities() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, FieldCapabilities::register);
    }

    public static void register(final RegisterCapabilitiesEvent event) {
        for (final ModContent content : ModContent.all()) {
            for (final BlockEntityType<?> type : content.declaredBlockEntityTypes()) {
                register(event, type);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void register(final RegisterCapabilitiesEvent event, final BlockEntityType<?> type) {
        final BlockEntityType<BlockEntity> any = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, any, (blockEntity, side) ->
                offered(blockEntity, Capabilities.ItemHandler.BLOCK, side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, any, (blockEntity, side) ->
                offered(blockEntity, Capabilities.EnergyStorage.BLOCK, side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, any, (blockEntity, side) ->
                offered(blockEntity, Capabilities.FluidHandler.BLOCK, side));
    }

    /* What a block entity's exposed fields offer for {@code capability} on {@code side}, through its faces' gate. */
    private static <T> @Nullable T offered(final BlockEntity blockEntity, final BlockCapability<T, ?> capability,
                                           final @Nullable Direction side) {
        if (!(blockEntity instanceof SyncedBlockEntity synced)) {
            return null;
        }
        final T whole = synced.fields().capability(capability);
        final T offered = synced instanceof IFaceGate gate ? gate.throughFace(capability, side, whole) : whole;
        return offered == null ? null : capability.typeClass().cast(CountedCapabilities.counted(offered,
                synced.fields().flow()));
    }
}
