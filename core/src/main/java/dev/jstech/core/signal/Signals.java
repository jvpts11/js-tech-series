/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Signals carried by the game's redstone: every sensor speaks redstone through one capability, {@link #OUTPUT}, and a
 * block gives the signal its output says ({@link #strengthAt}). What a block gives changes only when what it reads
 * does ({@link SignalLevel}), so a large build weighs nothing on the game.
 */
public final class Signals {

    /** What a sensor or a machine reads and says in redstone, on any face or the one asked. */
    public static final BlockCapability<ISignalOutput, @Nullable Direction> OUTPUT = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "signal_output"), ISignalOutput.class);

    private Signals() {
    }

    /** The signal the block at {@code pos} gives through its {@code face}, by its output; 0 for a block with none. */
    public static int strengthAt(final Level level, final BlockPos pos, final @Nullable Direction face) {
        final ISignalOutput output = level.getCapability(OUTPUT, pos, face);
        return output == null ? 0 : output.strength();
    }

    /** Offers {@link #OUTPUT} for the block entities of {@code type}, which are their own output on every face. */
    public static <T extends BlockEntity & ISignalOutput> void offer(final RegisterCapabilitiesEvent event,
                                                                    final BlockEntityType<T> type) {
        event.registerBlockEntity(OUTPUT, type, (entity, face) -> entity);
    }
}
