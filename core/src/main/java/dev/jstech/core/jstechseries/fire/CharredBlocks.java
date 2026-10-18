/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.resource.RuntimePacks;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/**
 * What fire leaves: wood charred black that burns no more, and a layer of ash where cloth, leaves and thin wood burnt
 * away. Their textures are the game's own wood and powder, darkened and greyed as the game loads its resources.
 */
public final class CharredBlocks {

    public static final BlockEntry<RotatedPillarBlock> CHARRED_LOG = CoreItems.CONTENT.block("charred_log",
                    RotatedPillarBlock::new)
            .properties(properties -> properties.mapColor(MapColor.COLOR_BLACK).strength(1.5F)
                    .sound(SoundType.WOOD))
            .named("Charred Log")
            .look(IBlockLook.axis(new IBlockModel.Handmade("charred_log")))
            .item()
            .drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_AXE)
            .tab(JsTechTab.BLOCKS_AND_TOOLS)
            .register();
    public static final BlockEntry<Block> CHARRED_PLANKS = CoreItems.CONTENT.block("charred_planks", Block::new)
            .properties(properties -> properties.mapColor(MapColor.COLOR_BLACK).strength(1.5F)
                    .sound(SoundType.WOOD))
            .named("Charred Planks")
            .look(IBlockLook.fixed(new IBlockModel.Handmade("charred_planks")))
            .item()
            .drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_AXE)
            .tab(JsTechTab.BLOCKS_AND_TOOLS)
            .register();
    public static final BlockEntry<AshBlock> ASH = CoreItems.CONTENT.block("ash", AshBlock::new)
            .properties(properties -> properties.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.1F)
                    .sound(SoundType.SAND).noOcclusion().replaceable())
            .named("Ash")
            .look(IBlockLook.fixed(new IBlockModel.Handmade("ash")))
            .item()
            .drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_SHOVEL)
            .tab(JsTechTab.BLOCKS_AND_TOOLS)
            .register();

    /** Their textures, each drawn as the player's game loads from one of the game's own, in its colour. */
    public static final List<BurntTexture> TEXTURES = List.of(
            new BurntTexture("charred_log_side", "block/oak_log", 0x2A2522),
            new BurntTexture("charred_log_top", "block/oak_log_top", 0x3A2E26),
            new BurntTexture("charred_planks", "block/oak_planks", 0x2A2522),
            new BurntTexture("ash", "block/light_gray_concrete_powder", 0x9E9A94));

    static {
        // The Core's fire leaves them where wood, cloth and leaves burnt, so the Core itself uses them.
        SharedUses.declare(JsCore.MODID, CHARRED_LOG::asItem);
        SharedUses.declare(JsCore.MODID, CHARRED_PLANKS::asItem);
        SharedUses.declare(JsCore.MODID, ASH::asItem);
        for (final BurntTexture texture : TEXTURES) {
            RuntimePacks.promise(texture.id());
        }
    }

    private CharredBlocks() {
    }

    /** Declares the blocks, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * A texture of what fire leaves.
     *
     * @param name   its name, a block texture of the Core's
     * @param from   the game's own texture it is drawn from
     * @param colour the colour it is drawn in, the game's texture's light kept
     */
    public record BurntTexture(String name, String from, int colour) {

        /** Its id, as a model names it. */
        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "block/" + this.name);
        }
    }
}
