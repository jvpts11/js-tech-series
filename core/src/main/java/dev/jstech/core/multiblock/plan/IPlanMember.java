/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import dev.jstech.core.JsCore;
import dev.jstech.core.multiblock.shape.FormedPlace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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

    /**
     * The part of a plan the block at {@code pos} belongs to has formed, or come apart, and which block of the part's
     * formed model it stands for, where its place in the plan names one: a block the model draws may keep it, to be
     * collided with, aimed at and outlined as the model is drawn there ({@code IFormedPlaced}). Tells
     * {@link #partFormed(ServerLevel, BlockPos, boolean)} unless a member wants its place.
     *
     * @param place which block of the formed model it stands for; none for a place naming none
     */
    default void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed,
                            final @Nullable FormedPlace place) {
        partFormed(level, pos, formed);
    }

    /** Whether {@code state}'s block keeps itself in a plan, a member or in the tag. */
    static boolean keepsItself(final BlockState state) {
        return state.getBlock() instanceof IPlanMember || state.is(KEEPS_ITSELF);
    }
}
