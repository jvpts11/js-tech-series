/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Which blocks microblocks are cut from: any full block of any mod, drawn by a model, with no entity and an item of
 * its own; a pack keeps one out with the tag {@code jscore:not_cuttable}.
 */
public final class MicroblockMaterials {

    /** The blocks no microblock is cut from, whatever they are. */
    public static final TagKey<Block> NOT_CUTTABLE = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "not_cuttable"));

    private MicroblockMaterials() {
    }

    /** Whether a piece of {@code material} hides what lies behind it, as a solid block of it does; glass does not. */
    public static boolean opaque(final Block material) {
        return material.defaultBlockState().isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    /** Whether microblocks are cut from {@code block}. */
    public static boolean cuttable(final Block block) {
        if (block instanceof EntityBlock || block.asItem() == Items.AIR) {
            return false;
        }
        final BlockState state = block.defaultBlockState();
        return !state.isAir() && !state.is(NOT_CUTTABLE) && state.getRenderShape() == RenderShape.MODEL
                && state.getFluidState().isEmpty()
                && state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }
}
