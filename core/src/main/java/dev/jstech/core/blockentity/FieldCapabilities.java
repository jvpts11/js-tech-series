/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.ModContent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Offers every declared inventory, energy store and tank marked {@code exposed()} to pipes, cables and machines,
 * for every block entity type any mod of the series declares, so no mod registers them one by one. A block entity
 * that exposes nothing answers none, and any capability a mod registers itself still stands beside these.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class FieldCapabilities {

    private FieldCapabilities() {
    }

    @SubscribeEvent
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
                blockEntity instanceof SyncedBlockEntity synced
                        ? synced.fields().capability(Capabilities.ItemHandler.BLOCK) : null);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, any, (blockEntity, side) ->
                blockEntity instanceof SyncedBlockEntity synced
                        ? synced.fields().capability(Capabilities.EnergyStorage.BLOCK) : null);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, any, (blockEntity, side) ->
                blockEntity instanceof SyncedBlockEntity synced
                        ? synced.fields().capability(Capabilities.FluidHandler.BLOCK) : null);
    }
}
