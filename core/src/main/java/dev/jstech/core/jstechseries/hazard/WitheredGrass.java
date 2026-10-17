/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.hazard.Withering;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.registry.CoreItems;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * Withered grass, what grass dies into where radiation or pollution is high: the game's grass block gone the dry brown
 * of grass in the badlands. It drops dirt, or itself with silk touch, and grows back into grass where the contamination
 * falls. Any mod's contamination withers it, through the Core's withering of the land.
 */
public final class WitheredGrass {

    /** The colour withered grass is drawn in, over the game's grass: the dry brown of grass in the badlands. */
    public static final int COLOUR = 0x90814D;
    public static final BlockEntry<Block> BLOCK = CoreItems.CONTENT.block("withered_grass", Block::new)
            .properties(properties -> properties.mapColor(MapColor.TERRACOTTA_YELLOW).strength(0.6F)
                    .sound(SoundType.GRASS))
            .named("Withered Grass")
            .look(IBlockLook.fixed(new IBlockModel.Handmade("withered_grass")))
            .item()
            .drops(Drops.HANDMADE)
            .tag(BlockTags.DIRT)
            .tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .tab(JsTechTab.BLOCKS_AND_TOOLS)
            .register();

    static {
        // The Core's withering of the land turns grass into it, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, BLOCK::get);
        Withering.into(() -> Blocks.GRASS_BLOCK, () -> BLOCK.get().defaultBlockState());
        Withering.recovers(BLOCK, Blocks.GRASS_BLOCK::defaultBlockState);
    }

    private WitheredGrass() {
    }

    /** Declares the block, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }
}
