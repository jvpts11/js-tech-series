/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import dev.jstech.core.connect.texture.OptifineRule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The tint of an overlay: none; the tint index the block it lies on gives it, as its model's own faces are tinted; or
 * the colour another block's colours give at the place, such as grass's, laid into its corners, since a face is
 * tinted only by the colours of the block it belongs to.
 */
final class PackTints {

    /* The block format's ints per corner, and where in them a corner keeps its colour. */
    private static final int CORNERS = 4;
    private static final int STRIDE = 8;
    private static final int COLOUR = 3;
    private static final int NO_TINT = -1;
    private static final int CHANNEL = 0xFF;
    private static final int OPAQUE = 0xFF << 24;

    private PackTints() {
    }

    /** {@code quad} tinted as {@code overlay} says, among the blocks {@code around}. */
    static BakedQuad tinted(final BakedQuad quad, final OptifineRule.Overlay overlay,
                            final PackSurroundings around) {
        if (overlay.tintBlock().isEmpty()) {
            return overlay.tintIndex() == quad.getTintIndex() ? quad : new BakedQuad(quad.getVertices(),
                    overlay.tintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(),
                    quad.hasAmbientOcclusion());
        }
        final Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(overlay.tintBlock().get()));
        final ClientLevel level = Minecraft.getInstance().level;
        if (block == Blocks.AIR || level == null) {
            return quad;
        }
        final int colour = Minecraft.getInstance().getBlockColors().getColor(block.defaultBlockState(), level,
                around.pos(), Math.max(0, overlay.tintIndex()));
        if (colour == NO_TINT) {
            return quad;
        }
        // The corners hold their colour as red, green, blue and alpha from the lowest byte up.
        final int abgr = OPAQUE | (colour & CHANNEL) << 16 | (colour >> 8 & CHANNEL) << 8 | colour >> 16 & CHANNEL;
        final int[] data = quad.getVertices().clone();
        for (int i = 0; i < CORNERS; i++) {
            data[i * STRIDE + COLOUR] = abgr;
        }
        return new BakedQuad(data, NO_TINT, quad.getDirection(), quad.getSprite(), quad.isShade(),
                quad.hasAmbientOcclusion());
    }
}
