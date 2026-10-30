/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block of a plan that keeps something of its own, as a computer keeps its state or a piece keeps a formed look of
 * its own, so does not merge into its part's formed model, with a block entity or without: told as its part forms and
 * comes apart, it may change how it looks, as one drawn by the model once its part is formed stops drawing itself, or
 * one takes its own formed shape. A block a mod cannot make one of, a vanilla crafting table a plan wants where it
 * works, keeps itself the same way when it is in the tag {@link #KEEPS_ITSELF}, told nothing.
 */
public interface IPlanMember {

    /** The blocks that keep themselves in a plan as its members do, drawing and working as they are. */
    TagKey<Block> KEEPS_ITSELF = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "keeps_itself_in_plans"));

    /** The part of a plan the block at {@code pos} belongs to has formed, or come apart. */
    void partFormed(ServerLevel level, BlockPos pos, boolean formed);

    /** Whether {@code state}'s block keeps itself in a plan, a member or in the tag. */
    static boolean keepsItself(final BlockState state) {
        return state.getBlock() instanceof IPlanMember || state.is(KEEPS_ITSELF);
    }
}
