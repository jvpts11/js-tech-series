/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedParts;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * A merged block of a formed structure on the player's game: breaking in the pieces of the block it was, and, told to,
 * drawing its part's whole model ({@link FormedPartRenderer}).
 */
public final class FormedPartsClient {

    private FormedPartsClient() {
    }

    /**
     * Hands the mod's bus the block's breaking, and the drawing of a part's model by one of its blocks, once, from the
     * client setup.
     */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                FormedParts.ENTITY.get(), FormedPartRenderer::new));
        modBus.addListener(RegisterClientExtensionsEvent.class, event -> event.registerBlock(
                new IClientBlockExtensions() {
                    @Override
                    public boolean addDestroyEffects(final BlockState state, final Level level, final BlockPos pos,
                                                     final ParticleEngine manager) {
                        final BlockState was = FormedPartBlock.originalAt(level, pos, state);
                        if (was == state) {
                            return false;
                        }
                        manager.destroy(pos, was);
                        return true;
                    }
                }, FormedParts.BLOCK.get()));
    }
}
